/*
 * Copyright 2012 frdfsnlght <frdfsnlght@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.frdfsnlght.transporter;

import com.frdfsnlght.transporter.api.TypeMap;
import com.frdfsnlght.transporter.GateMap.Point;
import com.frdfsnlght.transporter.GateMap.Volume;
import com.frdfsnlght.transporter.api.GateException;
import com.frdfsnlght.transporter.api.BlockGate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class BlockGateImpl extends GateImpl implements BlockGate {

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\\\n");

    private static final Set<String> OPTIONS = new HashSet<String>(GateImpl.BASEOPTIONS);

    static {
        OPTIONS.add("restoreOnClose");
    }

    private String designName;
    private boolean restoreOnClose;

    private List<GateBlock> blocks;
    private List<SavedBlock> savedBlocks = null;

    // creation from file
    public BlockGateImpl(World world, TypeMap conf) throws GateException {
        super(world, conf);
        options = new Options(this, OPTIONS, "trp.gate", this);

        designName = conf.getString("designName");
        restoreOnClose = conf.getBoolean("restoreOnClose", false);

        List<TypeMap> maps = conf.getMapList("blocks");
        if (maps == null)
            throw new GateException("missing blocks");
        blocks = new ArrayList<GateBlock>();
        for (TypeMap map : maps) {
            try {
                GateBlock block = new GateBlock(map);
                block.setWorld(world);
                blocks.add(block);
            } catch (BlockException be) {
                throw new GateException(be.getMessage());
            }
        }

        maps = conf.getMapList("saved");
        if (maps != null) {
            savedBlocks = new ArrayList<SavedBlock>();
            for (TypeMap map : maps) {
                try {
                    SavedBlock block = new SavedBlock(map);
                    block.setWorld(world);
                    savedBlocks.add(block);
                } catch (BlockException be) {
                    throw new GateException(be.getMessage());
                }
            }
            if (savedBlocks.isEmpty()) savedBlocks = null;
        }

        calculateCenter();
        validate();
    }

    // creation from design
    public BlockGateImpl(World world, String gateName, Player player, BlockFace direction, Design design, TransformedDesign tDesign) throws GateException {
        super(world, gateName, player, direction);
        options = new Options(this, OPTIONS, "trp.gate", this);

        designName = design.getName();
        duration = design.getDuration();
        restoreOnClose = design.getRestoreOnClose();

        protect = design.getProtect();
        teleportFormat = design.getTeleportFormat();
        noLinksFormat = design.getNoLinksFormat();
        noLinkSelectedFormat = design.getNoLinkSelectedFormat();
        invalidLinkFormat = design.getInvalidLinkFormat();
        unknownLinkFormat = design.getUnknownLinkFormat();
        hidden = design.getHidden();
        countdown = design.getCountdown();
        countdownInterval = design.getCountdownInterval();
        countdownFormat = design.getCountdownFormat();
        countdownIntervalFormat = design.getCountdownIntervalFormat();
        countdownCancelFormat = design.getCountdownCancelFormat();

        this.blocks = tDesign.getBlocks();

        calculateCenter();
        validate();
        generateFile();
        updateScreens();
        dirty = true;
    }

    @Override
    public Location getSpawnLocation(Location fromLocation, BlockFace fromDirection) {
        List<GateBlock> gbs = new ArrayList<GateBlock>();
        for (GateBlock gb : blocks)
            if (gb.getDetail().isSpawn()) gbs.add(gb);
        GateBlock block = gbs.get((new Random()).nextInt(gbs.size()));
        Location toLocation = block.getLocation().clone();
        toLocation.add(0.5, 0, 0.5);
        toLocation.setYaw(block.getDetail().getSpawn().calculateYaw(fromLocation.getYaw(), fromDirection, getDirection()));
        toLocation.setPitch(fromLocation.getPitch());
        return toLocation;
    }

    @Override
    public void onProtect(Location loc) {
        GateBlock gb = getGateBlock(loc);
        if ((gb != null) &&
            gb.getDetail().isBuildable() &&
            ((! portalOpen) || (! gb.getDetail().isPortal())))
            gb.getDetail().getBuildBlock().build(loc);
        updateScreens();
    }

    public void rebuild() {
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isBuildable()) continue;
            if (portalOpen && gb.getDetail().isPortal()) continue;
            gb.getDetail().getBuildBlock().build(gb.getLocation());
        }
        updateScreens();
    }

    @Override
    protected void onValidate() throws GateException {
        if (designName == null)
            throw new GateException("designName is required");
        if (! Design.isValidName(designName))
            throw new GateException("designName is not valid");
        if (blocks.isEmpty())
            throw new GateException("must have at least one block");
    }

    @Override
    protected void onAdd() {
        Gates.addScreenVolume(getScreenVolume());
        Gates.addTriggerVolume(getTriggerVolume());
        Gates.addSwitchVolume(getSwitchVolume());
        if (portalOpen)
            Gates.addPortalVolume(getPortalVolume());
        if (protect)
            Gates.addProtectionVolume(getBuildVolume());
        updateScreens();
    }

    @Override
    protected void onRemove() {
        Gates.removePortalVolume(this);
        Gates.removeProtectionVolume(this);
        Gates.removeScreenVolume(this);
        Gates.removeTriggerVolume(this);
        Gates.removeSwitchVolume(this);
    }

    @Override
    protected void onDestroy(boolean unbuild) {
        Gates.removePortalVolume(this);
        Gates.removeProtectionVolume(this);
        Gates.removeScreenVolume(this);
        Gates.removeTriggerVolume(this);
        Gates.removeSwitchVolume(this);
        if (unbuild) {
            for (GateBlock gb : blocks) {
                if (! gb.getDetail().isBuildable()) continue;
                Block b = gb.getLocation().getBlock();
                b.setType(Material.AIR);
            }
        }
    }

    @Override
    protected void onOpen() {
        openPortal();
    }

    @Override
    protected void onClose() {
        closePortal();
    }

    @Override
    protected void onNameChanged() {
        updateScreens();
    }

    @Override
    protected void onDestinationChanged() {
        updateScreens();
    }

    @Override
    protected void onSave(TypeMap conf) {
        conf.set("designName", designName);
        conf.set("restoreOnClose", restoreOnClose);

        List<Object> mapList = new ArrayList<Object>();
        for (GateBlock block : blocks)
            mapList.add(block.encode());
        conf.set("blocks", mapList);

        if (savedBlocks != null) {
            mapList = new ArrayList<Object>();
            for (SavedBlock block : savedBlocks)
                mapList.add(block.encode());
            conf.set("saved", mapList);
        }
    }

    @Override
    protected void calculateCenter() {
        double cx = 0, cy = 0, cz = 0;
        for (GateBlock block : blocks) {
            cx += block.getLocation().getBlockX() + 0.5;
            cy += block.getLocation().getBlockY() + 0.5;
            cz += block.getLocation().getBlockZ() + 0.5;
        }
        cx /= blocks.size();
        cy /= blocks.size();
        cz /= blocks.size();
        center = new Vector(cx, cy, cz);
    }


    // Overrides

    @Override
    public String toString() {
        return "BlockGate[" + getFullName() + "]";
    }

    // Custom methods

    public String getDesignName() {
        return designName;
    }

    public GateBlock getGateBlock(Location loc) {
        for (GateBlock gb : blocks) {
            Location gbLoc = gb.getLocation();
            if ((loc.getBlockX() == gbLoc.getBlockX()) &&
                (loc.getBlockY() == gbLoc.getBlockY()) &&
                (loc.getBlockZ() == gbLoc.getBlockZ())) return gb;
        }
        return null;
    }

    private Volume getBuildVolume() {
        Volume vol = new Volume(this);
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isBuildable()) continue;
            vol.addPoint(new Point(gb.getLocation()));
        }
        return vol;
    }

    private Volume getScreenVolume() {
        Volume vol = new Volume(this);
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isScreen()) continue;
            vol.addPoint(new Point(gb.getLocation()));
        }
        return vol;
    }

    private Volume getTriggerVolume() {
        Volume vol = new Volume(this);
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isTrigger()) continue;
            vol.addPoint(new Point(gb.getLocation()));
        }
        return vol;
    }

    private Volume getSwitchVolume() {
        Volume vol = new Volume(this);
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isSwitch()) continue;
            vol.addPoint(new Point(gb.getLocation()));
        }
        return vol;
    }

    private Volume getPortalVolume() {
        Volume vol = new Volume(this);
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isPortal()) continue;
            vol.addPoint(new Point(gb.getLocation()));
        }
        return vol;
    }

    private void updateScreens() {
        Set<GateBlock> screens = new HashSet<GateBlock>();
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isScreen()) continue;
            screens.add(gb);
        }
        if (screens.isEmpty()) return;

        String format;
        GateImpl toGate = null;

        if (outgoing == null) {
            if (! isLinked())
                format = getLinkNoneFormat();
            else
                format = getLinkUnselectedFormat();
        } else {
            toGate = Gates.get(outgoing);
            if (toGate == null)
                format = getLinkOfflineFormat();
            else {
                if (! toGate.isSameWorld(world))
                    format = getLinkWorldFormat();
                else
                    format = getLinkLocalFormat();
            }
        }
        List<String> lines = new ArrayList<String>();

        if ((format != null) && (! format.equals("-"))) {
            format = format.replace("%fromGate%", this.getName());
            format = format.replace("%fromWorld%", this.getWorld().getName());
            if (toGate != null) {
                format = format.replace("%toGate%", toGate.getName());
                format = format.replace("%toWorld%", toGate.getWorld().getName());
            } else if (outgoing != null) {
                String[] parts = outgoing.split("\\.");
                format = format.replace("%toGate%", parts[parts.length - 1]);
                if (parts.length > 1)
                    format = format.replace("%toWorld%", parts[parts.length - 2]);
            }
            lines.addAll(Arrays.asList(NEWLINE_PATTERN.split(format)));
        }

        for (GateBlock gb : screens) {
            Block block = gb.getLocation().getBlock();
            BlockState sign = block.getState();
            if (! (sign instanceof Sign)) continue;
            for (int i = 0; i < 4; i++) {
                if (i >= lines.size())
                    ((Sign)sign).setLine(i, "");
                else
                    ((Sign)sign).setLine(i, lines.get(i));
            }
            sign.update();
        }
    }

    private void openPortal() {
        savedBlocks = new ArrayList<SavedBlock>();
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isOpenable()) continue;
            if (restoreOnClose)
                savedBlocks.add(new SavedBlock(gb.getLocation()));
            gb.getDetail().getOpenBlock().build(gb.getLocation());
        }
        if (savedBlocks.isEmpty()) savedBlocks = null;
        Gates.addPortalVolume(getPortalVolume());
        dirty = true;
    }

    private void closePortal() {
        if (savedBlocks != null) {
            for (SavedBlock b : savedBlocks)
                b.restore();
            savedBlocks = null;
        } else {
            for (GateBlock gb : blocks) {
                if (! gb.getDetail().isOpenable()) continue;
                if (gb.getDetail().isBuildable())
                    gb.getDetail().getBuildBlock().build(gb.getLocation());
                else
                gb.getLocation().getBlock().setType(Material.AIR);
            }
        }
        Gates.removePortalVolume(this);
        dirty = true;
    }







    /* Begin options */

    public boolean getRestoreOnClose() {
        return restoreOnClose;
    }

    public void setRestoreOnClose(boolean b) {
        restoreOnClose = b;
        dirty = true;
    }

    @Override
    public void onOptionSet(Context ctx, String name, String value) {
        super.onOptionSet(ctx, name, value);
        if (name.equals("protect")) {
            if (protect)
                Gates.addProtectionVolume(getBuildVolume());
            else
                Gates.removeProtectionVolume(this);
        }
    }

    /* End options */


}

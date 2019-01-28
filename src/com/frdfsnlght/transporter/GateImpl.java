/*
 * Copyright 2011 frdfsnlght <frdfsnlght@gmail.com>.
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.frdfsnlght.transporter.GateMap.Point;
import com.frdfsnlght.transporter.GateMap.Volume;
import com.frdfsnlght.transporter.api.Gate;
import com.frdfsnlght.transporter.api.TypeMap;
import com.frdfsnlght.transporter.api.event.GateClosedEvent;
import com.frdfsnlght.transporter.api.event.GateOpenedEvent;
import com.frdfsnlght.transporter.command.CommandException;
import com.frdfsnlght.transporter.exceptions.BlockException;
import com.frdfsnlght.transporter.exceptions.GateException;
import com.frdfsnlght.transporter.exceptions.OptionsException;
import com.frdfsnlght.transporter.exceptions.PermissionsException;
import com.frdfsnlght.transporter.exceptions.TransporterException;
import com.frdfsnlght.transporter.listeners.OptionsListener;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class GateImpl implements Gate, OptionsListener {

    public static GateImpl load(World world, File file) throws GateException {
        if (! file.exists())
            throw new GateException("%s not found", file.getAbsolutePath());
        if (! file.isFile())
            throw new GateException("%s is not a file", file.getAbsolutePath());
        if (! file.canRead())
            throw new GateException("unable to read %s", file.getAbsoluteFile());
        TypeMap conf = new TypeMap(file);
        conf.load();

        return new GateImpl(world, conf);
    }

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\\\n");
    protected static final Set<String> OPTIONS = new HashSet<String>();

    static {
        OPTIONS.add("duration");
        OPTIONS.add("direction");
        OPTIONS.add("linkLocal");
        OPTIONS.add("linkWorld");
        OPTIONS.add("linkNoneFormat");
        OPTIONS.add("linkUnselectedFormat");
        OPTIONS.add("linkOfflineFormat");
        OPTIONS.add("linkLocalFormat");
        OPTIONS.add("linkWorldFormat");
        OPTIONS.add("protect");
        OPTIONS.add("restoreOnClose");
        OPTIONS.add("teleportFormat");
        OPTIONS.add("noLinksFormat");
        OPTIONS.add("noLinkSelectedFormat");
        OPTIONS.add("invalidLinkFormat");
        OPTIONS.add("unknownLinkFormat");
        OPTIONS.add("countdown");
        OPTIONS.add("countdownInterval");
        OPTIONS.add("countdownFormat");
        OPTIONS.add("countdownIntervalFormat");
        OPTIONS.add("countdownCancelFormat");
        OPTIONS.add("hidden");
    }

    protected File file;
    private String designName;
    private Player creator;
    protected World world;
    protected Vector center;
    protected BlockFace direction;

    protected String name;
    protected int duration;
    protected boolean linkLocal;
    protected boolean linkWorld;
    protected String linkNoneFormat;
    protected String linkUnselectedFormat;
    protected String linkOfflineFormat;
    protected String linkLocalFormat;
    protected String linkWorldFormat;
    protected boolean protect;
    private boolean restoreOnClose;
    protected String teleportFormat;
    protected String noLinksFormat;
    protected String noLinkSelectedFormat;
    protected String invalidLinkFormat;
    protected String unknownLinkFormat;
    protected boolean hidden;
    protected int countdown;
    protected int countdownInterval;
    protected String countdownFormat;
    protected String countdownIntervalFormat;
    protected String countdownCancelFormat;

    protected final List<String> links = new ArrayList<String>();
    private List<GateBlock> blocks;
    private List<SavedBlock> savedBlocks = null;

    protected Set<String> incoming = new HashSet<String>();
    protected String outgoing = null;

    protected boolean dirty = false;
    protected boolean portalOpen = false;
    protected long portalOpenTime = 0;
    protected Options options = new Options(this, OPTIONS, "trp.gate", this);

    protected GateImpl(World world, TypeMap conf) throws GateException {
        this.file = conf.getFile();
        designName = conf.getString("designName");
        String creatorStr = conf.getString("creatorUUID");
        if (creatorStr != null) {
            creator = Bukkit.getPlayer(UUID.fromString(creatorStr));
        } else {
            creator = Bukkit.getPlayer(conf.getString("creatorName"));
        }
        this.world = world;
        name = conf.getString("name");
        try {
            direction = Utils.valueOf(BlockFace.class, conf.getString("direction", "NORTH"));
        } catch (IllegalArgumentException iae) {
            throw new GateException(iae.getMessage() + " direction");
        }

        options = new Options(this, OPTIONS, "trp.gate", this);
        duration = conf.getInt("duration", -1);
        linkLocal = conf.getBoolean("linkLocal", true);
        linkWorld = conf.getBoolean("linkWorld", true);

        linkNoneFormat = conf.getString("linkNoneFormat", "%fromGate%\\n\\n<none>");
        linkUnselectedFormat = conf.getString("linkUnselectedFormat", "%fromGate%\\n\\n<unselected>");
        linkOfflineFormat = conf.getString("linkOfflineFormat", "%fromGate%\\n\\n<offline>");
        linkLocalFormat = conf.getString("linkLocalFormat", "%fromGate%\\n%toGate%");
        linkWorldFormat = conf.getString("linkWorldFormat", "%fromGate%\\n%toWorld%\\n%toGate%");

        links.addAll(conf.getStringList("links", new ArrayList<String>()));
        portalOpen = conf.getBoolean("portalOpen", false);
        protect = conf.getBoolean("protect", false);
        restoreOnClose = conf.getBoolean("restoreOnClose", false);
        teleportFormat = conf.getString("teleportFormat", "%GOLD%teleported to '%toGateCtx%'");
        noLinksFormat = conf.getString("noLinksFormat", "this gate has no links");
        noLinkSelectedFormat = conf.getString("noLinkSelectedFormat", "no link is selected");
        invalidLinkFormat = conf.getString("invalidLinkFormat", "invalid link selected");
        unknownLinkFormat = conf.getString("unknownLinkFormat", "unknown or offline destination gate");
        hidden = conf.getBoolean("hidden", false);
        countdown = conf.getInt("countdown", -1);
        countdownInterval = conf.getInt("countdownInterval", 1000);
        countdownFormat = conf.getString("countdownFormat", "%RED%Teleport countdown started...");
        countdownIntervalFormat = conf.getString("countdownIntervalFormat", "%RED%Teleport in %time% seconds...");
        countdownCancelFormat = conf.getString("countdownCancelFormat", "Teleport canceled");

        incoming.addAll(conf.getStringList("incoming", new ArrayList<String>()));
        outgoing = conf.getString("outgoing");
        
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
    public GateImpl(World world, String gateName, Player player, BlockFace direction, Design design, TransformedDesign tDesign) throws GateException {
        this.world = world;
        name = gateName;
        creator = player;
        this.direction = direction;
        setDefaults();
        
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

    private void setDefaults() {
        setDuration(-1);
        setLinkLocal(true);
        setLinkWorld(true);
        setLinkNoneFormat(null);
        setLinkUnselectedFormat(null);
        setLinkOfflineFormat(null);
        setLinkLocalFormat(null);
        setLinkWorldFormat(null);
        setProtect(false);
        setTeleportFormat(null);
        setNoLinksFormat(null);
        setNoLinkSelectedFormat(null);
        setInvalidLinkFormat(null);
        setUnknownLinkFormat(null);
        setHidden(false);
        setCountdown(-1);
        setCountdownInterval(1000);
        setCountdownFormat(null);
        setCountdownIntervalFormat(null);
        setCountdownCancelFormat(null);
    }

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

    public void rebuild() {
        for (GateBlock gb : blocks) {
            if (! gb.getDetail().isBuildable()) continue;
            if (portalOpen && gb.getDetail().isPortal()) continue;
            gb.getDetail().getBuildBlock().build(gb.getLocation());
        }
        updateScreens();
    }
    
    // Events

    public void onProtect(Location loc) {
        GateBlock gb = getGateBlock(loc);
        if ((gb != null) &&
            gb.getDetail().isBuildable() &&
            ((! portalOpen) || (! gb.getDetail().isPortal())))
            gb.getDetail().getBuildBlock().build(loc);
        updateScreens();
    }

    protected void onValidate() throws GateException {
        if (designName == null)
            throw new GateException("designName is required");
        if (! Design.isValidName(designName))
            throw new GateException("designName is not valid");
        if (blocks.isEmpty())
            throw new GateException("must have at least one block");
    }
    
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
    
    protected void onRemove() {
        Gates.removePortalVolume(this);
        Gates.removeProtectionVolume(this);
        Gates.removeScreenVolume(this);
        Gates.removeTriggerVolume(this);
        Gates.removeSwitchVolume(this);
    }

    protected void onOpen() {
        openPortal();
    }

    protected void onClose() {
        closePortal();
    }

    protected void onNameChanged() {
        updateScreens();
    }

    protected void onDestinationChanged() {
        updateScreens();
    }

    // Gate interface

    public String getFullName() {
        return world.getName() + "." + getName();
    }

    public String getName(Context ctx) {
        if ((ctx != null) && ctx.isPlayer()) return getName();
        return getFullName();
    }

    public static boolean isValidName(String name) {
        if ((name.length() == 0) || (name.length() > 15)) return false;
        return ! (name.contains(".") || name.contains("*"));
    }

    public String getDesignName() {
        return designName;
    }
    
    protected void attach(GateImpl origin) {
        if (origin != null) {
            String originName = origin.getFullName();
            if (incoming.contains(originName)) return;
            incoming.add(originName);
            dirty = true;
        }

        // 2 new
        portalOpen = true;
        portalOpenTime = System.currentTimeMillis();

        onOpen();

        // try to attach to our destination
        if ((outgoing == null) || (! hasLink(outgoing))) {
            if (! isLinked())
                outgoing = null;
            else
                outgoing = getLinks().get(0);
            onDestinationChanged();
        }
        if (outgoing != null) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.attach(this);
        }

        // new
        if (duration > 0) {
            final GateImpl myself = this;
            Utils.fireDelayed(new Runnable() {
                
                public void run() {
                    myself.closeIfAllowed();
                }
            }, duration + 100);
        }
    }

    
    protected void detach(GateImpl origin) {
        String originName = origin.getFullName();
        if (! incoming.contains(originName)) return;

        incoming.remove(originName);
        dirty = true;
        closeIfAllowed();
    }

    // End interfaces and implementations

    public void onRenameComplete() {
        file.delete();
        generateFile();
        save(true);
        onNameChanged();
    }

    public void onGateAdded(GateImpl gate) {
        if (gate == this)
            onAdd();
        else {
            if ((outgoing != null) && outgoing.equals(gate.getFullName()))
                onDestinationChanged();
        }
    }

    public void onGateRemoved(GateImpl gate) {
        if (gate == this)
            onRemove();
        else {
            String gateName = gate.getFullName();
            if (gateName.equals(outgoing)) {
                //outgoing = null;
                //dirty = true;
                onDestinationChanged();
            }
            closeIfAllowed();
        }
    }

    public void onGateDestroyed(GateImpl gate) {
        if (gate == this) return;
        String gateName = gate.getFullName();
        if (removeLink(gateName))
            dirty = true;
        if (gateName.equals(outgoing)) {
            outgoing = null;
            dirty = true;
            onDestinationChanged();
        }
        if (incoming.contains(gateName)) {
            incoming.remove(gateName);
            dirty = true;
        }
        closeIfAllowed();
    }

    public void onGateRenamed(GateImpl gate, String oldFullName) {
        if (gate == this) return;
        String newName = gate.getFullName();
        if (links.contains(oldFullName)) {
            links.set(links.indexOf(oldFullName), newName);
            dirty = true;
        }
        if (oldFullName.equals(outgoing)) {
            outgoing = newName;
            dirty = true;
            onDestinationChanged();
        }
        if (incoming.contains(oldFullName)) {
            incoming.remove(oldFullName);
            incoming.add(newName);
            dirty = true;
        }
    }

    public void destroy(boolean unbuild) {
        close();
        if (! file.delete())
            Utils.warning("unable to delete gate file %s", file.getAbsolutePath());
        else
            Utils.info("deleted gate file %s", file.getAbsolutePath());
        file = null;
        onDestroy(unbuild);
    }

    public boolean isOpen() {
        return portalOpen;
    }

    public boolean isClosed() {
        return ! portalOpen;
    }

    public boolean isSameWorld(World world) {
        return this.world == world;
    }

    public World getWorld() {
        return world;
    }

    public void open() throws GateException {
        if (portalOpen) return;

        // try to get our destination
        if ((outgoing == null) || (! hasLink(outgoing))) {
            if (! isLinked())
                throw new GateException("this gate has no links");
            outgoing = getLinks().get(0);
            dirty = true;
        }
        GateImpl gate = Gates.get(outgoing);
        if (gate == null)
            throw new GateException("unknown or offline gate '%s'", outgoing);

        portalOpen = true;
        portalOpenTime = System.currentTimeMillis();
        gate.attach(this);
        onOpen();
        onDestinationChanged();

        GateOpenedEvent event = new GateOpenedEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        if (duration > 0) {
            final GateImpl myself = this;
            Utils.fireDelayed(new Runnable() {
                
                public void run() {
                    myself.closeIfAllowed();
                }
            }, duration + 100);
        }
    }

    public void close() {
        if (! portalOpen) return;
        portalOpen = false;

        ReservationImpl.removeCountdowns(this);
        incoming.clear();
        onClose();
        onDestinationChanged();

        GateClosedEvent event = new GateClosedEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        // try to detach from our destination
        if (outgoing != null) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.detach(this);
        }
    }

    
    public void save(boolean force) {
        if ((! dirty) && (! force)) return;
        if (file == null) return;

        TypeMap conf = new TypeMap(file);
        conf.set("name", name);
        conf.set("designName", designName);
        conf.set("creatorUUID", creator.getUniqueId().toString());
        conf.set("direction", direction.toString());
        conf.set("duration", duration);
        conf.set("linkLocal", linkLocal);
        conf.set("linkWorld", linkWorld);

        conf.set("linkNoneFormat", linkNoneFormat);
        conf.set("linkUnselectedFormat", linkUnselectedFormat);
        conf.set("linkOfflineFormat", linkOfflineFormat);
        conf.set("linkLocalFormat", linkLocalFormat);
        conf.set("linkWorldFormat", linkWorldFormat);

        conf.set("links", links);
        conf.set("protect", protect);
        conf.set("restoreOnClose", restoreOnClose);
        conf.set("teleportFormat", teleportFormat);
        conf.set("noLinksFormat", noLinksFormat);
        conf.set("noLinkSelectedFormat", noLinkSelectedFormat);
        conf.set("invalidLinkFormat", invalidLinkFormat);
        conf.set("unknownLinkFormat", unknownLinkFormat);
        conf.set("hidden", hidden);
        conf.set("countdown", countdown);
        conf.set("countdownInterval", countdownInterval);
        conf.set("countdownFormat", countdownFormat);
        conf.set("countdownIntervalFormat", countdownIntervalFormat);
        conf.set("countdownCancelFormat", countdownCancelFormat);

        conf.set("portalOpen", portalOpen);

        if (! incoming.isEmpty()) conf.set("incoming", new ArrayList<String>(incoming));
        if (outgoing != null) conf.set("outgoing", outgoing);

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

        File parent = file.getParentFile();
        if (! parent.exists())
            parent.mkdirs();
        conf.save();
        dirty = false;
    }

    protected void validate() throws GateException {
        if (name == null)
            throw new GateException("name is required");
        if (! isValidName(name))
            throw new GateException("name is not valid");
        if (creator == null)
            throw new GateException("creator is required");
        onValidate();
    }

    public Vector getCenter() {
        return center;
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
    
    public String toString() {
        return "Gate[" + getFullName() + "]";
    }
    
    /* Begin options */

    public boolean getRestoreOnClose() {
        return restoreOnClose;
    }

    public void setRestoreOnClose(boolean b) {
        restoreOnClose = b;
        dirty = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public int getDuration() {
        return duration;
    }

    
    public void setDuration(int i) {
        if (i <= 0) i = -1;
        duration = i;
        dirty = true;
    }

    
    public BlockFace getDirection() {
        return direction;
    }

    
    public void setDirection(BlockFace dir) {
        direction = dir;
        dirty = true;
    }

    
    public boolean getLinkLocal() {
        return linkLocal;
    }

    
    public void setLinkLocal(boolean b) {
        linkLocal = b;
        dirty = true;
    }

    
    public boolean getLinkWorld() {
        return linkWorld;
    }

    
    public void setLinkWorld(boolean b) {
        linkWorld = b;
        dirty = true;
    }

    
    public String getLinkNoneFormat() {
        return linkNoneFormat;
    }

    
    public void setLinkNoneFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<none>";
        linkNoneFormat = s;
        dirty = true;
    }

    
    public String getLinkUnselectedFormat() {
        return linkUnselectedFormat;
    }

    
    public void setLinkUnselectedFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<unselected>";
        linkUnselectedFormat = s;
        dirty = true;
    }

    
    public String getLinkOfflineFormat() {
        return linkOfflineFormat;
    }

    
    public void setLinkOfflineFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<offline>";
        linkOfflineFormat = s;
        dirty = true;
    }

    
    public String getLinkLocalFormat() {
        return linkLocalFormat;
    }

    
    public void setLinkLocalFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n%toGate%";
        linkLocalFormat = s;
        dirty = true;
    }

    
    public String getLinkWorldFormat() {
        return linkWorldFormat;
    }

    
    public void setLinkWorldFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n%toWorld%\\n%toGate%";
        linkWorldFormat = s;
        dirty = true;
    }

    
    public boolean getProtect() {
        return protect;
    }

    
    public void setProtect(boolean b) {
        protect = b;
        dirty = true;
    }

    
    public String getTeleportFormat() {
        return teleportFormat;
    }

    
    public void setTeleportFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%GOLD%teleported to '%toGateCtx%'";
        teleportFormat = s;
        dirty = true;
    }

    
    public String getNoLinksFormat() {
        return noLinksFormat;
    }

    
    public void setNoLinksFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "this gate has no links";
        noLinksFormat = s;
        dirty = true;
    }

    
    public String getNoLinkSelectedFormat() {
        return noLinkSelectedFormat;
    }

    
    public void setNoLinkSelectedFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "no link is selected";
        noLinkSelectedFormat = s;
        dirty = true;
    }

    
    public String getInvalidLinkFormat() {
        return invalidLinkFormat;
    }

    
    public void setInvalidLinkFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "invalid link selected";
        invalidLinkFormat = s;
        dirty = true;
    }

    
    public String getUnknownLinkFormat() {
        return unknownLinkFormat;
    }

    
    public void setUnknownLinkFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "unknown or offline destination gate";
        unknownLinkFormat = s;
        dirty = true;
    }

    
    public boolean getHidden() {
        return hidden;
    }

    
    public void setHidden(boolean b) {
        boolean old = hidden;
        hidden = b;
        dirty = dirty || (old != hidden);
    }

    
    public int getCountdown() {
        return countdown;
    }

    
    public void setCountdown(int i) {
        countdown = i;
        dirty = true;
    }

    
    public int getCountdownInterval() {
        return countdownInterval;
    }

    
    public void setCountdownInterval(int i) {
        if (i < 1) i = 1;
        countdownInterval = i;
        dirty = true;
    }

    
    public String getCountdownFormat() {
        return countdownFormat;
    }

    
    public void setCountdownFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport countdown started...";
        countdownFormat = s;
        dirty = true;
    }

    
    public String getCountdownIntervalFormat() {
        return countdownIntervalFormat;
    }

    
    public void setCountdownIntervalFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport in %time% seconds...";
        countdownIntervalFormat = s;
        dirty = true;
    }

    
    public String getCountdownCancelFormat() {
        return countdownCancelFormat;
    }

    
    public void setCountdownCancelFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport canceled";
        countdownCancelFormat = s;
        dirty = true;
    }



    public void getOptions(Context ctx, String name) throws OptionsException, PermissionsException {
        options.getOptions(ctx, name);
    }

    public String getOption(Context ctx, String name) throws OptionsException, PermissionsException {
        return options.getOption(ctx, name);
    }

    public void setOption(Context ctx, String name, String value) throws OptionsException, PermissionsException {
        options.setOption(ctx, name, value);
    }

    
    public void onOptionSet(Context ctx, String name, String value) {
        ctx.send("option '%s' set to '%s' for gate '%s'", name, value, getName(ctx));
        if (name.equals("protect")) {
            if (protect)
                Gates.addProtectionVolume(getBuildVolume());
            else
                Gates.removeProtectionVolume(this);
        }
    }

    
    public String getOptionPermission(Context ctx, String name) {
        return name + "." + name;
    }

    /* End options */

    public List<String> getLinks() {
        return new ArrayList<String>(links);
    }

    public boolean isLinked() {
        return ! links.isEmpty();
    }

    public boolean hasLink(String link) {
        return links.contains(link);
    }

    public void addLink(Context ctx, String toGateName) throws TransporterException {
        Permissions.require(ctx.getPlayer(), "trp.gate.link.add." + getFullName());

        if (isLinked())
            throw new GateException("gates cannot accept multiple links", getName(ctx));

        GateImpl toGate = Gates.find(ctx, toGateName);
        if (toGate == null)
            throw new GateException("gate '%s' cannot be found", toGateName);

        if (isSameWorld(toGate.getWorld())) {
            if (! Config.getAllowLinkLocal())
                throw new CommandException("linking to on-world gates is not permitted");
        } else {
            if (! Config.getAllowLinkWorld())
                throw new CommandException("linking to off-world gates is not permitted");
        }

        if (! addLink(toGate.getFullName()))
            throw new GateException("gate '%s' already links to '%s'", getName(ctx), toGate.getName(ctx));

        ctx.sendLog("added link from '%s' to '%s'", getName(ctx), toGate.getName(ctx));
    }

    protected boolean addLink(String link) {
        if (links.contains(link)) return false;
        links.add(link);
        if (links.size() == 1)
            outgoing = link;
        onDestinationChanged();
        dirty = true;
        return true;
    }

    public void removeLink(Context ctx, String toGateName) throws TransporterException {
        Permissions.require(ctx.getPlayer(), "trp.gate.link.remove." + getFullName());

        GateImpl toGate = Gates.find(toGateName);
        if (toGate != null) toGateName = toGate.getFullName();

        if (! removeLink(toGateName))
            throw new GateException("gate '%s' does not have a link to '%s'", getName(ctx), toGateName);

        ctx.sendLog("removed link from '%s' to '%s'", getName(ctx), toGateName);
    }

    protected boolean removeLink(String link) {
        if (! links.contains(link)) return false;
        links.remove(link);
        if (link.equals(outgoing))
            outgoing = null;
        onDestinationChanged();
        closeIfAllowed();
        dirty = true;
        return true;
    }

    public void nextLink() throws GateException {
        // trivial case of single link to prevent needless detach/attach
        if ((links.size() == 1) && links.contains(outgoing)) {
            //updateScreens();
            return;
        }

        // detach from the current gate
        if (portalOpen && (outgoing != null)) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.detach(this);
        }

        // select next link
        if ((outgoing == null) || (! links.contains(outgoing))) {
            if (! links.isEmpty()) {
                outgoing = links.get(0);
                dirty = true;
            }

        } else {
            int i = links.indexOf(outgoing) + 1;
            if (i >= links.size()) i = 0;
            outgoing = links.get(i);
            dirty = true;
        }

        onDestinationChanged();

        // attach to the next gate
        if (portalOpen && (outgoing != null)) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.attach(this);
            else {
                Utils.debug("closing if allowed");
                closeIfAllowed();
            }
        }
        getDestinationGate();
    }

    public boolean isLastLink() {
        if (outgoing == null)
            return links.isEmpty();
        return links.indexOf(outgoing) == (links.size() - 1);
    }

    public boolean hasValidDestination() {
        try {
            getDestinationGate();
            return true;
        } catch (GateException e) {
            return false;
        }
    }

    public String getDestinationLink() {
        return outgoing;
    }

    public GateImpl getDestinationGate() throws GateException {
        if (outgoing == null) {
            if (! isLinked())
                throw new GateException(getNoLinksFormat());
            else
                throw new GateException(getNoLinkSelectedFormat());
        } else if (! hasLink(outgoing))
            throw new GateException(getInvalidLinkFormat());
        GateImpl gate = Gates.get(outgoing);
        if (gate == null)
            throw new GateException(getUnknownLinkFormat());
        return gate;
    }

    protected void generateFile() {
        File worldFolder = Worlds.worldPluginFolder(world);
        File gatesFolder = new File(worldFolder, "gates");
        String fileName = name.replaceAll("[^\\w-\\.]", "_");
        if (name.hashCode() > 0) fileName += "-";
        fileName += name.hashCode();
        fileName += ".yml";
        file = new File(gatesFolder, fileName);
    }

    private void closeIfAllowed() {
        if (! portalOpen) return;
        if (canClose()) close();
    }

    private boolean canClose() {

        if (duration < 1)
            return (! hasValidDestination()) && incoming.isEmpty();

        // temporary gate
        boolean expired = ((System.currentTimeMillis() - portalOpenTime) + 50) > duration;

        // handle mutually paired gates
        if ((outgoing != null) && hasValidDestination() && incoming.contains(outgoing) && (incoming.size() == 1)) return expired;

        if (incoming.isEmpty())
            return (outgoing == null) || expired;

        return false;
    }

}

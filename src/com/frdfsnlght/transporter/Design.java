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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.frdfsnlght.transporter.api.TypeMap;
import com.frdfsnlght.transporter.exceptions.BlockException;
import com.frdfsnlght.transporter.exceptions.DesignException;
import com.frdfsnlght.transporter.exceptions.GateException;
import com.frdfsnlght.transporter.exceptions.TransporterException;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public class Design {

    public static boolean isValidName(String name) {
        if ((name.length() == 0) || (name.length() > 15)) return false;
        return ! (name.contains(".") || name.contains("*"));
    }

    private String name;
    private String attribution;
    private boolean enabled;
    private int duration;
    private boolean buildable;
    private boolean creatable;
    private boolean buildFromInventory;
    private boolean linkLocal;
    private boolean linkWorld;
    private String linkNoneFormat;
    private String linkUnselectedFormat;
    private String linkOfflineFormat;
    private String linkLocalFormat;
    private String linkWorldFormat;
    private boolean restoreOnClose;
    private boolean protect;
    private String teleportFormat;
    private String noLinksFormat;
    private String noLinkSelectedFormat;
    private String invalidLinkFormat;
    private String unknownLinkFormat;
    protected boolean hidden;
    private int countdown;
    private int countdownInterval;
    private String countdownFormat;
    private String countdownIntervalFormat;
    private String countdownCancelFormat;

    private List<Pattern> buildWorlds = null;
    private List<DesignBlock> blocks = null;

    private int sizeX, sizeY, sizeZ;    // calculated

    @SuppressWarnings("unchecked")
    public Design(File file) throws DesignException, BlockException {
        if (! file.exists())
            throw new DesignException("%s not found", file.getAbsolutePath());
        if (! file.isFile())
            throw new DesignException("%s is not a file", file.getAbsolutePath());
        if (! file.canRead())
            throw new DesignException("unable to read %s", file.getAbsoluteFile());
        TypeMap conf = new TypeMap(file);
        conf.load();

        name = conf.getString("name");
        attribution = conf.getString("attribution");
        enabled = conf.getBoolean("enabled", true);
        duration = conf.getInt("duration", -1);
        buildable = conf.getBoolean("buildable", true);
        creatable = conf.getBoolean("creatable", true);
        buildFromInventory = conf.getBoolean("buildFromInventory", false);
        linkLocal = conf.getBoolean("linkLocal", true);
        linkWorld = conf.getBoolean("linkWorld", true);
        linkNoneFormat = conf.getString("linkNoneFormat", "%fromGate%\\n\\n<none>");
        linkUnselectedFormat = conf.getString("linkUnselectedFormat", "%fromGate%\\n\\n<unselected>");
        linkOfflineFormat = conf.getString("linkOfflineFormat", "%fromGate%\\n\\n<offline>");
        linkLocalFormat = conf.getString("linkLocalFormat", "%fromGate%\\n%toGate%");
        linkWorldFormat = conf.getString("linkWorldFormat", "%fromGate%\\n%toWorld%\\n%toGate%");
        restoreOnClose = conf.getBoolean("restoreOnClose", false);
        protect = conf.getBoolean("protect", false);
        teleportFormat = conf.getString("teleportFormat", "%GOLD%teleported to '%toGateCtx%'");
        noLinksFormat = conf.getString("noLinksFormat", "this gate has no links");
        noLinkSelectedFormat = conf.getString("noLinkSelectedFormat", "no link is selected");
        invalidLinkFormat = conf.getString("invalidLinkFormat", "invalid link selected");
        unknownLinkFormat = conf.getString("unknownLinkFormat", "unknown or offline destination endpoint");
        hidden = conf.getBoolean("hidden", false);
        countdown = conf.getInt("countdown", -1);
        countdownInterval = conf.getInt("countdownInterval", 1000);
        countdownFormat = conf.getString("countdownFormat", "%RED%Teleport countdown started...");
        countdownIntervalFormat = conf.getString("countdownIntervalFormat", "%RED%Teleport in %time% seconds...");
        countdownCancelFormat = conf.getString("countdownCancelFormat", "%RED%Teleport canceled");

        buildWorlds = new ArrayList<Pattern>();
        String pattern = conf.getString("buildWorlds");
        if (pattern != null)
            try {
                buildWorlds.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException pse) {
                throw new DesignException("invalid buildWorld pattern '%s': %s", pattern, pse.getMessage());
            }
        else {
            List<String> patternList = conf.getStringList("buildWorlds", null);
            if (patternList == null)
                buildWorlds.add(Pattern.compile(".*"));
            else {
                for (String pat : patternList) {
                    try {
                        buildWorlds.add(Pattern.compile(pat));
                    } catch (PatternSyntaxException pse) {
                        throw new DesignException("invalid buildWorld pattern '%s': %s", pat, pse.getMessage());
                    }
                }
            }
        }

        List<String> blockKeys = conf.getKeys("blockKey");
        if (blockKeys == null)
            throw new DesignException("blockKey mappings are required");
        Map<Character,DesignBlockDetail> blockKey = new HashMap<Character,DesignBlockDetail>();
        for (String key : blockKeys) {
            if (key.length() > 1)
                throw new DesignException("blockKey keys must be a single character: %s", key);
            if (blockKey.containsKey(key.charAt(0)))
                throw new DesignException("blockKey key '%s' is already defined", key);
            DesignBlockDetail db;
            TypeMap blockKeyMap = conf.getMap("blockKey." + key);
            if (blockKeyMap == null) {
                String blockType = conf.getString("blockKey." + key);
                if (blockType == null)
                    throw new DesignException("missing material for blockKey key '%s'", key);
                else
                    db = new DesignBlockDetail(blockType);
            } else
                db = new DesignBlockDetail(blockKeyMap);
            blockKey.put(key.charAt(0), db);
        }

        blocks = new ArrayList<DesignBlock>();
        sizeX = sizeY = sizeZ = -1;
        int x, y, z;
        List<Object> blocksMap = conf.getList("blocks");
        if (blocksMap == null)
            throw new DesignException("at least one block slice is required");
        sizeZ = blocksMap.size();
        z = -1;
        for (Object o : blocksMap) {
            z++;
            if ((! (o instanceof List)) ||
                ((List)o).isEmpty() ||
                (! (((List)o).get(0) instanceof String)))
                throw new DesignException("block slice %d is not a list of strings", z);
            List<String> lines = (List<String>)o;
            if (sizeY == -1)
                sizeY = lines.size();
            else if (sizeY != lines.size())
                throw new DesignException("block slice %d does not have %d lines", z, sizeY);
            y = sizeY;
            for (String line : lines) {
                y--;
                line = line.trim();
                if (sizeX == -1)
                    sizeX = line.length();
                else if (sizeX != line.length())
                    throw new DesignException("block slice %d, line %d does not have %d blocks", z, sizeY - y, sizeX);
                x = -1;
                for (char ch : line.toCharArray()) {
                    x++;
                    if (! blockKey.containsKey(ch))
                        throw new DesignException("block slice %d, line %d, block %d '%s' does not have a mapping in the blockKey", z, sizeY - y, x, ch);
                    DesignBlockDetail db = blockKey.get(ch);
                    if (db == null)
                        throw new DesignException("unknown block key '%s'", ch);
                    blocks.add(new DesignBlock(x, y, z, db));
                }
            }
        }

        if (name == null)
            throw new DesignException("name is required");
        if (! isValidName(name))
            throw new DesignException("name is not valid");

        if (sizeX > 255)
            throw new DesignException("must be less than 255 blocks wide");
        if (sizeY > 255)
            throw new DesignException("must be less than 255 blocks high");
        if (sizeZ > 255)
            throw new DesignException("must be less than 255 blocks deep");
//        if ((sizeX * sizeY * sizeZ) < 4)
//            throw new DesignException("volume of gate must be at least 4 cubic meters");

        int screenCount = 0,
            triggerCount = 0,
            spawnCount = 0,
            portalCount = 0,
            insertCount = 0;
        for (DesignBlock db : blocks) {
            DesignBlockDetail d = db.getDetail();
            if (d.isScreen()) screenCount++;
            if (d.isTrigger()) triggerCount++;
            if (d.isPortal()) portalCount++;
            if (d.isInsert()) insertCount++;
            if (d.isSpawn()) spawnCount++;
        }

        if (screenCount == 0)
            throw new DesignException("must have at least one screen block");
        if (triggerCount == 0)
            throw new DesignException("must have at least one trigger block");
        if (insertCount != 1)
            throw new DesignException("must have exactly one insert block");
        if (portalCount == 0)
            throw new DesignException("must have at least one portal block");
        if (spawnCount == 0)
            throw new DesignException("must have at least one spawn block");
    }

    public void dump(Context ctx) {
        Utils.debug("Design:");
        Utils.debug("  Blocks:");
        for (DesignBlock db : blocks) {
            Utils.debug("    %s", db);
        }
    }

    @Override
    public String toString() {
        return name + ((attribution == null) ? "" : (" " + attribution));
    }

    public List<DesignBlock> getBlocks() {
        return blocks;
    }

    public String getName() {
        return name;
    }

    public String getAttribution() {
        return attribution;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isAlwaysOpen() {
        return duration == -1;
    }

    public boolean isBuildable() {
        return buildable;
    }

    public boolean isCreatable() {
        return creatable;
    }

    public boolean mustBuildFromInventory() {
        return buildFromInventory;
    }

    public boolean getLinkLocal() {
        return linkLocal;
    }

    public boolean getLinkWorld() {
        return linkWorld;
    }

    public String getLinkNoneFormat() {
        return linkNoneFormat;
    }

    public String getLinkUnselectedFormat() {
        return linkUnselectedFormat;
    }

    public String getLinkOfflineFormat() {
        return linkOfflineFormat;
    }

    public String getLinkLocalFormat() {
        return linkLocalFormat;
    }

    public String getLinkWorldFormat() {
        return linkWorldFormat;
    }

    public boolean getRestoreOnClose() {
        return restoreOnClose;
    }

    public boolean getProtect() {
        return protect;
    }

    public String getTeleportFormat() {
        return teleportFormat;
    }

    public String getNoLinksFormat() {
        return noLinksFormat;
    }

    public String getNoLinkSelectedFormat() {
        return noLinkSelectedFormat;
    }

    public String getInvalidLinkFormat() {
        return invalidLinkFormat;
    }

    public String getUnknownLinkFormat() {
        return unknownLinkFormat;
    }

    public boolean getHidden() {
        return hidden;
    }

    public int getCountdown() {
        return countdown;
    }

    public int getCountdownInterval() {
        return countdownInterval;
    }

    public String getCountdownFormat() {
        return countdownFormat;
    }

    public String getCountdownIntervalFormat() {
        return countdownIntervalFormat;
    }

    public String getCountdownCancelFormat() {
        return countdownCancelFormat;
    }

    private Collection<DesignBlock> getScreenBlocks() {
        Collection<DesignBlock> screens = new ArrayList<DesignBlock>();
        for (DesignBlock db : blocks)
            if (db.getDetail().isScreen())
                screens.add(db);
        return screens;
    }

    private DesignBlock getInsertBlock() {
        for (DesignBlock db : blocks)
            if (db.getDetail().isInsert()) return db;
        return null;
    }

    public boolean isBuildableInWorld(World world) {
        String worldName = world.getName();
        for (Pattern pattern : buildWorlds)
            if (pattern.matcher(worldName).matches()) return true;
        return false;
    }

    public Map<Material,Integer> getInventoryBlocks() {
        Map<Material,Integer> ib = new EnumMap<Material,Integer>(Material.class);
        for (DesignBlock db : blocks)
            if (db.getDetail().isInventory()) {
                Material m = db.getDetail().getBuildBlock().getMaterial();
                if (ib.containsKey(m))
                    ib.put(m, ib.get(m) + 1);
                else
                    ib.put(m, 1);
            }
        return ib;
    }

    // Builds a gate at the specified location.
    // Location must include a yaw that indicates the gate's direction.
    public DesignMatch build(Location location, Player player) throws DesignException {

        // must be in a buildable world
        World world = location.getWorld();
        String worldName = world.getName();
        boolean matched = false;
        for (Pattern pattern : buildWorlds)
            if (pattern.matcher(worldName).matches()) {
                matched = true;
                break;
            }
        if (! matched)
            throw new DesignException("unable to build in this world");

        DesignBlock insertBlock = getInsertBlock();

        BlockFace direction = Utils.yawToCourseDirection(location.getYaw());
//        while (yaw < 0) yaw += 360;
//        if ((yaw > 315) || (yaw <= 45)) direction = BlockFace.WEST;
//        else if ((yaw > 45) && (yaw <= 135)) direction = BlockFace.NORTH;
//        else if ((yaw > 135) && (yaw <= 225)) direction = BlockFace.EAST;
//        else direction = BlockFace.SOUTH;

        Utils.debug("location=%s", Utils.blockCoords(location));
        Utils.debug("direction=%s", direction);

        // adjust location to represent 0,0,0 of design blocks
        switch (direction) {
            case NORTH:
                translate(location, -insertBlock.getX(), -insertBlock.getY(), -insertBlock.getZ());
//                translate(location, insertBlock.getZ(), -insertBlock.getY(), -insertBlock.getX());
                break;
            case EAST:
                translate(location, insertBlock.getZ(), -insertBlock.getY(), -insertBlock.getX());
//                translate(location, insertBlock.getX(), -insertBlock.getY(), insertBlock.getZ());
                break;
            case SOUTH:
                translate(location, insertBlock.getX(), -insertBlock.getY(), insertBlock.getZ());
//                translate(location, -insertBlock.getZ(), -insertBlock.getY(), insertBlock.getX());
                break;
            case WEST:
                translate(location, -insertBlock.getZ(), -insertBlock.getY(), insertBlock.getX());
//                translate(location, -insertBlock.getX(), -insertBlock.getY(), -insertBlock.getZ());
                break;
            default: break;
        }

        Utils.debug("new location=%s", Utils.blockCoords(location));

        if ((location.getBlockY() + sizeY) > 255)
            throw new DesignException("insertion point is too high to build");
        if (location.getBlockY() < 0)
            throw new DesignException("insertion point is too low to build");

        TransformedDesign tDesign = new TransformedDesign(this, location, direction);

        // check blocks that will be replaced (can't build in bedrock)
        while (tDesign.hasMoreBlocks()) {
            GateBlock gb = tDesign.nextBlock();
            if (! gb.getDetail().isBuildable()) continue;
            if (gb.getLocation().getBlock().getType() == Material.BEDROCK)
                throw new DesignException("unable to build in bedrock");
        }
        tDesign.reset();

        // build it!
        List<SavedBlock> savedBlocks = new ArrayList<SavedBlock>();
        while (tDesign.hasMoreBlocks()) {
            GateBlock gb = tDesign.nextBlock();
            if (! gb.getDetail().isBuildable()) continue;
            savedBlocks.add(new SavedBlock(gb.getLocation()));
            gb.getDetail().getBuildBlock().build(gb.getLocation());
        }
        Designs.setBuildUndo(player, savedBlocks);
        return new DesignMatch(this, tDesign, world, direction);
    }

    // Attempts to match the blocks around the given location with this design.
    // The location should be the location of one of the design's screen.
    public DesignMatch matchScreen(Location location) {
        Utils.debug("checking design '%s'", name);

        // must be in a buildable world
        World world = location.getWorld();
        String worldName = world.getName();
        boolean matched = false;
        for (Pattern pattern : buildWorlds)
            if (pattern.matcher(worldName).matches()) {
                matched = true;
                break;
            }
        if (! matched) return null;
        Utils.debug("world is OK");

        Block targetBlock = location.getBlock();
        BlockFace direction = null;
        TransformedDesign tDesign = null;
        matched = false;

        // iterate over each screen trying to find a match with what's around the targetBlock
        for (DesignBlock screenBlock : getScreenBlocks()) {
            location = targetBlock.getLocation();
            direction = screenBlock.getDetail().getBuildBlock().matchTypeAndDirection(targetBlock);
            if (direction == null) continue;

            Utils.debug("screen %s,%s,%s", screenBlock.getX(), screenBlock.getY(), screenBlock.getZ());
            Utils.debug("direction=%s", direction);

            // adjust location to represent 0,0,0 of design blocks
            switch (direction) {
                case NORTH:
                    translate(location, -screenBlock.getX(), -screenBlock.getY(), -screenBlock.getZ());
//                    translate(location, screenBlock.getZ(), -screenBlock.getY(), -screenBlock.getX());
                    break;
                case EAST:
                    translate(location, screenBlock.getZ(), -screenBlock.getY(), -screenBlock.getX());
//                    translate(location, screenBlock.getX(), -screenBlock.getY(), screenBlock.getZ());
                    break;
                case SOUTH:
                    translate(location, screenBlock.getX(), -screenBlock.getY(), screenBlock.getZ());
//                    translate(location, -screenBlock.getZ(), -screenBlock.getY(), screenBlock.getX());
                    break;
                case WEST:
                    translate(location, -screenBlock.getZ(), -screenBlock.getY(), screenBlock.getX());
//                    translate(location, -screenBlock.getX(), -screenBlock.getY(), -screenBlock.getZ());
                    break;
                default:
                    continue;
            }
            Utils.debug("matched a screen");
            Utils.debug("direction=%s", direction);

            tDesign = new TransformedDesign(this, location, direction);

            // check the target blocks to make sure they match the design
            matched = true;
            while (tDesign.hasMoreBlocks()) {
                GateBlock gb = tDesign.nextBlock();
                if (gb.getDetail().isMatchable() &&
                    (! gb.getDetail().getBuildBlock().matches(gb.getLocation()))) {
                    matched = false;
                    break;
                }
            }
            tDesign.reset();
            if (matched) break;
            Utils.debug("blocks don't match");
        }

        if (! matched) {
            Utils.debug("didn't match design");
            return null;
        }
        Utils.debug("matched design!");

        return new DesignMatch(this, tDesign, world, direction);
    }

    // Returns a new gate if a match in the surrounding blocks is found, otherwise null.
    public GateImpl create(DesignMatch match, Player player, String gateName) throws GateException {
        GateImpl gate = new GateImpl(match.world, gateName, player, match.direction, this, match.tDesign);
        return gate;
    }

    // Builds a gate at the specified location, creates it, and returns it.
    // The location must contain a yaw that indicates the gate direction.
    public GateImpl create(Location location, Player player, String gateName) throws TransporterException {
        DesignMatch match = build(location, player);
        return create(match, player, gateName);
    }

    private Location translate(Location loc, int dx, int dy, int dz) {
        loc.setX(loc.getBlockX() + dx);
        loc.setY(loc.getBlockY() + dy);
        loc.setZ(loc.getBlockZ() + dz);
        return loc;
    }

}

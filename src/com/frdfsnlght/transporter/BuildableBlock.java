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

import com.frdfsnlght.transporter.api.TypeMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.Sign;
import org.bukkit.material.Directional;
import org.bukkit.material.Openable;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class BuildableBlock {

    protected Material type;
    protected BlockData data;
    protected String[] lines = null;
    protected boolean physics = false;

    public BuildableBlock(BuildableBlock src, BlockFace direction) {
        this.type = src.type;
        this.data = src.data;
        this.lines = src.lines;
        this.physics = src.physics;
        rotate(direction);
    }

    public BuildableBlock(Location location) {
        Block block = location.getBlock();
        type = block.getType();
        data = block.getBlockData();
        BlockState state = block.getState();
        if (state instanceof Sign)
            lines = ((Sign)state).getLines();
        physics = false;
    }

    public BuildableBlock(String str) throws BlockException {
        type = Material.getMaterial(str);
        data = type.createBlockData();
    }

    public BuildableBlock(TypeMap map) throws BlockException {
        type = Material.getMaterial(map.getString("type"));
        data = type.createBlockData();
        physics = map.getBoolean("physics", false);

        if (type == null) return;

        String str;
        if (data instanceof Directional) {
            str = map.getString("facing");
            if (str != null) {
                try {
                    ((Directional)data).setFacingDirection(Utils.valueOf(BlockFace.class, str));
                } catch (IllegalArgumentException iae) {
                    throw new BlockException(iae.getMessage() + " facing '%s'", str);
                }
            }
        }
        if (data instanceof MultipleFacing) {
            str = map.getString("mfacing");
            if (str != null) {
                String[] faces = str.split("\n");
                for(int i = 0; i < faces.length; i++) {
                    try {
                        ((MultipleFacing)data).setFace(Utils.valueOf(BlockFace.class, faces[i]), true);
                    } catch (IllegalArgumentException iae) {
                        throw new BlockException(iae.getMessage() + " multiple-facing '%s'", str);
                    }
                }
            }
        }
        if (data instanceof Openable) {
            str = map.getString("open");
            if (str != null)
                ((Openable)data).setOpen(map.getBoolean("open"));
        }

        str = map.getString("lines");
        if (str != null) {
            lines = str.split("\n");
            if (lines.length > 4)
                lines = Arrays.copyOfRange(lines, 0, 3);
            for (int i = 0; i < lines.length; i++)
                if (lines[i].length() > 15)
                    lines[i] = lines[i].substring(0, 15);
        }

    }

    public Map<String,Object> encode() {
        Map<String,Object> node = new HashMap<String,Object>();
        node.put("type", type.toString());
        if (data instanceof Directional) {
            node.put("facing", ((Directional)data).toString());
        }
        if (data instanceof MultipleFacing) {
            StringBuilder buf = new StringBuilder();
            for (BlockFace face: ((MultipleFacing)data).getFaces()) {
                if (buf.length() > 0) buf.append("\n");
                buf.append(face.toString());
            }
            node.put("mfacing", buf.toString());
        }
        if (data instanceof Openable) {
            node.put("open", ((Openable)data).toString());
        }
        if (physics) node.put("physics", physics);
        if (lines != null) {
            StringBuilder buf = new StringBuilder();
            for (String line : lines) {
                if (buf.length() > 0) buf.append("\n");
                buf.append(line);
            }
            node.put("lines", buf.toString());
        }
        return node;
    }

    public boolean hasType() {
        return type != null;
    }

    public Material getType() {
        return type;
    }

    public BlockData getData() {
        return data;
    }

    public String[] getLines() {
        return lines;
    }

    public Block build(Location location) {
        Block block = location.getBlock();
        block.setType(type, physics);
        block.setBlockData(data);
        if (lines != null) {
            BlockState sign = block.getState();
            if (sign instanceof Sign) {
                for (int i = 0; i < lines.length; i++)
                    ((Sign)sign).setLine(i, lines[i]);
                sign.update();
            }
        }
        return block;
    }

    public boolean matches(Location location) {
        return matches(location.getBlock());
    }

    public boolean matches(Block block) {
        Utils.debug("match %s to %s", this, Utils.block(block));

        if (block.getType() != type) {
            return false;
        }

        // can't simply compare data values because signs can have multiple values indicating
        // the same facing direction!
        BlockData otherData = block.getBlockData();
        if ((data instanceof Directional) &&
            (otherData instanceof Directional)) {
            return ((Directional)data).getFacing() == ((Directional)otherData).getFacing();
            // this is broken if there are other aspects to compare
        }
        if (otherData != data) return false;
        // we don't care about matching lines on a sign
        return true;
    }

    // only applied to screens (i.e., signs)
    public BlockFace matchTypeAndDirection(Block block) {
        if (block.getType() != type) return null;

        if (data == null) return null;
        if (! (data instanceof Directional)) return null;
        Directional myDir = (Directional)data;
        if ((myDir.getFacing() == BlockFace.UP) ||
            (myDir.getFacing() == BlockFace.DOWN)) return null;

        BlockData otherData = block.getBlockData();
        if (otherData == null) return null;
        if (! (otherData instanceof Directional)) return null;
        Directional otherDir = (Directional)otherData;
        if ((otherDir.getFacing() == BlockFace.UP) ||
            (otherDir.getFacing() == BlockFace.DOWN)) return null;

        float fromYaw = Utils.directionToYaw(myDir.getFacing());
        float toYaw = Utils.directionToYaw(otherDir.getFacing());
        float result = toYaw - fromYaw + 180;
//        float result = toYaw - fromYaw + 90;

        Utils.debug("fromYaw=%s", fromYaw);
        Utils.debug("toYaw=%s", toYaw);
        Utils.debug("result=%s", result);

        return Utils.yawToDirection(result);
    }

    public boolean isSign() {
        return type == Material.WALL_SIGN;
    }

    public void rotate(BlockFace to) {
        if (! (data instanceof Directional)) return;

        Directional dir = (Directional)data;
        dir.setFacingDirection(Utils.rotate(dir.getFacing(), to));
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("BuildableBlock[");
        buf.append(type.toString()).append(",");
        buf.append(data.getAsString()).append(",");
        buf.append(physics);
        if (lines != null)
            buf.append(",").append(lines.length).append(" lines");
        buf.append("]");
        return buf.toString();
    }

    @Override
    public int hashCode() {
        return type.hashCode() + data.hashCode() + ((lines != null) ? lines.hashCode() : 0) + (physics ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (! (obj instanceof BuildableBlock)) return false;
        BuildableBlock other = (BuildableBlock)obj;

        if (type != other.type) return false;
        if (data != other.data) return false;
        if (physics != other.physics) return false;

        if ((lines == null) && (other.lines != null)) return false;
        if ((lines != null) && (other.lines == null)) return false;
        if ((lines != null) && (other.lines != null)) {
            if (lines.length != other.lines.length) return false;
            for (int i = 0; i < lines.length; i++)
                if (! lines[i].equals(other.lines[i])) return false;
        }
        return true;
    }

}

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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Worlds {

    //public static final File WorldBaseFolder = Utils.BukkitBaseFolder;
    private static final Map<String,WorldImpl> worlds = new HashMap<String,WorldImpl>();

    public static void onConfigLoad(Context ctx) {
        worlds.clear();

        // add default worlds if they don't exist
        for (String name : new String[] { "world", "world_nether", "world_the_end"} ) {
            World world = Global.plugin.getServer().getWorld(name);
            if (world == null) continue;
            try {
                add(world);
            } catch (WorldException e) {}
        }

        List<TypeMap> worldMaps = Config.getMapList("worlds");
        if (worldMaps != null) {
            for (TypeMap map : worldMaps) {
                try {
                    WorldImpl world = new WorldImpl(map);
                    if (add(world)) {
                        if (Global.started && Config.getAutoLoadWorlds() && world.getAutoLoad())
                            world.load(ctx);
                    }
                } catch (WorldException e) {
                    ctx.warn(e.getMessage());
                }
            }
        }

        if (Global.started)
            autoLoad(ctx);
    }

    public static void onConfigSave() {
        List<Map<String,Object>> worldNodes = new ArrayList<Map<String,Object>>();
        for (WorldImpl world : worlds.values())
            worldNodes.add(world.encode());
        Config.setPropertyDirect("worlds", worldNodes);
    }

    public static void autoLoad(Context ctx) {
        if (! Config.getAutoLoadWorlds()) return;
        for (WorldImpl world : worlds.values())
            if (world.getAutoLoad())
                world.load(ctx);
    }

    public static WorldImpl add(World world) throws WorldException {
        if (worlds.containsKey(world.getName())) return null;
        WorldImpl wp = new WorldImpl(world.getName(), world.getEnvironment(), null, world.getSeed() + "");
        add(wp);
        return wp;
    }

    public static boolean add(WorldImpl world) {
        return worlds.put(world.getName(), world) == null;
    }

    public static void remove(WorldImpl world) {
        worlds.remove(world.getName());
    }

    public static WorldImpl get(String name) {
        return worlds.get(name);
    }

    public static WorldImpl find(String name) {
        if (worlds.containsKey(name)) return worlds.get(name);
        WorldImpl world = null;
        name = name.toLowerCase();
        for (String key : worlds.keySet()) {
            if (key.toLowerCase().startsWith(name)) {
                if (world == null) world = worlds.get(key);
                else return null;
            }
        }
        return world;
    }

    public static List<WorldImpl> getAll() {
        return new ArrayList<WorldImpl>(worlds.values());
    }

    public static boolean isEmpty() {
        return size() == 0;
    }

    public static int size() {
        return worlds.size();
    }

    public static File worldFolder(String name) {
        return new File(Bukkit.getWorldContainer(), name);
    }

    public static File worldFolder(World world) {
        return new File(Bukkit.getWorldContainer(), world.getName());
    }

    public static File worldPluginFolder(World world) {
        return new File(worldFolder(world), Global.pluginName);
    }

}

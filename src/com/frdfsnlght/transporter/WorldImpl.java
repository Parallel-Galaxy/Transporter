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
import com.frdfsnlght.transporter.exceptions.OptionsException;
import com.frdfsnlght.transporter.exceptions.PermissionsException;
import com.frdfsnlght.transporter.exceptions.WorldException;
import com.frdfsnlght.transporter.listeners.OptionsListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.frdfsnlght.transporter.api.LocalWorld;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class WorldImpl implements OptionsListener, LocalWorld {

    private static final Set<String> OPTIONS = new HashSet<String>();

    static {
        OPTIONS.add("environment");
        OPTIONS.add("generator");
        OPTIONS.add("seed");
        OPTIONS.add("autoLoad");
    }

    public static boolean isValidName(String name) {
        if (name.length() == 0) return false;
        return ! (name.contains(".") || name.contains("*"));
    }

    private Options options = new Options(this, OPTIONS, "trp.world", this);
    private String name;
    private Environment environment;
    private String generator;
    private String seed;
    private boolean autoLoad;

    public WorldImpl(String name, Environment environment, String generator, String seed) throws WorldException {
        try {
            setName(name);
            setEnvironment(environment);
            setGenerator(generator);
            setSeed(seed);
            autoLoad = true;
        } catch (IllegalArgumentException e) {
            throw new WorldException(e.getMessage());
        }
    }

    public WorldImpl(TypeMap map) throws WorldException {
        try {
            setName(map.getString("name"));

            // try to convert old environment to new environment/generator
            String envStr = map.getString("environment", "NORMAL");
            if (envStr.equals("SKYLANDS")) envStr = "NORMAL";
            try {
                setEnvironment(Utils.valueOf(Environment.class, envStr));
            } catch (IllegalArgumentException iae) {
                throw new WorldException(iae.getMessage() + " environment");
            }

            setGenerator(map.getString("generator", null));
            setSeed(map.getString("seed", null));
            setAutoLoad(map.getBoolean("autoLoad", true));
        } catch (IllegalArgumentException e) {
            throw new WorldException(e.getMessage());
        }
    }

    
    public String getName() {
        return name;
    }

    private void setName(String name) {
        if (name == null)
            throw new IllegalArgumentException("name is required");
        if (! isValidName(name))
            throw new IllegalArgumentException("name is not valid");
        this.name = name;
    }

    /* Begin options */

    
    public Environment getEnvironment() {
        return environment;
    }

    
    public void setEnvironment(Environment environment) {
        if (environment == null) environment = Environment.NORMAL;
        this.environment = environment;
    }

    
    public String getGenerator() {
        return generator;
    }

    
    public void setGenerator(String generator) {
        if ((generator != null) &&
            (generator.isEmpty() || generator.equals("-"))) generator = null;
        this.generator = generator;
    }

    
    public String getSeed() {
        return seed;
    }

    
    public void setSeed(String seed) {
        if ((seed != null) &&
            (seed.isEmpty() || seed.equals("-"))) seed = null;
        this.seed = seed;
    }

    
    public boolean getAutoLoad() {
        return autoLoad;
    }

    
    public void setAutoLoad(boolean b) {
        autoLoad = b;
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
        ctx.send("option '%s' set to '%s' for world '%s'", name, value, getName());
    }

    
    public String getOptionPermission(Context ctx, String name) {
        return name;
    }

    /* End options */

    public Map<String,Object> encode() {
        Map<String,Object> node = new HashMap<String,Object>();
        node.put("name", name);
        node.put("environment", environment.toString());
        node.put("generator", generator);
        node.put("seed", seed);
        node.put("autoLoad", autoLoad);
        return node;
    }

    
    public World getWorld() {
        return Bukkit.getWorld(name);
    }

    public World load(Context ctx) {
        World world = getWorld();
        boolean loadEndpoints = (world != null);
        if (world == null) {
            ctx.send("loading world '%s'...", name);
            WorldCreator wc = new WorldCreator(name);
            wc.environment(environment);
            wc.generator(generator);
            if (seed != null)
                try {
                    wc.seed(Long.parseLong(seed));
                } catch (NumberFormatException e) {
                    wc.seed(seed.hashCode());
                }
            world = wc.createWorld();
            if (seed == null) {
                seed = world.getSeed() + "";
                ctx.send("seed set to %s", seed);
            }
        } else {
            ctx.send("world '%s' is already loaded", name);
            if (seed == null)
                seed = world.getSeed() + "";
        }
        if (loadEndpoints)
            Gates.loadGatesForWorld(ctx, world);
        return world;
    }

    public World unload() {
        World world = getWorld();
        if (world != null) {
            Bukkit.unloadWorld(world, true);
            // Bukkit onWorldUnloaded event handler should do this, but just to be sure...
            Gates.removeGatesForWorld(world);
        }
        return world;
    }

    
    public boolean isLoaded() {
        return getWorld() != null;
    }

}

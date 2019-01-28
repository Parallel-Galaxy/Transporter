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

import com.frdfsnlght.transporter.GateMap.Volume;
import com.frdfsnlght.transporter.api.event.GateCreateEvent;
import com.frdfsnlght.transporter.api.event.GateDestroyEvent;
import com.frdfsnlght.transporter.exceptions.GateException;
import com.frdfsnlght.transporter.exceptions.TransporterException;
import com.frdfsnlght.transporter.exceptions.WorldException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Manages a collection of both local and remote gates.
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Gates {

    // Gate build blocks that are protected
    private static final GateMap protectionMap = new GateMap();

    // Portal blocks for open, local gates
    private static final GateMap portalMap = new GateMap();

    // Gate screens for local gates
    private static final GateMap screenMap = new GateMap();

    // Gate switches for local gates
    public static final GateMap switchMap = new GateMap();

    // Gate triggers for local gates
    public static final GateMap triggerMap = new GateMap();

    // Indexed by full name
    private static final Map<String,GateImpl> gates = new HashMap<String,GateImpl>();

    private static Map<Integer,GateImpl> selectedGates = new HashMap<Integer,GateImpl>();

    public static void load(Context ctx) {
        clearGates();
        for (World world : Bukkit.getWorlds())
            loadGatesForWorld(ctx, world);
    }

    public static int loadGatesForWorld(Context ctx, World world) {
        File worldFolder = Worlds.worldPluginFolder(world);
        File gatesFolder = new File(worldFolder, "gates");
        if (! gatesFolder.exists()) {
            Utils.info("no gates found for world '%s'", world.getName());
            return 0;
        }
        int loadedCount = 0;
        for (File gateFile : Utils.listYAMLFiles(gatesFolder)) {
            try {
                GateImpl gate = GateImpl.load(world, gateFile);
                if (gates.containsKey(gate.getFullName())) continue;
                try {
                    add(gate, false);
                    ctx.sendLog("loaded gate '%s' for world '%s'", gate.getName(), world.getName());
                    loadedCount++;
                } catch (GateException ge) {
                    ctx.warnLog("unable to load gate '%s' for world '%s': %s", gate.getName(), world.getName(), ge.getMessage());
                }
            } catch (TransporterException te) {
                ctx.warnLog("'%s' contains an invalid gate file for world '%s': %s", gateFile.getPath(), world.getName(), te.getMessage());
            } catch (Throwable t) {
                Utils.severe(t, "there was a problem loading the gate file '%s' for world '%s':", gateFile.getPath(), world.getName());
            }
        }
        return loadedCount;
    }

    public static void save(Context ctx) {
        if (gates.isEmpty()) return;
        Set<GateImpl> lgates = getGates();
        for (GateImpl gate : lgates) {
            gate.save(true);
            if ((ctx != null) && Config.getShowGatesSavedMessage())
                ctx.sendLog("saved '%s'", gate.getFullName());
        }
        if ((ctx != null) && (! Config.getShowGatesSavedMessage()))
            ctx.sendLog("saved %s gates", lgates.size());
    }

    public static GateImpl find(Context ctx, String name) {
        int pos = name.indexOf('.');
        if (pos == -1) {
            // asking for a local gate in the player's current world
            if (! ctx.isPlayer()) return null;
            name = ctx.getPlayer().getWorld().getName() + "." + name;
        }
        return find(name);
    }

    public static GateImpl find(String name) {
        if (gates.containsKey(name)) return gates.get(name);
        String lname = name.toLowerCase();
        GateImpl gate = null;
        for (String key : gates.keySet()) {
            if (key.toLowerCase().startsWith(lname)) {
                if (gate == null) gate = gates.get(key);
                else return null;
            }
        }
        return gate;
    }

    public static GateImpl get(String name) {
        return gates.get(name);
    }

    public static void add(GateImpl gate, boolean created) throws GateException {
        if (gates.containsKey(gate.getFullName()))
            throw new GateException("a gate with the same name already exists here");
        gates.put(gate.getFullName(), gate);
        for (GateImpl lg : getGates())
            lg.onGateAdded(gate);
        
        GateCreateEvent event = new GateCreateEvent(gate);
        Bukkit.getPluginManager().callEvent(event);
        World world = gate.getWorld();
        if (Config.getAutoAddWorlds())
            try {
                WorldImpl wp = Worlds.add(world);
                if (wp != null)
                    Utils.info("automatically added world '%s' for new gate '%s'", wp.getName(), gate.getName());
            } catch (WorldException we) {}
        else if (Worlds.get(world.getName()) == null)
            Utils.warning("Gate '%s' has been added to world '%s' but the world has not been added to the plugin's list of worlds!", gate.getName(), world.getName());
    }

    public static void remove(GateImpl gate) throws GateException {
        if (! gates.containsKey(gate.getFullName()))
            throw new GateException("gate not found");
        for (GateImpl lg : getGates())
            lg.onGateRemoved(gate);
        gates.remove(gate.getFullName());
        deselectGate(gate);
        gate.save(false);
    }

    public static void destroy(GateImpl gate, boolean unbuild) {
        gates.remove(gate.getFullName());
        for (GateImpl lg : getGates())
            lg.onGateDestroyed(gate);
        deselectGate(gate);
        GateDestroyEvent event = new GateDestroyEvent(gate);
        Bukkit.getPluginManager().callEvent(event);
        gate.destroy(unbuild);
    }

    public static void rename(GateImpl gate, String newName) throws GateException {
        String oldName = gate.getName();
        String oldFullName = gate.getFullName();
        gate.setName(newName);
        String newFullName = gate.getFullName();
        if (gates.containsKey(newFullName)) {
            gate.setName(oldName);
            throw new GateException("gate name already exists");
        }
        gates.remove(oldFullName);
        gates.put(newFullName, gate);
        for (GateImpl lg : getGates())
            lg.onGateRenamed(gate, oldFullName);
        gate.onRenameComplete();
    }

    public static void removeGatesForWorld(World world) {
        for (GateImpl lg : getGates()) {
            if (lg.getWorld() == world)
                try {
                    remove(lg);
                } catch (GateException ee) {}
        }
    }

    public static GateImpl getGate(String name) {
        return gates.get(name);
    }

    public static Set<GateImpl> getGates() {
        return new HashSet<GateImpl>(gates.values());
    }



    public static GateImpl findGateForPortal(Location loc) {
        return portalMap.getGate(loc);
    }

    public static void addPortalVolume(Volume vol) {
        portalMap.put(vol);
    }

    public static void removePortalVolume(GateImpl gate) {
        portalMap.removeGate(gate);
    }

    public static GateImpl findGateForProtection(Location loc) {
        return protectionMap.getGate(loc);
    }

    public static void addProtectionVolume(Volume vol) {
        protectionMap.put(vol);
    }

    public static void removeProtectionVolume(GateImpl gate) {
        protectionMap.removeGate(gate);
    }

    public static GateImpl findGateForScreen(Location loc) {
        return screenMap.getGate(loc);
    }

    public static void addScreenVolume(Volume vol) {
        screenMap.put(vol);
    }

    public static void removeScreenVolume(GateImpl gate) {
        screenMap.removeGate(gate);
    }

    public static GateImpl findGateForSwitch(Location loc) {
        return switchMap.getGate(loc);
    }

    public static void addSwitchVolume(Volume vol) {
        switchMap.put(vol);
    }

    public static void removeSwitchVolume(GateImpl gate) {
        switchMap.removeGate(gate);
    }

    public static GateImpl findGateForTrigger(Location loc) {
        return triggerMap.getGate(loc);
    }

    public static void addTriggerVolume(Volume vol) {
        triggerMap.put(vol);
    }

    public static void removeTriggerVolume(GateImpl gate) {
        triggerMap.removeGate(gate);
    }

    public static void dumpMaps() {
        if (!Config.getDebug()) {
            Utils.info("You need to activate the debug mode first. Use: " + ChatColor.BOLD + "/trp set debug true");
            return;
        }
        Utils.debug("portalMap=%s", portalMap);
        Utils.debug("protectionMap=%s", protectionMap);
        Utils.debug("screenMap=%s", screenMap);
        Utils.debug("switchMap=%s", switchMap);
        Utils.debug("triggerMap=%s", triggerMap);
    }


    public static void setSelectedGate(Player player, GateImpl gate) {
        selectedGates.put((player == null) ? Integer.MAX_VALUE : player.getEntityId(), gate);
    }

    public static GateImpl getSelectedGate(Player player) {
        return selectedGates.get((player == null) ? Integer.MAX_VALUE : player.getEntityId());
    }

    public static void deselectGate(GateImpl gate) {
        for (Integer playerId : new ArrayList<Integer>(selectedGates.keySet()))
            if (selectedGates.get(playerId) == gate)
                selectedGates.remove(playerId);
    }

    private static void clearGates() {
        for (GateImpl gate : new HashSet<GateImpl>(gates.values()))
            gates.remove(gate.getFullName());
    }

}

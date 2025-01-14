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
import com.frdfsnlght.transporter.api.ReservationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Realm {

    private static final Set<String> OPTIONS = new HashSet<String>();

    private static final Options options;

    static {
        OPTIONS.add("name");
        OPTIONS.add("defaultServer");
        OPTIONS.add("defaultWorld");
        OPTIONS.add("defaultGate");
        OPTIONS.add("respawn");
        OPTIONS.add("respawnGate");
        OPTIONS.add("serverOfflineFormat");
        OPTIONS.add("restoreWhenServerOffline");
        OPTIONS.add("restoreWhenServerOfflineFormat");
        OPTIONS.add("kickWhenServerOffline");
        OPTIONS.add("kickWhenServerOfflineFormat");

        options = new Options(Realm.class, OPTIONS, "trp.realm", new OptionsListener() {
            public void onOptionSet(Context ctx, String name, String value) {
                ctx.send("realm option '%s' set to '%s'", name, value);
            }
            public String getOptionPermission(Context ctx, String name) {
                return name;
            }
        });
    }

    private static boolean started = false;
    private static Set<String> redirectedPlayers = new HashSet<String>();
    private static Set<String> respawningPlayers = new HashSet<String>();

    public static boolean isStarted() {
        return started;
    }

    // called from main thread
    public static void start(Context ctx) {
        if (! getEnabled()) return;
        try {
            if (getName() == null)
                throw new RealmException("name is not set");

            redirectedPlayers.clear();

            started = true;
            ctx.send("realm support started");

            for (Server server : Servers.getAll())
                if (server.isConnected()) server.sendRefreshData();

        } catch (RealmException e) {
            ctx.warn("realm support cannot be started: %s", e.getMessage());
        }
    }

    // called from main thread
    public static void stop(Context ctx) {
        if (! started) return;
        started = false;
        respawningPlayers.clear();
        ctx.send("realm support stopped");
    }

    public static void onConfigLoad(Context ctx) {}

    public static void onConfigSave() {}

    // Player events

    public static void onTeleport(Player player, Location toLocation) {
        if (! started) return;
        if (redirectedPlayers.contains(player.getName())) return;
        if (! respawningPlayers.remove(player.getName())) return;
        Utils.debug("realm respawn '%s'", player.getName());
        if (! getRespawn()) return;
        GateImpl respawnGate = getRespawnGateImpl();
        if (respawnGate != null)
            sendPlayerToGate(player, respawnGate);
    }

    public static boolean onJoin(Player player) {
        if (! started) return false;
        Utils.debug("realm join '%s'", player.getName());
        redirectedPlayers.remove(player.getName());
        return false;
    }

    public static void onRespawn(Player player) {
        if (! started) return;
        respawningPlayers.add(player.getName());
    }


    // End Player events

    private static boolean sendPlayerToGate(Player player, GateImpl gate) {
        try {
            ReservationImpl res = new ReservationImpl(player, gate);
            res.depart();
            return true;
        } catch (ReservationException re) {
            Utils.warning("Reservation exception while sending player '%s' to gate '%s': %s", player.getName(), gate.getLocalName(), re.getMessage());
            return false;
        }
    }

    public static boolean getEnabled() {
        return Config.getBooleanDirect("realm.enabled", false);
    }

    public static void setEnabled(Context ctx, boolean b) {
        Config.setPropertyDirect("realm.enabled", b);
        stop(ctx);
        if (b) start(ctx);
    }

    /* Begin options */

    public static String getName() {
        return Config.getStringDirect("realm.name", null);
    }

    public static void setName(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        Config.setPropertyDirect("realm.name", s);
    }

    public static String getDefaultServer() {
        return Config.getStringDirect("realm.defaultServer", null);
    }

    public static void setDefaultServer(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        if (s != null) {
            Server server = Servers.find(s);
            if (server == null)
                throw new IllegalArgumentException("unknown server");
            s = server.getName();
        }
        Config.setPropertyDirect("realm.defaultServer", s);
    }

    public static String getDefaultWorld() {
        return Config.getStringDirect("realm.defaultWorld", null);
    }

    public static void setDefaultWorld(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        if (s != null) {
            LocalWorldImpl world = Worlds.get(s);
            if (world == null)
                throw new IllegalArgumentException("unknown world");
            s = world.getName();
        }
        Config.setPropertyDirect("realm.defaultWorld", s);
    }

    public static String getDefaultGate() {
        return Config.getStringDirect("realm.defaultGate", null);
    }

    public static void setDefaultGate(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        if (s != null) {
            GateImpl gate = Gates.find(s);
            if (gate == null)
                throw new IllegalArgumentException("unknown or offline gate");
            s = gate.getLocalName();
        }
        Config.setPropertyDirect("realm.defaultGate", s);
    }

    public static boolean getRespawn() {
        return Config.getBooleanDirect("realm.respawn", true);
    }

    public static void setRespawn(boolean b) {
        Config.setPropertyDirect("realm.respawn", b);
    }

    public static String getRespawnGate() {
        return Config.getStringDirect("realm.respawnGate", null);
    }

    public static void setRespawnGate(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        if (s != null) {
            GateImpl gate = Gates.find(s);
            if (gate == null)
                throw new IllegalArgumentException("unknown or offline gate");
            s = gate.getLocalName();
        }
        Config.setPropertyDirect("realm.respawnGate", s);
    }

    public static String getServerOfflineFormat() {
        return Config.getStringDirect("realm.serverOfflineFormat", "You're not where you belong because server '%server%' is offline.");
    }

    public static void setServerOfflineFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        Config.setPropertyDirect("realm.serverOfflineFormat", s);
    }

    public static boolean getRestoreWhenServerOffline() {
        return Config.getBooleanDirect("realm.restoreWhenServerOffline", true);
    }

    public static void setRestoreWhenServerOffline(boolean b) {
        Config.setPropertyDirect("realm.restoreWhenServerOffline", b);
    }

    public static String getRestoreWhenServerOfflineFormat() {
        return Config.getStringDirect("realm.restoreWhenServerOfflineFormat", "You're not where you belong because server '%server%' is offline.");
    }

    public static void setRestoreWhenServerOfflineFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        Config.setPropertyDirect("realm.restoreWhenServerOfflineFormat", s);
    }

    public static boolean getKickWhenServerOffline() {
        return Config.getBooleanDirect("realm.kickWhenServerOffline", true);
    }

    public static void setKickWhenServerOffline(boolean b) {
        Config.setPropertyDirect("realm.kickWhenServerOffline", b);
    }

    public static String getKickWhenServerOfflineFormat() {
        return Config.getStringDirect("realm.kickWhenServerOfflineFormat", "Server '%server%' is offline.");
    }

    public static void setKickWhenServerOfflineFormat(String s) {
        if (s != null) {
            if (s.equals("*"))
                s = null;
        }
        Config.setPropertyDirect("realm.kickWhenServerOfflineFormat", s);
    }

    public static void getOptions(Context ctx, String name) throws OptionsException, PermissionsException {
        options.getOptions(ctx, name);
    }

    public static String getOption(Context ctx, String name) throws OptionsException, PermissionsException {
        return options.getOption(ctx, name);
    }

    public static void setOption(Context ctx, String name, String value) throws OptionsException, PermissionsException {
        options.setOption(ctx, name, value);
    }

    /* End options */

    private static GateImpl getDefaultGateImpl() {
        String gName = getDefaultGate();
        if (gName == null) return null;
        GateImpl gate = Gates.get(gName);
        return gate;
    }

    private static GateImpl getRespawnGateImpl() {
        String gName = getRespawnGate();
        if (gName == null) return null;
        GateImpl gate = Gates.get(gName);
        return gate;
    }

}

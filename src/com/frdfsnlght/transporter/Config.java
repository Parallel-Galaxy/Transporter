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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.frdfsnlght.transporter.api.TypeMap;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Config {

    private static final int CONFIG_VERSION = 2;

    private static final Set<String> OPTIONS = new HashSet<String>();
    private static final Options options;
    private static TypeMap config = null;

    static {
        OPTIONS.add("debug");
        OPTIONS.add("deleteDebugFile");
        OPTIONS.add("allowBuild");
        OPTIONS.add("allowLinkLocal");
        OPTIONS.add("allowLinkWorld");
        OPTIONS.add("allowSignCreation");
        OPTIONS.add("autoAddWorlds");
        OPTIONS.add("autoLoadWorlds");
        OPTIONS.add("gateLockExpiration");
        OPTIONS.add("arrivalWindow");
        OPTIONS.add("useGatePermissions");
        OPTIONS.add("consolePMFormat");
        OPTIONS.add("localPMFormat");
        OPTIONS.add("worldPMFormat");
        OPTIONS.add("serverPMFormat");
        OPTIONS.add("useVaultPermissions");
        OPTIONS.add("useVaultChat");
        OPTIONS.add("hideLocalLoginLeaveMessage");
        OPTIONS.add("exportedGatesFile");
        OPTIONS.add("worldLoadDelay");
        OPTIONS.add("showGatesSavedMessage");

        options = new Options(Config.class, OPTIONS, "trp", new OptionsListener() {
            public void onOptionSet(Context ctx, String name, String value) {
                ctx.sendLog("global option '%s' set to '%s'", name, value);
            }
            public String getOptionPermission(Context ctx, String name) {
                return name;
            }
        });
    }

    public static File getConfigFile() {
        File dataFolder = Global.plugin.getDataFolder();
        return new File(dataFolder, "config.yml");
    }

    public static void load(Context ctx) {
        File confFile = getConfigFile();
        config = new TypeMap(confFile);
        config.load();

        int version = config.getInt("configVersion", -9999);

        if (version < CONFIG_VERSION) {
            // do conversion here

        } else if (version > CONFIG_VERSION) {
            ctx.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            ctx.warn("");
            ctx.warn("The configuration file version is for a newer version of the");
            ctx.warn("plugin. Good luck with that.");
            ctx.warn("");
            ctx.warn("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }

        ctx.sendLog("loaded configuration");
        Worlds.onConfigLoad(ctx);
        Pins.onConfigLoad(ctx);
    }

    public static void save(Context ctx) {
        if (config == null) return;
        Worlds.onConfigSave();
        Pins.onConfigSave();
        File configDir = Global.plugin.getDataFolder();
        if (! configDir.exists()) configDir.mkdirs();
        config.save();
        if (ctx != null)
            ctx.sendLog("saved configuration");
    }

    public static String getStringDirect(String path) {
        return config.getString(path, null);
    }

    public static String getStringDirect(String path, String def) {
        return config.getString(path, def);
    }

    public static int getIntDirect(String path, int def) {
        return config.getInt(path, def);
    }

    public static boolean getBooleanDirect(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    public static List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public static List<TypeMap> getMapList(String path) {
        return config.getMapList(path);
    }

    public static TypeMap getMap(String path) {
        return config.getMap(path);
    }

    public static void setPropertyDirect(String path, Object v) {
        if (config == null) return;
        if (v == null)
            config.remove(path);
        else
            config.set(path, v);
    }



    /* Begin options */

    public static boolean getDebug() {
        return config.getBoolean("global.debug", false);
    }

    public static void setDebug(boolean b) {
        config.set("global.debug", b);
    }

    public static boolean getDeleteDebugFile() {
        return config.getBoolean("global.deleteDebugFile", true);
    }

    public static void setDeleteDebugFile(boolean b) {
        config.set("global.deleteDebugFile", b);
    }

    public static boolean getAllowBuild() {
        return config.getBoolean("global.allowBuild", true);
    }

    public static void setAllowBuild(boolean b) {
        config.set("global.allowBuild", b);
    }

    public static boolean getAllowLinkLocal() {
        return config.getBoolean("global.allowLinkLocal", true);
    }

    public static void setAllowLinkLocal(boolean b) {
        config.set("global.allowLinkLocal", b);
    }

    public static boolean getAllowLinkWorld() {
        return config.getBoolean("global.allowLinkWorld", true);
    }

    public static void setAllowLinkWorld(boolean b) {
        config.set("global.allowLinkWorld", b);
    }

    public static boolean getAllowSignCreation() {
        return config.getBoolean("global.allowSignCreation", true);
    }

    public static void setAllowSignCreation(boolean b) {
        config.set("global.allowSignCreation", b);
    }

    public static boolean getAutoAddWorlds() {
        return config.getBoolean("global.autoAddWorlds", true);
    }

    public static void setAutoAddWorlds(boolean b) {
        config.set("global.autoAddWorlds", b);
    }

    public static boolean getAutoLoadWorlds() {
        return config.getBoolean("global.autoLoadWorlds", true);
    }

    public static void setAutoLoadWorlds(boolean b) {
        config.set("global.autoLoadWorlds", b);
    }

    public static int getGateLockExpiration() {
        return config.getInt("global.gateLockExpiration", 2000);
    }

    public static void setGateLockExpiration(int i) {
        if (i < 500)
            throw new IllegalArgumentException("gateLockExpiration must be at least 500");
        config.set("global.gateLockExpiration", i);
    }

    public static int getArrivalWindow() {
        return config.getInt("global.arrivalWindow", 20000);
    }

    public static void setArrivalWindow(int i) {
        if (i < 1000)
            throw new IllegalArgumentException("arrivalWindow must be at least 1000");
        config.set("global.arrivalWindow", i);
    }

    public static boolean getUseGatePermissions() {
        return config.getBoolean("global.useGatePermissions", false);
    }

    public static void setUseGatePermissions(boolean b) {
        config.set("global.useGatePermissions", b);
    }

    public static String getConsolePMFormat() {
        return config.getString("global.consolePMFormat", "[console] %GREEN%%message%");
    }

    public static void setConsolePMFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        setPropertyDirect("global.consolePMFormat", s);
    }

    public static String getLocalPMFormat() {
        return config.getString("global.localPMFormat", "[%fromPlayer%] %GREEN%%message%");
    }

    public static void setLocalPMFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        setPropertyDirect("global.localPMFormat", s);
    }

    public static String getWorldPMFormat() {
        return config.getString("global.worldPMFormat", "[%fromPlayer%/%fromWorld%] %GREEN%%message%");
    }

    public static void setWorldPMFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        setPropertyDirect("global.worldPMFormat", s);
    }

    public static String getServerPMFormat() {
        return config.getString("global.serverPMFormat", "[%fromPlayer%/%fromWorld%@%fromServer%] %GREEN%%message%");
    }

    public static void setServerPMFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        setPropertyDirect("global.serverPMFormat", s);
    }

    public static boolean getUseVaultPermissions() {
        return config.getBoolean("global.useVaultPermissions", false);
    }

    public static void setUseVaultPermissions(boolean b) {
        config.set("global.useVaultPermissions", b);
    }

    public static boolean getUseVaultChat() {
        return config.getBoolean("global.useVaultChat", false);
    }

    public static void setUseVaultChat(boolean b) {
        config.set("global.useVaultChat", b);
    }
    
    public static boolean getHideLocalLoginLeaveMessage() {
        return config.getBoolean("global.hideLocalLoginLeaveMessage", true);
    }

    public static void setHideLocalLoginLeaveMessage(boolean b) {
        config.set("global.hideLocalLoginLeaveMessage", b);
    }

    public static String getExportedGatesFile() {
        return config.getString("global.exportedGatesFile", null);
    }

    public static void setExportedGatesFile(String s) {
        if ((s != null) && (s.equals("-") || s.equals("*"))) s = null;
        setPropertyDirect("global.exportedGatesFile", s);
    }

    public static int getWorldLoadDelay() {
        return config.getInt("global.worldLoadDelay", 5000);
    }

    public static void setWorldLoadDelay(int i) {
        if (i < 0) i = 0;
        setPropertyDirect("global.worldLoadDelay", i);
    }

    public static boolean getShowGatesSavedMessage() {
        return config.getBoolean("global.showGatesSavedMessages", true);
    }

    public static void setShowGatesSavedMessage(boolean b) {
        setPropertyDirect("global.showGatesSavedMessages", b);
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

}

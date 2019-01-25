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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Chat {

    private static net.milkbowl.vault.chat.Chat vaultPlugin = null;

    private static Pattern colorPattern = Pattern.compile("%(\\w+)%");

    public static boolean vaultAvailable() {
        if (! Config.getUseVaultChat()) return false;
        Plugin p = Global.plugin.getServer().getPluginManager().getPlugin("Vault");
        if (p == null) {
            Utils.warning("Vault is not installed!");
            return false;
        }
        if (! p.isEnabled()) {
            Utils.warning("Vault is not enabled!");
            return false;
        }
        if (vaultPlugin != null) return true;
        RegisteredServiceProvider<net.milkbowl.vault.chat.Chat> rsp =
                Global.plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.chat.Chat.class);
        if (rsp == null) {
            Utils.warning("Vault didn't return a service provider!");
            return false;
        }
        vaultPlugin = rsp.getProvider();
        if (vaultPlugin == null) {
            Utils.warning("Vault didn't return a chat provider!");
            return false;
        }
        Utils.info("Initialized Vault for Chat");
        return true;
    }

    public static String colorize(String msg) {
        if (msg == null) return null;
        Matcher matcher = colorPattern.matcher(msg);
        StringBuffer b = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            try {
                ChatColor color = Utils.valueOf(ChatColor.class, name);
                matcher.appendReplacement(b, color.toString());
            } catch (IllegalArgumentException iae) {
                matcher.appendReplacement(b, matcher.group());
            }
        }
        matcher.appendTail(b);
        return b.toString();
    }

    public static String getPrefix(Player player) {
        if (vaultAvailable())
            return vaultPlugin.getPlayerPrefix(player);
        return null;
    }

    public static String getSuffix(Player player) {
        if (vaultAvailable())
            return vaultPlugin.getPlayerSuffix(player);
        return null;
    }

}

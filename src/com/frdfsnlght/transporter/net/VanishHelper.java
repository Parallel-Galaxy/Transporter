package com.frdfsnlght.transporter.net;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kitteh.vanish.event.VanishStatusChangeEvent;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;

import com.frdfsnlght.transporter.Global;

public class VanishHelper implements Listener {

    public static boolean isVanished(String playerName) {
        try {
            return VanishNoPacket.isVanished(playerName);
        } catch (VanishNotLoadedException e) {
            return false;
        }
    }
}

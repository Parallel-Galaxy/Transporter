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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import com.frdfsnlght.transporter.api.GateException;
import com.frdfsnlght.transporter.api.ReservationException;
import com.frdfsnlght.transporter.api.TransporterException;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class PlayerListenerImpl implements Listener {

    // Logic map for player interaction
    private static final Map<Integer,String> ACTIONS = new HashMap<Integer,String>();

    // Masks are strings composed of zeros and ones. Each character position
    // corresponds to a bit position (bit 0 is first position).
    // 0 1  : Is the gate currently open?
    // 1 2  : Does the player have trp.gate.open permission?
    // 2 4  : Does the player have trp.gate.close permission?
    // 3 8  : Does the player have trp.gate.changeLink permission?
    // 4 16 : Does the gate have a valid destination?
    // 5 32 : Is the gate on its last link?
    // 6 64 : Is the gate block a trigger?
    // 7 128: Is the gate block a switch?

    // Values are a comma separated list of actions to perform:
    // OPEN: open the gate
    // CLOSE: close the gate
    // CHANGELINK: change the gate's link

    static {
        // gate is closed
        addAction("01xxxx1x", "OPEN");
        addAction("0xx1xx01", "CHANGELINK");
        addAction("00x1xxx1", "CHANGELINK");
        addAction("01x10x11", "CHANGELINK,OPEN");

        // gate is open
        addAction("1x1xxx10", "CLOSE");
        addAction("1x10xx11", "CLOSE");
        addAction("1x11x111", "CLOSE,CHANGELINK");
        addAction("1x01xxx1", "CHANGELINK");
        addAction("1xx1xx01", "CHANGELINK");
        addAction("1xx1x011", "CHANGELINK");
    }

    private static void addAction(String mask, String action) {
        Set<Integer> masks = expandMask(mask);
        for (Integer m : masks) {
            ACTIONS.put(m, action);
        }
    }

    public static Set<Integer> expandMask(String mask) {
        return expandMask(0, 0, mask.charAt(0), mask.substring(1));
    }

    private static Set<Integer> expandMask(int bitPos, int prefix, char bit, String suffix) {
        switch (bit) {
            case '0':
            case '1':
                int bitValue = (bit == '0') ? 0 : (int)Math.pow(2, bitPos);
                if (suffix.isEmpty()) {
                    Set<Integer> masks = new HashSet<Integer>();
                    masks.add(prefix + bitValue);
                    return masks;
                }
                return expandMask(bitPos + 1, prefix + bitValue, suffix.charAt(0), suffix.substring(1));
            default:
                Set<Integer> masks = new HashSet<Integer>();
                masks.addAll(expandMask(bitPos, prefix, '0', suffix));
                masks.addAll(expandMask(bitPos, prefix, '1', suffix));
                return masks;
        }
    }

    public static Player testPlayer = null;

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) {
            Utils.debug("no block was interacted with?");
            return;
        }
        Location location = block.getLocation();
        Context ctx = new Context(event.getPlayer());

        GateImpl triggerGate = Gates.findGateForTrigger(location);
        GateImpl switchGate = Gates.findGateForSwitch(location);
        if (event.getPlayer() == testPlayer) {
            Utils.debug("-Interaction-----------------------------------------");
            Utils.debug("location: %s", Utils.blockCoords(location));
            Utils.debug("triggerGate: %s", (triggerGate == null) ? "none" : triggerGate.getFullName());
            Utils.debug("switchGate: %s", (switchGate == null) ? "none" : switchGate.getFullName());
            if ((triggerGate == null) && (switchGate == null)) {
                Utils.debug("triggerMap: %s", Gates.triggerMap.toString(testPlayer.getWorld()));
                Utils.debug("switchMap: %s", Gates.switchMap.toString(testPlayer.getWorld()));
            }
        }

        if ((triggerGate == null) && (switchGate == null)) return;
        if ((triggerGate != null) && (switchGate != null) && (triggerGate != switchGate)) switchGate = null;

        GateImpl testGate = (triggerGate == null) ? switchGate : triggerGate;
        Player player = event.getPlayer();
        Gates.setSelectedGate(player, testGate);

        int key =
                (testGate.isOpen() ? 1 : 0) +
                (Permissions.has(player, "trp.gate.open." + testGate.getFullName()) ? 2 : 0) +
                (Permissions.has(player, "trp.gate.close." + testGate.getFullName()) ? 4 : 0) +
                (Permissions.has(player, "trp.gate.changeLink." + testGate.getFullName()) ? 8 : 0) +
                (testGate.hasValidDestination() ? 16 : 0) +
                (testGate.isLastLink() ? 32 : 0) +
                ((triggerGate != null) ? 64 : 0) +
                ((switchGate != null) ? 128 : 0);
        String value = ACTIONS.get(key);
        Utils.debug("gate key/action is %s/%s", key, value);

        if (value == null) {
            ctx.send("not permitted");
            return;
        }
        String[] actions = value.split(",");

        for (String action : actions) {

            if (action.equals("OPEN")) {
                try {
                    testGate.open();
                    ctx.send("opened gate '%s'", testGate.getName());
                    Utils.debug("player '%s' open gate '%s'", player.getName(), testGate.getName());
                } catch (GateException ee) {
                    ctx.warnLog(ee.getMessage());
                }
            }

            if (action.equals("CLOSE")) {
                testGate.close();
                ctx.send("closed gate '%s'", testGate.getName());
                Utils.debug("player '%s' closed gate '%s'", player.getName(), testGate.getName());
            }

            if (action.equals("CHANGELINK")) {
                try {
                    testGate.nextLink();
                    Utils.debug("player '%s' changed link for gate '%s'", player.getName(), testGate.getName());
                } catch (TransporterException te) {
                    ctx.warnLog(te.getMessage());
                }
            }
        }

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if ((event.getFrom().getBlockX() == event.getTo().getBlockX()) &&
            (event.getFrom().getBlockY() == event.getTo().getBlockY()) &&
            (event.getFrom().getBlockZ() == event.getTo().getBlockZ())) return;

        Player player = event.getPlayer();
        GateImpl fromGate = Gates.findGateForPortal(event.getTo());
        if (fromGate == null) {
            ReservationImpl.removeGateLock(player);
            ReservationImpl.removeCountdown(player);
            return;
        }
        if (ReservationImpl.isGateLocked(player)) return;

        if (ReservationImpl.hasCountdown(player)) return;
        if (fromGate.getCountdown() > 0) {
            Countdown countdown = new Countdown(player, fromGate);
            countdown.start();
            return;
        }

        Context ctx = new Context(player);
        try {
            ReservationImpl r = new ReservationImpl(player, fromGate);
            r.depart();
            Location newLoc = r.getToLocation();
            if (newLoc != null) {
                event.setFrom(newLoc);
                event.setTo(newLoc);
            }
        } catch (ReservationException re) {
            ctx.warnLog(re.getMessage());
        }
    }

}

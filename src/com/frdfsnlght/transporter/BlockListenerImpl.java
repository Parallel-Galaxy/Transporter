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

import com.frdfsnlght.transporter.api.GateException;
import com.frdfsnlght.transporter.api.TransporterException;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public class BlockListenerImpl implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        GateImpl gate = Gates.findGateForPortal(event.getBlock().getLocation());
        if ((gate != null) && gate.isOpen())
            event.setBuildable(false);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        GateImpl gate = Gates.findGateForProtection(event.getBlock().getLocation());
        if (gate != null) {
            event.setCancelled(true);
            gate.onProtect(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        GateImpl gate = Gates.findGateForProtection(event.getBlock().getLocation());
        if (gate != null) {
            event.setCancelled(true);
            gate.onProtect(event.getBlock().getLocation());
            return;
        }

        gate = Gates.findGateForScreen(event.getBlock().getLocation());
        if (gate != null) {
            Context ctx = new Context(event.getPlayer());
            try {
                Permissions.require(ctx.getPlayer(), "trp.gate.destroy." + gate.getFullName());
                Gates.destroy(gate, false);
                ctx.sendLog("destroyed gate '%s'", gate.getName());
            } catch (PermissionsException pe) {
                ctx.warn(pe.getMessage());
                event.setCancelled(true);
                gate.onProtect(event.getBlock().getLocation());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (! Config.getAllowSignCreation()) return;

        Block block = event.getBlock();
        GateImpl gate = Gates.findGateForScreen(block.getLocation());
        if (gate != null) return;
        Context ctx = new Context(event.getPlayer());
        String gateName = null;
        String link = null;
        boolean reverse = false;
        for (String line : event.getLines()) {
            if ((line == null) || (line.trim().length() == 0)) continue;
            if (gateName == null)
                gateName = line;
            else if (link == null)
                link = line;
            else {
                if ("reverse".startsWith(line.toLowerCase())) {
                    reverse = true;
                    break;
                }
                link += "." + line;
            }
        }
        if (gateName == null) return;
        try {
            DesignMatch match = Designs.matchScreen(block.getLocation());
            if (match == null) return;

            Permissions.require(ctx.getPlayer(), "trp.create." + match.design.getName());

            gate = match.design.create(match, ctx.getPlayer(), gateName);
            Gates.add(gate, true);
            ctx.sendLog("created gate '%s'", gate.getName());
            Gates.setSelectedGate(ctx.getPlayer(), gate);

            if (link == null) return;
            ctx.getPlayer().performCommand("trp gate link add \"" + link + "\"" + (reverse ? " reverse" : ""));
        } catch (TransporterException te) {
            ctx.warn(te.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        // This prevents liquid portals from flowing out
        GateImpl gate = Gates.findGateForPortal(event.getBlock().getLocation());
        if (gate != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockRedstone(BlockRedstoneEvent event) {
        GateImpl triggerGate = Gates.findGateForTrigger(event.getBlock().getLocation());
        GateImpl switchGate = Gates.findGateForSwitch(event.getBlock().getLocation());
        BlockGateImpl gate;
        
        if (switchGate instanceof BlockGateImpl) {
            gate = (BlockGateImpl)switchGate;
            if (gate != null) {
                DesignBlockDetail block = gate.getGateBlock(event.getBlock().getLocation()).getDetail();
                Utils.debug("isOpen=%s", gate.isOpen());
                Utils.debug("triggerOpenMode=%s", block.getTriggerOpenMode());
                Utils.debug("triggerCloseMode=%s", block.getTriggerCloseMode());
                Utils.debug("newCurrent=%s", event.getNewCurrent());
                Utils.debug("oldCurrent=%s", event.getOldCurrent());
    
                if (gate.isClosed() && gate.hasValidDestination()) {
                    boolean openIt;
                    switch (block.getTriggerOpenMode()) {
                        case HIGH: openIt = (event.getNewCurrent() > 0) && (event.getOldCurrent() == 0); break;
                        case LOW: openIt = (event.getNewCurrent() == 0) && (event.getOldCurrent() > 0); break;
                        default: openIt = false;
                    }
                    if (openIt) {
                        try {
                            gate.open();
                            Utils.debug("gate '%s' opened via redstone", gate.getName());
                        } catch (GateException ee) {
                            Utils.warning(ee.getMessage());
                        }
                    }
                }
    
                else if (gate.isOpen()) {
                    boolean closeIt;
                    switch (block.getTriggerCloseMode()) {
                        case HIGH: closeIt = (event.getNewCurrent() > 0) && (event.getOldCurrent() == 0); break;
                        case LOW: closeIt = (event.getNewCurrent() == 0) && (event.getOldCurrent() > 0); break;
                        default: closeIt = false;
                    }
                    if (closeIt) {
                        gate.close();
                        Utils.debug("gate '%s' closed via redstone", gate.getName());
                    }
                }
                return;
            }
        }

        if (triggerGate instanceof BlockGateImpl) {
            gate = (BlockGateImpl)triggerGate;
            if (gate != null) {
                DesignBlockDetail block = gate.getGateBlock(event.getBlock().getLocation()).getDetail();
                boolean nextLink;
                switch (block.getSwitchMode()) {
                    case HIGH: nextLink = (event.getNewCurrent() > 0) && (event.getOldCurrent() == 0); break;
                    case LOW: nextLink = (event.getNewCurrent() == 0) && (event.getOldCurrent() > 0); break;
                    default: nextLink = false;
                }
                if (nextLink) {
                    try {
                        gate.nextLink();
                    } catch (TransporterException te) {
                        Utils.warning(te.getMessage());
                    }
                }
            }
        }
    }

}

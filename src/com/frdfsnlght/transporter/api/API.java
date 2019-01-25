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
package com.frdfsnlght.transporter.api;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;

import com.frdfsnlght.transporter.Config;
import com.frdfsnlght.transporter.GateImpl;
import com.frdfsnlght.transporter.Gates;
import com.frdfsnlght.transporter.LocalGateImpl;
import com.frdfsnlght.transporter.ReservationImpl;
import com.frdfsnlght.transporter.Worlds;

/**
 *  This class provides the top level API for the Transporter plugin.
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class API {

    /**
     * Returns a set of all gates on the local server.
     *
     * @return a set of {@link LocalGate} objects
     */
    public Set<LocalGate> getLocalGates() {
        return new HashSet<LocalGate>(Gates.getLocalGates());
    }

    /**
     * Returns a set of all worlds on the local server.
     *
     * @return a set of {@link LocalWorld} objects
     */
    public Set<LocalWorld> getLocalWorlds() {
        return new HashSet<LocalWorld>(Worlds.getAll());
    }

    /**
     * Saves all plugin configurations.
     * <p>
     * This method is the equivalent of calling <code>saveConfig</code>
     * and <code>saveGates</code>.
     */
    public void saveAll() {
        saveConfig();
        saveGates();
    }

    /**
     * Saves the main plugin configuration.
     */
    public void saveConfig() {
        Config.save(null);
    }

    /**
     * Saves all gate configurations.
     */
    public void saveGates() {
        Gates.save(null);
    }

    /**
     * Teleports the specified player as if they stepped into the specified gate.
     * <p>
     * This method will return before the teleportation is complete and may
     * not throw an exception even if the teleportation fails, under some
     * circumstances.
     *
     * @param player    the player to teleport
     * @param fromGate  the gate from which to teleport the player
     * @throws ReservationException if the teleportation cannot be completed
     */
    public void teleportPlayer(Player player, LocalGate fromGate) throws ReservationException {
        ReservationImpl res = new ReservationImpl(player, (LocalGateImpl)fromGate);
        res.depart();
    }

    /**
     * Teleports the specified player to the specified gate.
     * <p>
     * This method will return before the teleportation is complete and may
     * not throw an exception even if the teleportation fails, under some
     * circumstances.
     *
     * @param player    the player to teleport
     * @param toGate    the destination gate
     * @throws ReservationException if the teleportation cannot be completed
     */
    public void teleportPlayer(Player player, Gate toGate) throws ReservationException {
        ReservationImpl res = new ReservationImpl(player, (GateImpl)toGate);
        res.depart();
    }

}

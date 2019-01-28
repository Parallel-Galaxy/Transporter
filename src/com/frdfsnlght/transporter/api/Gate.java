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

import java.util.List;

import org.bukkit.block.BlockFace;

/**
 * This interface specifies methods common to all gates managed by the
 * Transporter plugin, whether local or remote.
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public interface Gate {

    /**
     * Returns the simple name of the gate.
     * <p>
     * A gate's simple name is the part of the name before any period ('.')
     * in the gate's fully qualified name (or local name or full name).
     *
     * @return the gate's simple name
     */
    public String getName();

    /**
     * Returns the full name of the gate.
     * <p>
     * A gate's full name is just like it's local name except that for local
     * gates a "server name" of "local" is used. For example:
     * <ul>
     *  <li>local.world.Gate (for a gate on the local server)</li>
     *  <li>Server1.world.Gate (for a gate on a remote server)</li>
     * </ul>
     *
     * @return the gate's full name
     */
    public String getFullName();

    /**
     * Saves the gate's configuration to disk.
     *
     * @param force     true to force the save even if no changes have been made
     */
    public void save(boolean force);

    /**
     * Rebuilds the block structure of the gate.
     */
    public void rebuild();

    /* Options */

    /**
     * Returns the name of the design this gate is based on.
     *
     * @return the name of the design
     */
    public String getDesignName();

    /**
     * Returns the value of the "restoreOnClose" option.
     *
     * @return  the option value
     */
    public boolean getRestoreOnClose();

    /**
     * Sets the "restoreOnClose" option.
     *
     * @param b     the option value
     */
    public void setRestoreOnClose(boolean b);

    /**
     * Returns the value of the "duration" option.
     *
     * @return      the option value
     */
    public int getDuration();

    /**
     * Sets the value of the "duration" option.
     *
     * @param i      the option value
     */
    public void setDuration(int i);

    /**
     * Returns the value of the "direction" option.
     *
     * @return      the option value
     */
    public BlockFace getDirection();

    /**
     * Sets the value of the "direction" option.
     *
     * @param i      the option value
     */
    public void setDirection(BlockFace dir);

    /**
     * Returns the list of links that are in the gate's links string list.
     *
     * @return a list of link strings
     */
    public List<String> getLinks();

    /**
     * Returns the value of the "linkLocal" option.
     *
     * @return      the option value
     */
    public boolean getLinkLocal();

    /**
     * Sets the value of the "linkLocal" option.
     *
     * @param b      the option value
     */
    public void setLinkLocal(boolean b);

    /**
     * Returns the value of the "linkWorld" option.
     *
     * @return      the option value
     */
    public boolean getLinkWorld();

    /**
     * Sets the value of the "linkWorld" option.
     *
     * @param b      the option value
     */
    public void setLinkWorld(boolean b);

    /**
     * Returns the value of the "linkNoneFormat" option.
     *
     * @return      the option value
     */
    public String getLinkNoneFormat();

    /**
     * Sets the value of the "linkNoneFormat" option.
     *
     * @param s      the option value
     */
    public void setLinkNoneFormat(String s);

    /**
     * Returns the value of the "linkUnselectedFormat" option.
     *
     * @return      the option value
     */
    public String getLinkUnselectedFormat();

    /**
     * Sets the value of the "linkUnselectedFormat" option.
     *
     * @param s      the option value
     */
    public void setLinkUnselectedFormat(String s);

    /**
     * Returns the value of the "linkOfflineFormat" option.
     *
     * @return      the option value
     */
    public String getLinkOfflineFormat();

    /**
     * Sets the value of the "linkOfflineFormat" option.
     *
     * @param s      the option value
     */
    public void setLinkOfflineFormat(String s);

    /**
     * Returns the value of the "linkLocalFormat" option.
     *
     * @return      the option value
     */
    public String getLinkLocalFormat();

    /**
     * Sets the value of the "linkLocalFormat" option.
     *
     * @param s      the option value
     */
    public void setLinkLocalFormat(String s);

    /**
     * Returns the value of the "linkWorldFormat" option.
     *
     * @return      the option value
     */
    public String getLinkWorldFormat();

    /**
     * Sets the value of the "linkWorldFormat" option.
     *
     * @param s      the option value
     */
    public void setLinkWorldFormat(String s);

    /**
     * Returns the value of the "protect" option.
     *
     * @return      the option value
     */
    public boolean getProtect();

    /**
     * Sets the value of the "protect" option.
     *
     * @param b      the option value
     */
    public void setProtect(boolean b);

    /**
     * Returns the value of the "teleportFormat" option.
     *
     * @return      the option value
     */
    public String getTeleportFormat();

    /**
     * Sets the value of the "teleportFormat" option.
     *
     * @param s      the option value
     */
    public void setTeleportFormat(String s);

    /**
     * Returns the value of the "noLinksFormat" option.
     *
     * @return      the option value
     */
    public String getNoLinksFormat();

    /**
     * Sets the value of the "noLinksFormat" option.
     *
     * @param s      the option value
     */
    public void setNoLinksFormat(String s);

    /**
     * Returns the value of the "noLinkSelectedFormat" option.
     *
     * @return      the option value
     */
    public String getNoLinkSelectedFormat();

    /**
     * Sets the value of the "noLinkSelectedFormat" option.
     *
     * @param s      the option value
     */
    public void setNoLinkSelectedFormat(String s);

    /**
     * Returns the value of the "invalidLinkFormat" option.
     *
     * @return      the option value
     */
    public String getInvalidLinkFormat();

    /**
     * Sets the value of the "invalidLinkFormat" option.
     *
     * @param s      the option value
     */
    public void setInvalidLinkFormat(String s);

    /**
     * Returns the value of the "unknownLinkFormat" option.
     *
     * @return      the option value
     */
    public String getUnknownLinkFormat();

    /**
     * Sets the value of the "unknownLinkFormat" option.
     *
     * @param s      the option value
     */
    public void setUnknownLinkFormat(String s);

    /**
     * Returns the value of the "countdown" option.
     *
     * @return      the option value
     */
    public int getCountdown();

    /**
     * Sets the value of the "countdown" option.
     *
     * @param i      the option value
     */
    public void setCountdown(int i);

    /**
     * Returns the value of the "countdownInterval" option.
     *
     * @return      the option value
     */
    public int getCountdownInterval();

    /**
     * Sets the value of the "countdownInterval" option.
     *
     * @param i      the option value
     */
    public void setCountdownInterval(int i);

    /**
     * Returns the value of the "countdownFormat" option.
     *
     * @return      the option value
     */
    public String getCountdownFormat();

    /**
     * Sets the value of the "countdownFormat" option.
     *
     * @param s      the option value
     */
    public void setCountdownFormat(String s);

    /**
     * Returns the value of the "countdownIntervalFormat" option.
     *
     * @return      the option value
     */
    public String getCountdownIntervalFormat();

    /**
     * Sets the value of the "countdownIntervalFormat" option.
     *
     * @param s      the option value
     */
    public void setCountdownIntervalFormat(String s);

    /**
     * Returns the value of the "countdownCancelFormat" option.
     *
     * @return      the option value
     */
    public String getCountdownCancelFormat();

    /**
     * Sets the value of the "countdownCancelFormat" option.
     *
     * @param s      the option value
     */
    public void setCountdownCancelFormat(String s);

    /**
     * Returns the value of the "hidden" option.
     *
     * @return      the option value
     */
    public boolean getHidden();

    /**
     * Sets the value of the "hidden" option.
     *
     * @param b     the option value
     */
    public void setHidden(boolean b);

    /* End Options */

}

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
import com.frdfsnlght.transporter.api.Gate;
import com.frdfsnlght.transporter.api.GateException;
import com.frdfsnlght.transporter.api.TransporterException;
import com.frdfsnlght.transporter.api.event.GateClosedEvent;
import com.frdfsnlght.transporter.api.event.GateOpenedEvent;
import com.frdfsnlght.transporter.command.CommandException;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public abstract class GateImpl implements Gate, OptionsListener {

    public static GateImpl load(World world, File file) throws GateException {
        if (! file.exists())
            throw new GateException("%s not found", file.getAbsolutePath());
        if (! file.isFile())
            throw new GateException("%s is not a file", file.getAbsolutePath());
        if (! file.canRead())
            throw new GateException("unable to read %s", file.getAbsoluteFile());
        TypeMap conf = new TypeMap(file);
        conf.load();

        return new BlockGateImpl(world, conf);
    }

    protected static final Set<String> BASEOPTIONS = new HashSet<String>();

    static {
        BASEOPTIONS.add("duration");
        BASEOPTIONS.add("direction");
        BASEOPTIONS.add("linkLocal");
        BASEOPTIONS.add("linkWorld");
        BASEOPTIONS.add("linkNoneFormat");
        BASEOPTIONS.add("linkUnselectedFormat");
        BASEOPTIONS.add("linkOfflineFormat");
        BASEOPTIONS.add("linkLocalFormat");
        BASEOPTIONS.add("linkWorldFormat");
        BASEOPTIONS.add("multiLink");
        BASEOPTIONS.add("protect");
        BASEOPTIONS.add("requirePin");
        BASEOPTIONS.add("requireValidPin");
        BASEOPTIONS.add("requireLevel");
        BASEOPTIONS.add("invalidPinDamage");
        BASEOPTIONS.add("sendChat");
        BASEOPTIONS.add("sendChatFilter");
        BASEOPTIONS.add("sendChatFormatFilter");
        BASEOPTIONS.add("sendChatDistance");
        BASEOPTIONS.add("receiveChat");
        BASEOPTIONS.add("receiveChatFilter");
        BASEOPTIONS.add("receiveChatDistance");
        BASEOPTIONS.add("requireAllowedItems");
        BASEOPTIONS.add("receiveInventory");
        BASEOPTIONS.add("deleteInventory");
        BASEOPTIONS.add("receiveGameMode");
        BASEOPTIONS.add("allowGameModes");
        BASEOPTIONS.add("gameMode");
        BASEOPTIONS.add("receiveXP");
        BASEOPTIONS.add("receivePotions");
        BASEOPTIONS.add("requireAllowedPotions");
        BASEOPTIONS.add("receiveStats");
        BASEOPTIONS.add("randomNextLink");
        BASEOPTIONS.add("sendNextLink");
        BASEOPTIONS.add("teleportFormat");
        BASEOPTIONS.add("noLinksFormat");
        BASEOPTIONS.add("noLinkSelectedFormat");
        BASEOPTIONS.add("invalidLinkFormat");
        BASEOPTIONS.add("unknownLinkFormat");
        BASEOPTIONS.add("countdown");
        BASEOPTIONS.add("countdownInterval");
        BASEOPTIONS.add("countdownFormat");
        BASEOPTIONS.add("countdownIntervalFormat");
        BASEOPTIONS.add("countdownCancelFormat");
        BASEOPTIONS.add("markerFormat");
        BASEOPTIONS.add("hidden");
        BASEOPTIONS.add("linkAddDistance");
    }

    protected File file;
    protected World world;
    protected Vector center;
    protected BlockFace direction;

    protected String name;
    protected int duration;
    protected boolean linkLocal;
    protected boolean linkWorld;
    protected String linkNoneFormat;
    protected String linkUnselectedFormat;
    protected String linkOfflineFormat;
    protected String linkLocalFormat;
    protected String linkWorldFormat;
    protected boolean multiLink;
    protected boolean requirePin;
    protected boolean requireValidPin;
    protected int requireLevel;
    protected int invalidPinDamage;
    protected boolean protect;
    protected boolean sendChat;
    protected String sendChatFilter;
    protected String sendChatFormatFilter;
    protected int sendChatDistance;
    protected boolean receiveChat;
    protected String receiveChatFilter;
    protected int receiveChatDistance;
    protected boolean requireAllowedItems;
    protected boolean receiveInventory;
    protected boolean deleteInventory;
    protected boolean receiveGameMode;
    protected String allowGameModes;
    protected GameMode gameMode;
    protected boolean receiveXP;
    protected boolean receivePotions;
    protected boolean requireAllowedPotions;
    protected boolean receiveStats;
    protected boolean randomNextLink;
    protected boolean sendNextLink;
    protected String teleportFormat;
    protected String noLinksFormat;
    protected String noLinkSelectedFormat;
    protected String invalidLinkFormat;
    protected String unknownLinkFormat;
    protected String markerFormat;
    protected boolean hidden;
    protected int linkAddDistance;
    protected int countdown;
    protected int countdownInterval;
    protected String countdownFormat;
    protected String countdownIntervalFormat;
    protected String countdownCancelFormat;

    protected final List<String> links = new ArrayList<String>();
    protected final Set<String> pins = new HashSet<String>();
    protected final Set<String> bannedItems = new HashSet<String>();
    protected final Set<String> allowedItems = new HashSet<String>();
    protected final Map<String,String> replaceItems = new HashMap<String,String>();
    protected final Set<String> bannedPotions = new HashSet<String>();
    protected final Set<String> allowedPotions = new HashSet<String>();
    protected final Map<String,String> replacePotions = new HashMap<String,String>();

    protected Set<String> incoming = new HashSet<String>();
    protected String outgoing = null;

    protected boolean dirty = false;
    protected boolean portalOpen = false;
    protected long portalOpenTime = 0;
    protected Options options = new Options(this, BASEOPTIONS, "trp.gate", this);

    protected GateImpl(World world, TypeMap conf) throws GateException {
        this.file = conf.getFile();
        this.world = world;
        name = conf.getString("name");
        try {
            direction = Utils.valueOf(BlockFace.class, conf.getString("direction", "NORTH"));
        } catch (IllegalArgumentException iae) {
            throw new GateException(iae.getMessage() + " direction");
        }

        duration = conf.getInt("duration", -1);
        linkLocal = conf.getBoolean("linkLocal", true);
        linkWorld = conf.getBoolean("linkWorld", true);

        linkNoneFormat = conf.getString("linkNoneFormat", "%fromGate%\\n\\n<none>");
        linkUnselectedFormat = conf.getString("linkUnselectedFormat", "%fromGate%\\n\\n<unselected>");
        linkOfflineFormat = conf.getString("linkOfflineFormat", "%fromGate%\\n\\n<offline>");
        linkLocalFormat = conf.getString("linkLocalFormat", "%fromGate%\\n%toGate%");
        linkWorldFormat = conf.getString("linkWorldFormat", "%fromGate%\\n%toWorld%\\n%toGate%");

        multiLink = conf.getBoolean("multiLink", true);
        links.addAll(conf.getStringList("links", new ArrayList<String>()));
        pins.addAll(conf.getStringList("pins", new ArrayList<String>()));
        portalOpen = conf.getBoolean("portalOpen", false);

        String gameModeStr = conf.getString("gameMode", null);
        if (gameModeStr == null)
            gameMode = null;
        else {
            try {
                gameMode = Utils.valueOf(GameMode.class, gameModeStr);
            } catch (IllegalArgumentException iae) {
                throw new GateException(iae.getMessage() + " game mode '%s'", gameModeStr);
            }
        }

        List<String> items = conf.getStringList("bannedItems", new ArrayList<String>());
        for (String item : items) {
            String i = Inventory.normalizeItem(item);
            if (i == null)
                throw new GateException("invalid banned item '%s'", item);
            bannedItems.add(i);
        }

        items = conf.getStringList("allowedItems", new ArrayList<String>());
        for (String item : items) {
            String i = Inventory.normalizeItem(item);
            if (i == null)
                throw new GateException("invalid allowed item '%s'", item);
            allowedItems.add(i);
        }

        items = conf.getKeys("replaceItems");
        if (items != null) {
            for (String oldItem : items) {
                String oi = Inventory.normalizeItem(oldItem);
                if (oi == null)
                    throw new GateException("invalid replace item '%s'", oldItem);
                String newItem = conf.getString("replaceItems." + oldItem);
                String ni = Inventory.normalizeItem(newItem);
                if (ni == null)
                    throw new GateException("invalid replace item '%s'", newItem);
                replaceItems.put(oi, ni);
            }
        }

        List<String> potions = conf.getStringList("bannedPotions", new ArrayList<String>());
        for (String potion : potions) {
            String p = PotionEffects.normalizePotion(potion);
            if (p == null)
                throw new GateException("invalid banned potion effect '%s'", potion);
            bannedPotions.add(p);
        }

        potions = conf.getStringList("allowedPotions", new ArrayList<String>());
        for (String potion : potions) {
            String p = PotionEffects.normalizePotion(potion);
            if (p == null)
                throw new GateException("invalid allowed potion effect '%s'", potion);
            allowedPotions.add(p);
        }

        potions = conf.getKeys("replacePotions");
        if (potions != null) {
            for (String oldPotion : potions) {
                String op = PotionEffects.normalizePotion(oldPotion);
                if (op == null)
                    throw new GateException("invalid replace potion effect '%s'", oldPotion);
                String newPotion = conf.getString("replacePotions." + oldPotion);
                String np = PotionEffects.normalizePotion(newPotion);
                if (np == null)
                    throw new GateException("invalid replace potion effect '%s'", newPotion);
                replacePotions.put(op, np);
            }
        }

        requirePin = conf.getBoolean("requirePin", false);
        requireValidPin = conf.getBoolean("requireValidPin", true);
        requireLevel = conf.getInt("requireLevel", 0);
        invalidPinDamage = conf.getInt("invalidPinDamage", 0);
        protect = conf.getBoolean("protect", false);
        sendChat = conf.getBoolean("sendChat", false);
        sendChatFilter = conf.getString("sendChatFilter");
        sendChatFormatFilter = conf.getString("sendChatFormatFilter");
        sendChatDistance = conf.getInt("sendChatDistance", 1000);
        receiveChat = conf.getBoolean("receiveChat", false);
        receiveChatFilter = conf.getString("receiveChatFilter");
        receiveChatDistance = conf.getInt("receiveChatDistance", 1000);
        requireAllowedItems = conf.getBoolean("requireAllowedItems", true);
        receiveInventory = conf.getBoolean("receiveInventory", true);
        deleteInventory = conf.getBoolean("deleteInventory", false);
        receiveGameMode = conf.getBoolean("receiveGameMode", false);
        allowGameModes = conf.getString("allowGameModes", "*");
        receiveXP = conf.getBoolean("receiveXP", false);
        receivePotions = conf.getBoolean("receivePotions", false);
        requireAllowedPotions = conf.getBoolean("requireAllowedPotions", true);
        receiveStats = conf.getBoolean("receiveStats", true);
        randomNextLink = conf.getBoolean("randomNextLink", false);
        sendNextLink = conf.getBoolean("sendNextLink", false);
        teleportFormat = conf.getString("teleportFormat", "%GOLD%teleported to '%toGateCtx%'");
        noLinksFormat = conf.getString("noLinksFormat", "this gate has no links");
        noLinkSelectedFormat = conf.getString("noLinkSelectedFormat", "no link is selected");
        invalidLinkFormat = conf.getString("invalidLinkFormat", "invalid link selected");
        unknownLinkFormat = conf.getString("unknownLinkFormat", "unknown or offline destination gate");
        markerFormat = conf.getString("markerFormat", "%name%");
        hidden = conf.getBoolean("hidden", false);
        linkAddDistance = conf.getInt("linkAddDistance", -1);
        countdown = conf.getInt("countdown", -1);
        countdownInterval = conf.getInt("countdownInterval", 1000);
        countdownFormat = conf.getString("countdownFormat", "%RED%Teleport countdown started...");
        countdownIntervalFormat = conf.getString("countdownIntervalFormat", "%RED%Teleport in %time% seconds...");
        countdownCancelFormat = conf.getString("countdownCancelFormat", "Teleport canceled");

        incoming.addAll(conf.getStringList("incoming", new ArrayList<String>()));
        outgoing = conf.getString("outgoing");
    }

    protected GateImpl(World world, String gateName, Player creator, BlockFace direction) throws GateException {
        this.world = world;
        name = gateName;
        this.direction = direction;
        setDefaults();
    }

    private void setDefaults() {
        setDuration(-1);
        setLinkLocal(true);
        setLinkWorld(true);
        setLinkNoneFormat(null);
        setLinkUnselectedFormat(null);
        setLinkOfflineFormat(null);
        setLinkLocalFormat(null);
        setLinkWorldFormat(null);
        setMultiLink(true);
        setRequirePin(false);
        setRequireValidPin(true);
        setRequireLevel(0);
        setInvalidPinDamage(0);
        setProtect(false);
        setSendChat(false);
        setSendChatFilter(null);
        setSendChatFormatFilter(null);
        setSendChatDistance(1000);
        setReceiveChat(false);
        setReceiveChatFilter(null);
        setReceiveChatDistance(1000);
        setRequireAllowedItems(true);
        setReceiveInventory(true);
        setDeleteInventory(false);
        setReceiveGameMode(false);
        setAllowGameModes("*");
        setGameMode(null);
        setReceiveXP(false);
        setReceivePotions(false);
        setRequireAllowedPotions(true);
        setReceiveStats(true);
        setRandomNextLink(false);
        setSendNextLink(false);
        setTeleportFormat(null);
        setNoLinksFormat(null);
        setNoLinkSelectedFormat(null);
        setInvalidLinkFormat(null);
        setUnknownLinkFormat(null);
        setMarkerFormat(null);
        setHidden(false);
        setLinkAddDistance(-1);
        setCountdown(-1);
        setCountdownInterval(1000);
        setCountdownFormat(null);
        setCountdownIntervalFormat(null);
        setCountdownCancelFormat(null);
    }

    public abstract Location getSpawnLocation(Location fromLoc, BlockFace fromDirection);

    public abstract void onSend(Entity entity);
    public abstract void onReceive(Entity entity);
    public abstract void onProtect(Location loc);

    protected abstract void onValidate() throws GateException;
    protected abstract void onDestroy(boolean unbuild);
    protected abstract void onAdd();
    protected abstract void onRemove();
    protected abstract void onOpen();
    protected abstract void onClose();
    protected abstract void onNameChanged();
    protected abstract void onDestinationChanged();
    protected abstract void onSave(TypeMap conf);

    protected abstract void calculateCenter();

    // Gate interface

    public String getFullName() {
        return world.getName() + "." + getName();
    }

    public String getName(Context ctx) {
        if ((ctx != null) && ctx.isPlayer()) return getName();
        return getFullName();
    }

    public static boolean isValidName(String name) {
        if ((name.length() == 0) || (name.length() > 15)) return false;
        return ! (name.contains(".") || name.contains("*"));
    }

    
    protected void attach(GateImpl origin) {
        if (origin != null) {
            String originName = origin.getFullName();
            if (incoming.contains(originName)) return;
            incoming.add(originName);
            dirty = true;
        }

        // 2 new
        portalOpen = true;
        portalOpenTime = System.currentTimeMillis();

        onOpen();

        // try to attach to our destination
        if ((outgoing == null) || (! hasLink(outgoing))) {
            if (! isLinked())
                outgoing = null;
            else
                outgoing = getLinks().get(0);
            onDestinationChanged();
        }
        if (outgoing != null) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.attach(this);
        }

        // new
        if (duration > 0) {
            final GateImpl myself = this;
            Utils.fireDelayed(new Runnable() {
                
                public void run() {
                    myself.closeIfAllowed();
                }
            }, duration + 100);
        }
    }

    
    protected void detach(GateImpl origin) {
        String originName = origin.getFullName();
        if (! incoming.contains(originName)) return;

        incoming.remove(originName);
        dirty = true;
        closeIfAllowed();
    }

    // End interfaces and implementations

    public void onRenameComplete() {
        file.delete();
        generateFile();
        save(true);
        onNameChanged();
    }

    public void onGateAdded(GateImpl gate) {
        if (gate == this)
            onAdd();
        else {
            if ((outgoing != null) && outgoing.equals(gate.getFullName()))
                onDestinationChanged();
        }
    }

    public void onGateRemoved(GateImpl gate) {
        if (gate == this)
            onRemove();
        else {
            String gateName = gate.getFullName();
            if (gateName.equals(outgoing)) {
                //outgoing = null;
                //dirty = true;
                onDestinationChanged();
            }
            closeIfAllowed();
        }
    }

    public void onGateDestroyed(GateImpl gate) {
        if (gate == this) return;
        String gateName = gate.getFullName();
        if (removeLink(gateName))
            dirty = true;
        if (gateName.equals(outgoing)) {
            outgoing = null;
            dirty = true;
            onDestinationChanged();
        }
        if (incoming.contains(gateName)) {
            incoming.remove(gateName);
            dirty = true;
        }
        closeIfAllowed();
    }

    public void onGateRenamed(GateImpl gate, String oldFullName) {
        if (gate == this) return;
        String newName = gate.getFullName();
        if (links.contains(oldFullName)) {
            links.set(links.indexOf(oldFullName), newName);
            dirty = true;
        }
        if (oldFullName.equals(outgoing)) {
            outgoing = newName;
            dirty = true;
            onDestinationChanged();
        }
        if (incoming.contains(oldFullName)) {
            incoming.remove(oldFullName);
            incoming.add(newName);
            dirty = true;
        }
    }

    public void destroy(boolean unbuild) {
        close();
        if (! file.delete())
            Utils.warning("unable to delete gate file %s", file.getAbsolutePath());
        else
            Utils.info("deleted gate file %s", file.getAbsolutePath());
        file = null;
        onDestroy(unbuild);
    }

    public boolean isOpen() {
        return portalOpen;
    }

    public boolean isClosed() {
        return ! portalOpen;
    }

    public boolean isSameWorld(World world) {
        return this.world == world;
    }

    public World getWorld() {
        return world;
    }

    public void open() throws GateException {
        if (portalOpen) return;

        // try to get our destination
        if ((outgoing == null) || (! hasLink(outgoing))) {
            if (! isLinked())
                throw new GateException("this gate has no links");
            outgoing = getLinks().get(0);
            dirty = true;
        }
        GateImpl gate = Gates.get(outgoing);
        if (gate == null)
            throw new GateException("unknown or offline gate '%s'", outgoing);

        portalOpen = true;
        portalOpenTime = System.currentTimeMillis();
        gate.attach(this);
        onOpen();
        onDestinationChanged();

        GateOpenedEvent event = new GateOpenedEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        if (duration > 0) {
            final GateImpl myself = this;
            Utils.fireDelayed(new Runnable() {
                
                public void run() {
                    myself.closeIfAllowed();
                }
            }, duration + 100);
        }
    }

    public void close() {
        if (! portalOpen) return;
        portalOpen = false;

        ReservationImpl.removeCountdowns(this);
        incoming.clear();
        onClose();
        onDestinationChanged();

        GateClosedEvent event = new GateClosedEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        // try to detach from our destination
        if (outgoing != null) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.detach(this);
        }
    }

    
    public void save(boolean force) {
        if ((! dirty) && (! force)) return;
        if (file == null) return;
        dirty = false;

        TypeMap conf = new TypeMap(file);
        conf.set("name", name);
        conf.set("direction", direction.toString());
        conf.set("duration", duration);
        conf.set("linkLocal", linkLocal);
        conf.set("linkWorld", linkWorld);

        conf.set("linkNoneFormat", linkNoneFormat);
        conf.set("linkUnselectedFormat", linkUnselectedFormat);
        conf.set("linkOfflineFormat", linkOfflineFormat);
        conf.set("linkLocalFormat", linkLocalFormat);
        conf.set("linkWorldFormat", linkWorldFormat);

        conf.set("multiLink", multiLink);
        conf.set("links", links);
        conf.set("pins", new ArrayList<String>(pins));
        conf.set("bannedItems", new ArrayList<String>(bannedItems));
        conf.set("allowedItems", new ArrayList<String>(allowedItems));
        conf.set("replaceItems", replaceItems);
        conf.set("requirePin", requirePin);
        conf.set("requireValidPin", requireValidPin);
        conf.set("requireLevel", requireLevel);
        conf.set("invalidPinDamage", invalidPinDamage);
        conf.set("protect", protect);
        conf.set("sendChat", sendChat);
        conf.set("sendChatFilter", sendChatFilter);
        conf.set("sendChatFormatFilter", sendChatFormatFilter);
        conf.set("sendChatDistance", sendChatDistance);
        conf.set("receiveChat", receiveChat);
        conf.set("receiveChatFilter", receiveChatFilter);
        conf.set("receiveChatDistance", receiveChatDistance);
        conf.set("requireAllowedItems", requireAllowedItems);
        conf.set("receiveInventory", receiveInventory);
        conf.set("deleteInventory", deleteInventory);
        conf.set("receiveGameMode", receiveGameMode);
        conf.set("allowGameModes", allowGameModes);
        conf.set("gameMode", gameMode);
        conf.set("receiveXP", receiveXP);
        conf.set("receivePotions", receivePotions);
        conf.set("requireAllowedPotions", requireAllowedPotions);
        conf.set("receiveStats", receiveStats);
        conf.set("bannedPotions", new ArrayList<String>(bannedPotions));
        conf.set("allowedPotions", new ArrayList<String>(allowedPotions));
        conf.set("replacePotions", replacePotions);
        conf.set("randomNextLink", randomNextLink);
        conf.set("sendNextLink", sendNextLink);
        conf.set("teleportFormat", teleportFormat);
        conf.set("noLinksFormat", noLinksFormat);
        conf.set("noLinkSelectedFormat", noLinkSelectedFormat);
        conf.set("invalidLinkFormat", invalidLinkFormat);
        conf.set("unknownLinkFormat", unknownLinkFormat);
        conf.set("markerFormat", markerFormat);
        conf.set("hidden", hidden);
        conf.set("linkAddDistance", linkAddDistance);
        conf.set("countdown", countdown);
        conf.set("countdownInterval", countdownInterval);
        conf.set("countdownFormat", countdownFormat);
        conf.set("countdownIntervalFormat", countdownIntervalFormat);
        conf.set("countdownCancelFormat", countdownCancelFormat);

        conf.set("portalOpen", portalOpen);

        if (! incoming.isEmpty()) conf.set("incoming", new ArrayList<String>(incoming));
        if (outgoing != null) conf.set("outgoing", outgoing);

        onSave(conf);

        File parent = file.getParentFile();
        if (! parent.exists())
            parent.mkdirs();
        conf.save();
    }

    protected void validate() throws GateException {
        if (name == null)
            throw new GateException("name is required");
        if (! isValidName(name))
            throw new GateException("name is not valid");
        onValidate();
    }

    public Vector getCenter() {
        return center;
    }

    /* Begin options */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public int getDuration() {
        return duration;
    }

    
    public void setDuration(int i) {
        if (i <= 0) i = -1;
        duration = i;
        dirty = true;
    }

    
    public BlockFace getDirection() {
        return direction;
    }

    
    public void setDirection(BlockFace dir) {
        direction = dir;
        dirty = true;
    }

    
    public boolean getLinkLocal() {
        return linkLocal;
    }

    
    public void setLinkLocal(boolean b) {
        linkLocal = b;
        dirty = true;
    }

    
    public boolean getLinkWorld() {
        return linkWorld;
    }

    
    public void setLinkWorld(boolean b) {
        linkWorld = b;
        dirty = true;
    }

    
    public String getLinkNoneFormat() {
        return linkNoneFormat;
    }

    
    public void setLinkNoneFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<none>";
        linkNoneFormat = s;
        dirty = true;
    }

    
    public String getLinkUnselectedFormat() {
        return linkUnselectedFormat;
    }

    
    public void setLinkUnselectedFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<unselected>";
        linkUnselectedFormat = s;
        dirty = true;
    }

    
    public String getLinkOfflineFormat() {
        return linkOfflineFormat;
    }

    
    public void setLinkOfflineFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n\\n<offline>";
        linkOfflineFormat = s;
        dirty = true;
    }

    
    public String getLinkLocalFormat() {
        return linkLocalFormat;
    }

    
    public void setLinkLocalFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n%toGate%";
        linkLocalFormat = s;
        dirty = true;
    }

    
    public String getLinkWorldFormat() {
        return linkWorldFormat;
    }

    
    public void setLinkWorldFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%fromGate%\\n%toWorld%\\n%toGate%";
        linkWorldFormat = s;
        dirty = true;
    }

    
    public boolean getMultiLink() {
        return multiLink;
    }

    
    public void setMultiLink(boolean b) {
        multiLink = b;
        dirty = true;
    }

    
    public boolean getProtect() {
        return protect;
    }

    
    public void setProtect(boolean b) {
        protect = b;
        dirty = true;
    }

    
    public boolean getRequirePin() {
        return requirePin;
    }

    
    public void setRequirePin(boolean b) {
        requirePin = b;
        dirty = true;
    }

    
    public boolean getRequireValidPin() {
        return requireValidPin;
    }

    
    public void setRequireValidPin(boolean b) {
        requireValidPin = b;
        dirty = true;
    }

    
    public int getRequireLevel() {
        return requireLevel;
    }

    
    public void setRequireLevel(int i) {
        requireLevel = i;
        dirty = true;
    }

    
    public int getInvalidPinDamage() {
        return invalidPinDamage;
    }

    
    public void setInvalidPinDamage(int i) {
        if (i < 0)
            throw new IllegalArgumentException("invalidPinDamage must be at least 0");
        invalidPinDamage = i;
        dirty = true;
    }

    
    public boolean getSendChat() {
        return sendChat;
    }

    
    public void setSendChat(boolean b) {
        sendChat = b;
        dirty = true;
    }

    
    public String getSendChatFilter() {
        return sendChatFilter;
    }

    
    public void setSendChatFilter(String s) {
        if (s != null) {
            if (s.isEmpty() || s.equals("-")) s = null;
            else
                try {
                    Pattern.compile(s);
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException("invalid regular expression");
                }
        }
        sendChatFilter = s;
    }

    
    public String getSendChatFormatFilter() {
        return sendChatFormatFilter;
    }

    
    public void setSendChatFormatFilter(String s) {
        if (s != null) {
            if (s.isEmpty() || s.equals("-")) s = null;
            else
                try {
                    Pattern.compile(s);
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException("invalid regular expression");
                }
        }
        sendChatFormatFilter = s;
    }

    
    public int getSendChatDistance() {
        return sendChatDistance;
    }

    
    public void setSendChatDistance(int i) {
        sendChatDistance = i;
        dirty = true;
    }

    
    public boolean getReceiveChat() {
        return receiveChat;
    }

    
    public void setReceiveChat(boolean b) {
        receiveChat = b;
        dirty = true;
    }

    
    public String getReceiveChatFilter() {
        return receiveChatFilter;
    }

    
    public void setReceiveChatFilter(String s) {
        if (s != null) {
            if (s.isEmpty() || s.equals("-")) s = null;
            else
                try {
                    Pattern.compile(s);
                } catch (PatternSyntaxException e) {
                    throw new IllegalArgumentException("invalid regular expression");
                }
        }
        receiveChatFilter = s;
    }

    
    public int getReceiveChatDistance() {
        return receiveChatDistance;
    }

    
    public void setReceiveChatDistance(int i) {
        receiveChatDistance = i;
        dirty = true;
    }

    
    public boolean getRequireAllowedItems() {
        return requireAllowedItems;
    }

    
    public void setRequireAllowedItems(boolean b) {
        requireAllowedItems = b;
        dirty = true;
    }

    
    public boolean getReceiveInventory() {
        return receiveInventory;
    }

    
    public void setReceiveInventory(boolean b) {
        receiveInventory = b;
        dirty = true;
    }

    
    public boolean getDeleteInventory() {
        return deleteInventory;
    }

    
    public void setDeleteInventory(boolean b) {
        deleteInventory = b;
        dirty = true;
    }

    
    public boolean getReceiveGameMode() {
        return receiveGameMode;
    }

    
    public void setReceiveGameMode(boolean b) {
        receiveGameMode = b;
        dirty = true;
    }

    
    public String getAllowGameModes() {
        return allowGameModes;
    }

    
    public void setAllowGameModes(String s) {
        if (s != null) {
            if (s.equals("*")) s = null;
        }
        if (s == null) s = "*";
        String[] parts = s.split(",");
        String modes = "";
        for (String part : parts) {
            if (part.equals("*")) {
                modes = "*,";
                break;
            }
            try {
                GameMode mode = Utils.valueOf(GameMode.class, part);
                modes += mode.toString() + ",";
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e.getMessage() + " game mode '" + part + "'");
            }
        }
        allowGameModes = modes.substring(0, modes.length() - 1);
        dirty = true;
    }

    
    public GameMode getGameMode() {
        return gameMode;
    }

    
    public void setGameMode(GameMode m) {
        gameMode = m;
        dirty = true;
    }

    
    public boolean getReceiveXP() {
        return receiveXP;
    }

    
    public void setReceiveXP(boolean b) {
        receiveXP = b;
        dirty = true;
    }

    
    public boolean getReceivePotions() {
        return receivePotions;
    }

    
    public void setReceivePotions(boolean b) {
        receivePotions = b;
        dirty = true;
    }

    
    public boolean getRequireAllowedPotions() {
        return requireAllowedPotions;
    }

    
    public void setRequireAllowedPotions(boolean b) {
        requireAllowedPotions = b;
        dirty = true;
    }

    
    public boolean getReceiveStats() {
        return receiveStats;
    }

    
    public void setReceiveStats(boolean b) {
        receiveStats = b;
        dirty = true;
    }

    
    public boolean getRandomNextLink() {
        return randomNextLink;
    }

    
    public void setRandomNextLink(boolean b) {
        randomNextLink = b;
        dirty = true;
    }

    
    public boolean getSendNextLink() {
        return sendNextLink;
    }

    
    public void setSendNextLink(boolean b) {
        sendNextLink = b;
        dirty = true;
    }

    
    public String getTeleportFormat() {
        return teleportFormat;
    }

    
    public void setTeleportFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%GOLD%teleported to '%toGateCtx%'";
        teleportFormat = s;
        dirty = true;
    }

    
    public String getNoLinksFormat() {
        return noLinksFormat;
    }

    
    public void setNoLinksFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "this gate has no links";
        noLinksFormat = s;
        dirty = true;
    }

    
    public String getNoLinkSelectedFormat() {
        return noLinkSelectedFormat;
    }

    
    public void setNoLinkSelectedFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "no link is selected";
        noLinkSelectedFormat = s;
        dirty = true;
    }

    
    public String getInvalidLinkFormat() {
        return invalidLinkFormat;
    }

    
    public void setInvalidLinkFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "invalid link selected";
        invalidLinkFormat = s;
        dirty = true;
    }

    
    public String getUnknownLinkFormat() {
        return unknownLinkFormat;
    }

    
    public void setUnknownLinkFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "unknown or offline destination gate";
        unknownLinkFormat = s;
        dirty = true;
    }

    
    public String getMarkerFormat() {
        return markerFormat;
    }

    
    public void setMarkerFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%name%";
        markerFormat = s;
        dirty = true;
    }

    
    public boolean getHidden() {
        return hidden;
    }

    
    public void setHidden(boolean b) {
        boolean old = hidden;
        hidden = b;
        dirty = dirty || (old != hidden);
    }

    
    public int getLinkAddDistance() {
        return linkAddDistance;
    }

    
    public void setLinkAddDistance(int i) {
        if (i <= 0) i = -1;
        linkAddDistance = i;
        dirty = true;
    }

    
    public int getCountdown() {
        return countdown;
    }

    
    public void setCountdown(int i) {
        countdown = i;
        dirty = true;
    }

    
    public int getCountdownInterval() {
        return countdownInterval;
    }

    
    public void setCountdownInterval(int i) {
        if (i < 1) i = 1;
        countdownInterval = i;
        dirty = true;
    }

    
    public String getCountdownFormat() {
        return countdownFormat;
    }

    
    public void setCountdownFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport countdown started...";
        countdownFormat = s;
        dirty = true;
    }

    
    public String getCountdownIntervalFormat() {
        return countdownIntervalFormat;
    }

    
    public void setCountdownIntervalFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport in %time% seconds...";
        countdownIntervalFormat = s;
        dirty = true;
    }

    
    public String getCountdownCancelFormat() {
        return countdownCancelFormat;
    }

    
    public void setCountdownCancelFormat(String s) {
        if (s != null) {
            if (s.equals("-")) s = "";
            else if (s.equals("*")) s = null;
        }
        if (s == null) s = "%RED%Teleport canceled";
        countdownCancelFormat = s;
        dirty = true;
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
        ctx.send("option '%s' set to '%s' for gate '%s'", name, value, getName(ctx));
    }

    
    public String getOptionPermission(Context ctx, String name) {
        return name + "." + name;
    }

    /* End options */

    public boolean canSendChat(String message, String format) {
        if ((! sendChat) || (message == null)) return false;
        if (sendChatFilter != null)
            if (! Pattern.compile(sendChatFilter).matcher(message).find()) return false;
        if (sendChatFormatFilter != null) {
            if (format == null) return false;
            if (! Pattern.compile(sendChatFormatFilter).matcher(format).find()) return false;
        }
        return true;
    }

    public boolean canReceiveChat(String message) {
        if ((! receiveChat) || (message == null)) return false;
        if (receiveChatFilter == null) return true;
        return Pattern.compile(receiveChatFilter).matcher(message).find();
    }

    public List<String> getLinks() {
        return new ArrayList<String>(links);
    }

    public boolean isLinked() {
        return ! links.isEmpty();
    }

    public boolean hasLink(String link) {
        return links.contains(link);
    }

    public void addLink(Context ctx, String toGateName) throws TransporterException {
        Permissions.require(ctx.getPlayer(), "trp.gate.link.add." + getFullName());

        if (isLinked() && (! getMultiLink()))
            throw new GateException("gate '%s' cannot accept multiple links", getName(ctx));

        if (ctx.isPlayer() && (linkAddDistance > 0)) {
            Location location = ctx.getPlayer().getLocation();
            if (location.getWorld() != world)
                throw new GateException("gate '%s' is too far away", getName(ctx));
            Vector there = new Vector(location.getX(), location.getY(), location.getZ());
            if (there.distance(center) > linkAddDistance)
                throw new GateException("gate '%s' is too far away", getName(ctx));
        }

        GateImpl toGate = Gates.find(ctx, toGateName);
        if (toGate == null)
            throw new GateException("gate '%s' cannot be found", toGateName);

        if (isSameWorld(toGate.getWorld())) {
            if (! Config.getAllowLinkLocal())
                throw new CommandException("linking to on-world gates is not permitted");
        } else {
            if (! Config.getAllowLinkWorld())
                throw new CommandException("linking to off-world gates is not permitted");
        }

        if (! addLink(toGate.getFullName()))
            throw new GateException("gate '%s' already links to '%s'", getName(ctx), toGate.getName(ctx));

        ctx.sendLog("added link from '%s' to '%s'", getName(ctx), toGate.getName(ctx));
    }

    protected boolean addLink(String link) {
        if (links.contains(link)) return false;
        links.add(link);
        if (links.size() == 1)
            outgoing = link;
        onDestinationChanged();
        dirty = true;
        return true;
    }

    public void removeLink(Context ctx, String toGateName) throws TransporterException {
        Permissions.require(ctx.getPlayer(), "trp.gate.link.remove." + getFullName());

        GateImpl toGate = Gates.find(toGateName);
        if (toGate != null) toGateName = toGate.getFullName();

        if (! removeLink(toGateName))
            throw new GateException("gate '%s' does not have a link to '%s'", getName(ctx), toGateName);

        ctx.sendLog("removed link from '%s' to '%s'", getName(ctx), toGateName);
    }

    protected boolean removeLink(String link) {
        if (! links.contains(link)) return false;
        links.remove(link);
        if (link.equals(outgoing))
            outgoing = null;
        onDestinationChanged();
        closeIfAllowed();
        dirty = true;
        return true;
    }

    public void nextLink() throws GateException {
        // trivial case of single link to prevent needless detach/attach
        if ((links.size() == 1) && links.contains(outgoing)) {
            //updateScreens();
            return;
        }

        // detach from the current gate
        if (portalOpen && (outgoing != null)) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.detach(this);
        }

        // select next link
        if ((outgoing == null) || (! links.contains(outgoing))) {
            if (! links.isEmpty()) {
                outgoing = links.get(0);
                dirty = true;
            }

        } else if (randomNextLink) {
            List<String> candidateLinks = new ArrayList<String>(links);
            candidateLinks.remove(outgoing);
            if (candidateLinks.size() > 1)
                Collections.shuffle(candidateLinks);
            if (! candidateLinks.isEmpty()) {
                outgoing = candidateLinks.get(0);
                dirty = true;
            }

        } else {
            int i = links.indexOf(outgoing) + 1;
            if (i >= links.size()) i = 0;
            outgoing = links.get(i);
            dirty = true;
        }

        onDestinationChanged();

        // attach to the next gate
        if (portalOpen && (outgoing != null)) {
            GateImpl gate = Gates.get(outgoing);
            if (gate != null)
                gate.attach(this);
            else {
                Utils.debug("closing if allowed");
                closeIfAllowed();
            }
        }
        getDestinationGate();
    }

    public boolean isLastLink() {
        if (outgoing == null)
            return links.isEmpty();
        return links.indexOf(outgoing) == (links.size() - 1);
    }

    public boolean hasValidDestination() {
        try {
            getDestinationGate();
            return true;
        } catch (GateException e) {
            return false;
        }
    }

    public String getDestinationLink() {
        return outgoing;
    }

    public GateImpl getDestinationGate() throws GateException {
        if (outgoing == null) {
            if (! isLinked())
                throw new GateException(getNoLinksFormat());
            else
                throw new GateException(getNoLinkSelectedFormat());
        } else if (! hasLink(outgoing))
            throw new GateException(getInvalidLinkFormat());
        GateImpl gate = Gates.get(outgoing);
        if (gate == null)
            throw new GateException(getUnknownLinkFormat());
        return gate;
    }

    
    public boolean addPin(String pin) throws GateException {
        if (! Pins.isValidPin(pin))
            throw new GateException("invalid pin");
        if (pins.contains(pin)) return false;
        pins.add(pin);
        dirty = true;
        return true;
    }

    
    public boolean removePin(String pin) {
        if (pins.contains(pin)) return false;
        pins.remove(pin);
        dirty = true;
        return true;
    }

    
    public void removeAllPins() {
        pins.clear();
        dirty = true;
    }

    
    public boolean hasPin(String pin) {
        return pins.contains(pin);
    }

    
    public Set<String> getBannedItems() {
        return bannedItems;
    }

    
    public boolean addBannedItem(String item) throws GateException {
        try {
            if (! Inventory.appendItemList(bannedItems, item)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeBannedItem(String item) throws GateException {
        try {
            if (! Inventory.removeItemList(bannedItems, item)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllBannedItems() {
        bannedItems.clear();
        dirty = true;
    }

    
    public Set<String> getAllowedItems() {
        return allowedItems;
    }

    
    public boolean addAllowedItem(String item) throws GateException {
        try {
            if (! Inventory.appendItemList(allowedItems, item)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeAllowedItem(String item) throws GateException {
        try {
            if (! Inventory.removeItemList(allowedItems, item)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllAllowedItems() {
        allowedItems.clear();
        dirty = true;
    }

    
    public Map<String,String> getReplaceItems() {
        return replaceItems;
    }

    
    public boolean addReplaceItem(String fromItem, String toItem) throws GateException {
        try {
            if (! Inventory.appendItemMap(replaceItems, fromItem, toItem)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeReplaceItem(String item) throws GateException {
        try {
            if (! Inventory.removeItemMap(replaceItems, item)) return false;
        } catch (InventoryException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllReplaceItems() {
        replaceItems.clear();
        dirty = true;
    }

    public boolean isAllowedGameMode(String mode) {
        if (allowGameModes == null) return false;
        if (allowGameModes.equals("*")) return true;
        for (String part : allowGameModes.split(","))
            if (part.equals(mode)) return true;
        return false;
    }

    public boolean isAcceptableInventory(ItemStack[] stacks) {
        if (stacks == null) return true;
        if (! requireAllowedItems) return true;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stack = stacks[i];
            if (stack == null) continue;
            if (Inventory.filterItemStack(stack, replaceItems, allowedItems, bannedItems) == null) return false;
        }
        return true;
    }

    public boolean filterInventory(ItemStack[] stacks) {
        if (stacks == null) return false;
        boolean filtered = false;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack newStack = Inventory.filterItemStack(stacks[i], replaceItems, allowedItems, bannedItems);
            if (newStack != stacks[i]) {
                stacks[i] = newStack;
                filtered = true;
            }
        }
        return filtered;
    }

    
    public Set<String> getBannedPotions() {
        return bannedPotions;
    }

    
    public boolean addBannedPotion(String potion) throws GateException {
        try {
            if (! PotionEffects.appendPotionList(bannedPotions, potion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeBannedPotion(String potion) throws GateException {
        try {
            if (! PotionEffects.removePotionList(bannedPotions, potion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllBannedPotions() {
        bannedPotions.clear();
        dirty = true;
    }

    
    public Set<String> getAllowedPotions() {
        return allowedPotions;
    }

    
    public boolean addAllowedPotion(String potion) throws GateException {
        try {
            if (! PotionEffects.appendPotionList(allowedPotions, potion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeAllowedPotion(String potion) throws GateException {
        try {
            if (! PotionEffects.removePotionList(allowedPotions, potion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllAllowedPotions() {
        allowedPotions.clear();
        dirty = true;
    }

    
    public Map<String,String> getReplacePotions() {
        return replacePotions;
    }

    
    public boolean addReplacePotion(String fromPotion, String toPotion) throws GateException {
        try {
            if (! PotionEffects.appendPotionMap(replacePotions, fromPotion, toPotion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public boolean removeReplacePotion(String potion) throws GateException {
        try {
            if (! PotionEffects.removePotionMap(replacePotions, potion)) return false;
        } catch (PotionEffectException e) {
            throw new GateException(e.getMessage());
        }
        dirty = true;
        return true;
    }

    
    public void removeAllReplacePotions() {
        replacePotions.clear();
        dirty = true;
    }

    public boolean isAcceptablePotions(PotionEffect[] effects) {
        if (effects == null) return true;
        if (! requireAllowedPotions) return true;
        for (int i = 0; i < effects.length; i++) {
            PotionEffect effect = effects[i];
            if (effect == null) continue;
            try {
                PotionEffects.filterPotionEffect(effect, replacePotions, allowedPotions, bannedPotions);
            } catch (PotionEffectException pee) {
                return false;
            }
        }
        return true;
    }

    public boolean filterPotions(PotionEffect[] effects) {
        if (effects == null) return false;
        boolean filtered = false;
        PotionEffect newEffect;
        for (int i = 0; i < effects.length; i++) {
            try {
                newEffect = PotionEffects.filterPotionEffect(effects[i], replacePotions, allowedPotions, bannedPotions);
            } catch (PotionEffectException pee) {
                newEffect = null;
            }
            if (newEffect != effects[i]) {
                effects[i] = newEffect;
                filtered = true;
            }
        }
        return filtered;
    }

    public boolean isInChatSendProximity(Location location) {
        if (! sendChat) return false;
        if (location.getWorld() != world) return false;
        if (sendChatDistance <= 0) return true;
        Vector there = new Vector(location.getX(), location.getY(), location.getZ());
        return (there.distance(center) <= sendChatDistance);
    }

    public boolean isInChatReceiveProximity(Location location) {
        if (! receiveChat) return false;
        if (location.getWorld() != world) return false;
        if (receiveChatDistance <= 0) return true;
        Vector there = new Vector(location.getX(), location.getY(), location.getZ());
        return (there.distance(center) <= receiveChatDistance);
    }

    protected void generateFile() {
        File worldFolder = Worlds.worldPluginFolder(world);
        File gatesFolder = new File(worldFolder, "gates");
        String fileName = name.replaceAll("[^\\w-\\.]", "_");
        if (name.hashCode() > 0) fileName += "-";
        fileName += name.hashCode();
        fileName += ".yml";
        file = new File(gatesFolder, fileName);
    }

    private void closeIfAllowed() {
        if (! portalOpen) return;
        if (canClose()) close();
    }

    private boolean canClose() {
//Utils.debug("duration < 1: %s", duration < 1);
//Utils.debug("! hasValidDestination: %s", ! hasValidDestination());
//Utils.debug("incoming.isEmpty(): %s", incoming.isEmpty());

        if (duration < 1)
            return (! hasValidDestination()) && incoming.isEmpty();

        // temporary gate
        boolean expired = ((System.currentTimeMillis() - portalOpenTime) + 50) > duration;
//Utils.debug("expired: %s", expired);
//Utils.debug("outgoing != null: %s", outgoing != null);
//Utils.debug("hasValidDestination: %s", hasValidDestination());
//Utils.debug("incoming.contains(outgoing): %s", incoming.contains(outgoing));
//Utils.debug("incoming.size() == 1: %s", incoming.size() == 1);

        // handle mutually paired gates
        if ((outgoing != null) && hasValidDestination() && incoming.contains(outgoing) && (incoming.size() == 1)) return expired;

//Utils.debug("incoming.isEmpty(): %s", incoming.isEmpty());
//Utils.debug("outgoing == null: %s", outgoing == null);

        if (incoming.isEmpty())
            return (outgoing == null) || expired;

        return false;
    }

}

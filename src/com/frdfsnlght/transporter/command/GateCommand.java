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
package com.frdfsnlght.transporter.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;

import com.frdfsnlght.transporter.Context;
import com.frdfsnlght.transporter.GateImpl;
import com.frdfsnlght.transporter.Gates;
import com.frdfsnlght.transporter.Permissions;
import com.frdfsnlght.transporter.api.TransporterException;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public class GateCommand extends TrpCommandProcessor {

    private static final String GROUP = "gate ";

    @Override
    public boolean matches(Context ctx, Command cmd, List<String> args) {
        return super.matches(ctx, cmd, args) &&
               GROUP.startsWith(args.get(0).toLowerCase());
    }

    @Override
    public List<String> getUsage(Context ctx) {
        List<String> cmds = new ArrayList<String>();
        cmds.add(getPrefix(ctx) + GROUP + "list");
        cmds.add(getPrefix(ctx) + GROUP + "select <gate>");
        cmds.add(getPrefix(ctx) + GROUP + "info [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "open [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "close [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "rebuild [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "destroy [<gate>] [unbuild]");
        cmds.add(getPrefix(ctx) + GROUP + "rename <newname> [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "link add [<from>] <to> [rev]");
        cmds.add(getPrefix(ctx) + GROUP + "link remove [<from>] <to> [rev]");

        cmds.add(getPrefix(ctx) + GROUP + "get <option>|* [<gate>]");
        cmds.add(getPrefix(ctx) + GROUP + "set <option> <value> [<gate>]");
        return cmds;
    }

    @Override
    public void process(Context ctx, Command cmd, List<String> args) throws TransporterException {
        args.remove(0);
        if (args.isEmpty())
            throw new CommandException("do what with a gate?");
        String subCmd = args.remove(0).toLowerCase();

        if ("list".startsWith(subCmd)) {
            Permissions.require(ctx.getPlayer(), "trp.gate.list");

            List<GateImpl> localGates = new ArrayList<GateImpl>(Gates.getGates());
            if ((! ctx.isConsole()) && (! ctx.isOp()))
                for (Iterator<GateImpl> i = localGates.iterator(); i.hasNext(); )
                    if (i.next().getHidden()) i.remove();
            if (localGates.isEmpty())
                ctx.send("there are no local gates");
            else {
                Collections.sort(localGates, new Comparator<GateImpl>() {
                    public int compare(GateImpl a, GateImpl b) {
                        return a.getFullName().compareToIgnoreCase(b.getFullName());
                    }
                });
                ctx.send("%d local gates:", localGates.size());
                for (GateImpl gate : localGates)
                    ctx.send("  %s", gate.getFullName());
            }
            return;
        }

        if ("select".startsWith(subCmd)) {
            GateImpl gate = getGate(ctx, args);
            Permissions.require(ctx.getPlayer(), "trp.gate.select." + gate.getFullName());
            Gates.setSelectedGate(ctx.getPlayer(), gate);
            ctx.send("selected gate '%s'", gate.getFullName());
            return;
        }

        if ("info".startsWith(subCmd)) {
            GateImpl gate = getGate(ctx, args);
            Permissions.require(ctx.getPlayer(), "trp.gate.info." + gate.getFullName());
            ctx.send("Full name: %s", gate.getFullName());
            List<String> links = gate.getLinks();
            ctx.send("Links: %d", links.size());
            for (String link : links)
                ctx.send(" %s%s", link.equals(gate.getDestinationLink()) ? "*": "", link);
            return;
        }

        if ("open".startsWith(subCmd)) {
            GateImpl gate = getGate(ctx, args);
            if (gate.isOpen())
                ctx.warn("gate '%s' is already open", gate.getName(ctx));
            else {
                Permissions.require(ctx.getPlayer(), "trp.gate.open." + gate.getFullName());
                gate.open();
                ctx.sendLog("opened gate '%s'", gate.getName(ctx));
            }
            return;
        }

        if ("close".startsWith(subCmd)) {
            GateImpl gate = getGate(ctx, args);
            if (gate.isOpen()) {
                Permissions.require(ctx.getPlayer(), "trp.gate.close." + gate.getFullName());
                gate.close();
                ctx.sendLog("closed gate '%s'", gate.getName(ctx));
            } else
                ctx.warn("gate '%s' is already closed", gate.getName(ctx));
            return;
        }

        if ("rebuild".startsWith(subCmd)) {
            GateImpl gate = getGate(ctx, args);
            Permissions.require(ctx.getPlayer(), "trp.gate.rebuild." + gate.getFullName());
            gate.rebuild();
            ctx.sendLog("rebuilt gate '%s'", gate.getName(ctx));
            return;
        }

        if ("destroy".startsWith(subCmd)) {
            boolean unbuild = false;
            if ((args.size() > 0) && "unbuild".startsWith(args.get(args.size() - 1).toLowerCase())) {
                unbuild = true;
                args.remove(args.size() - 1);
            }
            GateImpl gate = getGate(ctx, args);
            Permissions.require(ctx.getPlayer(), "trp.gate.destroy." + gate.getFullName());
            Gates.destroy(gate, unbuild);
            ctx.sendLog("destroyed gate '%s'", gate.getName(ctx));
            return;
        }

        if ("rename".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("new name required");
            String newName = args.remove(0);
            GateImpl gate = getGate(ctx, args);
            String oldName = gate.getName(ctx);
            Permissions.require(ctx.getPlayer(), "trp.gate.rename");
            Gates.rename(gate, newName);
            ctx.sendLog("renamed gate '%s' to '%s'", oldName, gate.getName(ctx));
            return;
        }

        if ("link".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("do what with a link?");
            subCmd = args.remove(0).toLowerCase();

            if (args.isEmpty())
                throw new CommandException("destination endpoint required");
            boolean reverse = false;
            if ("reverse".startsWith(args.get(args.size() - 1).toLowerCase())) {
                reverse = true;
                args.remove(args.size() - 1);
            }
            if (args.isEmpty())
                throw new CommandException("destination endpoint required");

            String toGateName = args.remove(args.size() - 1);
            GateImpl fromGate = getGate(ctx, args);

            GateImpl toGate = Gates.find(ctx, toGateName);

            if ("add".startsWith(subCmd)) {
                fromGate.addLink(ctx, toGateName);
                if (reverse && (ctx.getSender() != null) && (toGate != null)) {
                    Bukkit.dispatchCommand(ctx.getSender(), "trp gate link add \"" + toGate.getFullName() + "\" \"" + fromGate.getFullName() + "\"");
                }
                return;
            }

            if ("remove".startsWith(subCmd)) {
                fromGate.removeLink(ctx, toGateName);
                if (reverse && (ctx.getSender() != null) && (toGate != null)) {
                    Bukkit.dispatchCommand(ctx.getSender(), "trp gate link remove \"" + fromGate.getFullName() + "\" \"" + toGate.getFullName() + "\"");
                }
                return;
            }
            throw new CommandException("do what with a link?");
        }

        if ("create".startsWith(subCmd)) {
            if (! ctx.isPlayer())
                throw new CommandException("must be a player to use this command");
            ctx.getPlayer().performCommand("trp design create " + rebuildCommandArgs(args));
            return;
        }

        if ("set".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("option name required");
            String option = args.remove(0);
            if (args.isEmpty())
                throw new CommandException("option value required");
            String value = args.remove(0);
            GateImpl gate = getGate(ctx, args);
            gate.setOption(ctx, option, value);
            return;
        }

        if ("get".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("option name required");
            String option = args.remove(0);
            GateImpl gate = getGate(ctx, args);
            gate.getOptions(ctx, option);
            return;
        }

        throw new CommandException("do what with a gate?");
    }

    private GateImpl getGate(Context ctx, List<String> args) throws CommandException {
        GateImpl gate;
        if (! args.isEmpty()) {
            gate = Gates.find(ctx, args.get(0));
            if (gate == null)
                throw new CommandException("unknown gate '%s'", args.get(0));
            args.remove(0);
        } else
            gate = Gates.getSelectedGate(ctx.getPlayer());
        if (gate == null)
            throw new CommandException("gate name required");
        return gate;
    }

}

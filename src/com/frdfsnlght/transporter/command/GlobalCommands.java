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
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import com.frdfsnlght.transporter.Config;
import com.frdfsnlght.transporter.Context;
import com.frdfsnlght.transporter.Gates;
import com.frdfsnlght.transporter.LocalGateImpl;
import com.frdfsnlght.transporter.Permissions;
import com.frdfsnlght.transporter.ReservationImpl;
import com.frdfsnlght.transporter.api.ReservationException;
import com.frdfsnlght.transporter.api.TransporterException;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class GlobalCommands extends TrpCommandProcessor {

    @Override
    public boolean matches(Context ctx, Command cmd, List<String> args) {
        return super.matches(ctx, cmd, args) && (
               ("list".startsWith(args.get(0).toLowerCase())) ||
               ("get".startsWith(args.get(0).toLowerCase())) ||
               ("set".startsWith(args.get(0).toLowerCase())) ||
               ("go".startsWith(args.get(0).toLowerCase())) ||
               ("send".startsWith(args.get(0).toLowerCase()))
            );

    }

    @Override
    public List<String> getUsage(Context ctx) {
        List<String> cmds = new ArrayList<String>();
        cmds.add(getPrefix(ctx) + "list");
        cmds.add(getPrefix(ctx) + "get <option>|*");
        cmds.add(getPrefix(ctx) + "set <option> <value>");
        if (ctx.isPlayer())
            cmds.add(getPrefix(ctx) + "go [<gate>]");
        cmds.add(getPrefix(ctx) + "send <player> [<gate>]");
        return cmds;
    }

    @Override
    public void process(Context ctx, Command cmd, List<String> args) throws TransporterException {
        if (args.isEmpty())
            throw new CommandException("do what?");
        String subCmd = args.remove(0).toLowerCase();

        if ("list".startsWith(subCmd)) {
            Permissions.require(ctx.getPlayer(), "trp.list");
            List<Player> localPlayers = new ArrayList<Player>();
            Collections.addAll(localPlayers, (Player[])Bukkit.getOnlinePlayers().toArray());

            if (localPlayers.isEmpty())
                ctx.send("there are no players");
            else {
                Collections.sort(localPlayers, new Comparator<Player>() {
                    public int compare(Player a, Player b) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });

                ctx.send("%d local players:", localPlayers.size());
                for (Player p : localPlayers)
                    ctx.send("  %s (%s)", p.getDisplayName(), p.getWorld().getName());
            }
            return;
        }

        if ("go".startsWith(subCmd)) {
            if (! ctx.isPlayer())
                throw new CommandException("this command can only be used by a player");
            LocalGateImpl gate;
            if (! args.isEmpty()) {
                String name = args.remove(0);
                gate = Gates.find(ctx, name);
                if (gate == null)
                    throw new CommandException("unknown gate '%s'", name);
            } else
                gate = Gates.getSelectedGate(ctx.getPlayer());
            if (gate == null)
                throw new CommandException("gate name required");

            Permissions.require(ctx.getPlayer(), "trp.go." + gate.getFullName());
            try {
                ReservationImpl r = new ReservationImpl(ctx.getPlayer(), gate);
                r.depart();
            } catch (ReservationException e) {
                ctx.warnLog(e.getMessage());
            }
            return;
        }

        if ("send".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("player name required");
            Player player = Bukkit.getPlayer(args.remove(0));
            if (player == null)
                throw new CommandException("unknown player");
            LocalGateImpl gate;
            if (! args.isEmpty()) {
                String name = args.remove(0);
                gate = Gates.find(ctx, name);
                if (gate == null)
                    throw new CommandException("unknown gate '%s'", name);
            } else
                gate = Gates.getSelectedGate(ctx.getPlayer());
            if (gate == null)
                throw new CommandException("gate name required");

            Permissions.require(ctx.getPlayer(), "trp.send." + gate.getFullName());
            try {
                ReservationImpl res = new ReservationImpl(player, gate);
                ctx.send("sending player '%s' to '%s'", player.getName(), gate.getLocalName());
                res.depart();
            } catch (ReservationException re) {
                throw new CommandException(re.getMessage());
            }
            return;
        }

        if ("set".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("option name required");
            String option = args.remove(0);
            if (args.isEmpty())
                throw new CommandException("option value required");
            String value = args.remove(0);
            Config.setOption(ctx, option, value);
            return;
        }

        if ("get".startsWith(subCmd)) {
            if (args.isEmpty())
                throw new CommandException("option name required");
            String option = args.remove(0);
            Config.getOptions(ctx, option);
//            return;
        }

    }

}

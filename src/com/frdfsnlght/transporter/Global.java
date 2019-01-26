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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.PluginDescriptionFile;

import com.frdfsnlght.transporter.command.CommandProcessor;
import com.frdfsnlght.transporter.command.DebugCommand;
import com.frdfsnlght.transporter.command.DesignCommand;
import com.frdfsnlght.transporter.command.GateCommand;
import com.frdfsnlght.transporter.command.GlobalCommands;
import com.frdfsnlght.transporter.command.HelpCommand;
import com.frdfsnlght.transporter.command.ReloadCommand;
import com.frdfsnlght.transporter.command.SaveCommand;
import com.frdfsnlght.transporter.command.WorldCommand;
import com.frdfsnlght.transporter.test.TestCommand;

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Global {

    public static Transporter plugin = null;
    public static String pluginName;
    public static String pluginVersion;

    public static Thread mainThread = null;
    public static boolean enabled = false;
    public static boolean started = false;

    public static final List<CommandProcessor> commands = new ArrayList<CommandProcessor>();

    static {
        commands.add(new HelpCommand());
        commands.add(new ReloadCommand());
        commands.add(new SaveCommand());
        commands.add(new GlobalCommands());
        commands.add(new DesignCommand());
        commands.add(new GateCommand());
        commands.add(new WorldCommand());
        commands.add(new DebugCommand());

        if (isTesting()) {
            System.out.println("**** Transporter testing mode is enabled! ****");
            commands.add(new TestCommand());
        }
    }

    public static boolean isTesting() {
        return System.getenv("TRANSPORTER_TEST") != null;
    }

    public static void setPlugin(Transporter plugin) {
        Global.plugin = plugin;

        if (plugin != null) {
            PluginDescriptionFile pdf = plugin.getDescription();
            Global.pluginName = pdf.getName();
            Global.pluginVersion = pdf.getVersion();
        }
    }

}

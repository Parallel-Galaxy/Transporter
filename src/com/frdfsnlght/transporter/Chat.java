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

/**
 *
 * @author frdfsnlght <frdfsnlght@gmail.com>
 */
public final class Chat {

    private static Pattern colorPattern = Pattern.compile("%(\\w+)%");

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

}

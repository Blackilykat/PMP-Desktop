/*
 * PMP-Desktop - A desktop client for Personal Music Platform, a
 * self-hosted platform to play music and make sure everything is
 * always synced across devices.
 * Copyright (C) 2024 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import org.h2.mvstore.MVStore;

import java.io.File;
import java.util.Map;

public class Storage {
    public static final File LIBRARY = new File("library/");

    public static Map<String, Object> general;

    public static void init() {
        MVStore mvStore = MVStore.open("db");
        general = mvStore.openMap("general");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mvStore.close();
        }));

        if(!LIBRARY.exists()) {
            LIBRARY.mkdir();
        }
    }

    public static int getCurrentActionID() {
        return (Integer) general.getOrDefault("currentActionID", -1);
    }

    public static void setCurrentActionID(int newValue) {
        general.put("currentActionID", newValue);
    }
}

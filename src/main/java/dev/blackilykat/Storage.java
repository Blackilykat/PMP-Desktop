/*
 * Copyright (C) 2025 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import dev.blackilykat.util.Triple;
import dev.blackilykat.widgets.filters.LibraryFilter;
import dev.blackilykat.widgets.filters.LibraryFilterOption;
import dev.blackilykat.widgets.tracklist.Order;
import dev.blackilykat.widgets.tracklist.TrackDataHeader;
import org.h2.mvstore.MVStore;

import java.io.File;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@SuppressWarnings("unchecked")
public class Storage {
    public static final File LIBRARY = new File("library/");

    public static Map<String, Object> general;
    private static Queue<LibraryAction> pendingLibraryActions = null;

    public static void init() {
        MVStore mvStore = MVStore.open("db");
        general = mvStore.openMap("general");
        pendingLibraryActions = (Queue<LibraryAction>) general.getOrDefault("pendingLibraryActions", new LinkedList<>());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Map<String, Map<String, LibraryFilterOption.State>> filters = new HashMap<>();
            if(Audio.INSTANCE != null && Audio.INSTANCE.currentSession != null) {
                for(LibraryFilter filter : Audio.INSTANCE.currentSession.getLibraryFilters()) {
                    Map<String, LibraryFilterOption.State> options = new HashMap<>();
                    for(LibraryFilterOption option : filter.getOptions()) {
                        options.put(option.value, option.getState());
                    }
                    filters.put(filter.key, options);
                }
                Storage.setFilters(filters);
            }

            List<Triple<String, String, Integer>> headers = new ArrayList<>();
            for(TrackDataHeader header : Main.songListWidget.dataHeaders) {
                headers.add(new Triple<>(header.name, header.metadataKey, header.width));
            }
            Storage.setTrackHeaders(headers);

            general.put("pendingLibraryActions", pendingLibraryActions);

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

    public static Map<String, Map<String, LibraryFilterOption.State>> getFilters() {
        return (Map<String, Map<String, LibraryFilterOption.State>>) general.getOrDefault("filters", Map.of());
    }

    public static void setFilters(Map<String, Map<String, LibraryFilterOption.State>> filters) {
        if(filters == null) {
            general.remove("filters");
            return;
        }
        general.put("filters", filters);
    }

    /**
     * @return a list of all the track headers. The values in each triple are label, key, width.
     */
    public static List<Triple<String, String, Integer>> getTrackHeaders() {
        return (List<Triple<String, String, Integer>>) general.getOrDefault("trackHeaders", null);
    }

    public static void setTrackHeaders(List<Triple<String, String, Integer>> trackHeaders) {
        if(trackHeaders == null) {
            general.remove("trackHeaders");
            return;
        }
        general.put("trackHeaders", trackHeaders);
    }

    public static Key getServerPublicKey() {
        return (Key) general.getOrDefault("serverPublicKey", null);
    }

    public static void setServerPublicKey(Key key) {
        if(key == null) {
            general.remove("serverPublicKey");
            return;
        }
        general.put("serverPublicKey", key);
    }

    public static String getServerIp() {
        return (String) general.getOrDefault("serverIp", "localhost");
    }

    public static void setServerIp(String serverIp) {
        if(serverIp == null) {
            general.remove("serverIp");
            return;
        }
        general.put("serverIp", serverIp);
    }
    public static int getServerMainPort() {
        return (Integer) general.getOrDefault("serverMainPort", 5000);
    }

    public static void setServerMainPort(int mainPort) {
        if(mainPort < 0) {
            general.remove("serverMainPort");
            return;
        }
        general.put("serverMainPort", mainPort);
    }
    public static int getServerFilePort() {
        return (Integer) general.getOrDefault("serverFilePort", 5001);
    }

    public static void setServerFilePort(int port) {
        if(port < 0) {
            general.remove("serverFilePort");
            return;
        }
        general.put("serverFilePort", port);
    }

    public static void pushPendingLibraryAction(LibraryAction action) {
        pendingLibraryActions.offer(action);
    }

    public static LibraryAction popPendingLibraryAction() {
        return pendingLibraryActions.poll();
    }
}

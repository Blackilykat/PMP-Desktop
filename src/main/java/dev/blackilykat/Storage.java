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
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Storage {
    public static final File LIBRARY = new File("library/");

    public static Map<String, Object> general;

    public static void init() {
        MVStore mvStore = MVStore.open("db");
        general = mvStore.openMap("general");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Map<String, Map<String, LibraryFilterOption.State>> filters = new HashMap<>();
            for(LibraryFilter filter : Library.INSTANCE.filters) {
                Map<String, LibraryFilterOption.State> options = new HashMap<>();
                for(LibraryFilterOption option : filter.getOptions()) {
                    options.put(option.value, option.state);
                }
                filters.put(filter.key, options);
            }
            Storage.setFilters(filters);

            List<Triple<String, String, Integer>> headers = new ArrayList<>();
            for(TrackDataHeader header : Main.songListWidget.dataHeaders) {
                headers.add(new Triple<>(header.name, header.metadataKey, header.width));
            }
            Storage.setTrackHeaders(headers);

            Storage.setSortingHeader(Main.songListWidget.dataHeaders.indexOf(Main.songListWidget.orderingHeader));
            Storage.setSortingOrder(Main.songListWidget.order);

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

    /**
     * @return The index of the header that tracks are being sorted by currently. -1 if none.
     */
    public static int getSortingHeader() {
        return (Integer) general.getOrDefault("sortingHeaderIndex", -1);
    }

    public static void setSortingHeader(int sortingHeader) {
        if(sortingHeader < 0) {
            general.remove("sortingHeaderIndex");
            return;
        }
        general.put("sortingHeaderIndex", sortingHeader);
    }

    public static Order getSortingOrder() {
        return (Order) general.getOrDefault("sortingOrder", Order.DESCENDING);
    }

    public static void setSortingOrder(Order order) {
        if(order == null) {
            general.remove("sortingOrder");
            return;
        }
        general.put("sortingOrder", order);
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
}

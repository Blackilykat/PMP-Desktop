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

package dev.blackilykat.messages;

import com.google.gson.JsonObject;
import dev.blackilykat.Audio;
import dev.blackilykat.Json;
import dev.blackilykat.Library;
import dev.blackilykat.Main;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;
import dev.blackilykat.Track;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.filters.LibraryFilter;
import dev.blackilykat.widgets.filters.LibraryFilterOption;
import dev.blackilykat.widgets.playbar.PlayBarWidget;
import dev.blackilykat.widgets.playbar.TimeBar;
import dev.blackilykat.widgets.tracklist.Order;
import dev.blackilykat.widgets.tracklist.TrackDataHeader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Updates an existing playback session which is known by both the server and the client.
 */
public class PlaybackSessionUpdateMessage extends Message {
    public static final String MESSAGE_TYPE = "PLAYBACK_SESSION_UPDATE";

    public static final PlaybackSessionUpdateMessage VOID_BUFFER = new PlaybackSessionUpdateMessage(-1, null, null, null, null, null, null, null, null, null, null);

    public String track;
    public PlaybackSession.ShuffleOption shuffle;
    public PlaybackSession.RepeatOption repeat;
    public Boolean playing;
    public Integer position;
    public Integer owner;
    public Instant time;
    public int sessionId;
    public List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters;
    public Integer sortingHeader;
    public Order sortingOrder;

    public PlaybackSessionUpdateMessage(int sessionId) {
        this(sessionId, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * @param sessionId The session to update
     * @param track The new track that's playing, null if unchanged
     * @param shuffle The new shuffle option, null if unchanged
     * @param repeat The new repeat option, null if unchanged
     * @param playing Whether it's currently playing or not, null if unchanged
     * @param position The new position, null if unchanged (This should not be sent during normal progression of a track,
     *                 but only at jumps)
     */
    public PlaybackSessionUpdateMessage(
            int sessionId,
            String track,
            PlaybackSession.ShuffleOption shuffle,
            PlaybackSession.RepeatOption repeat,
            Boolean playing,
            Integer position,
            Integer owner,
            List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters,
            Integer sortingHeader,
            Order sortingOrder,
            Instant time
    ) {
        this.sessionId = sessionId;
        this.track = track;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.playing = playing;
        this.position = position;
        this.owner = owner;
        this.filters = filters;
        this.sortingHeader = sortingHeader;
        this.sortingOrder = sortingOrder;
        this.time = time;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
        object.addProperty("sessionId", sessionId);
        if(track != null) {
            object.addProperty("track", track);
        }
        if(shuffle != null) {
            object.addProperty("shuffle", shuffle.toString());
        }
        if(repeat != null) {
            object.addProperty("repeat", repeat.toString());
        }
        if(playing != null) {
            object.addProperty("playing", playing);
        }
        if(position != null) {
            object.addProperty("position", position);
        }
        if(time != null) {
            object.addProperty("time", time.toEpochMilli());
        }
        if(owner != null) {
            object.addProperty("owner", owner);
        }
        if(filters != null) {
            object.add("filters", Json.GSON.toJsonTree(filters));
        }
        if(sortingHeader != null) {
            object.addProperty("sortingHeader", sortingHeader);
        }
        if(sortingOrder != null) {
            object.addProperty("sortingOrder", sortingOrder.toString());
        }
    }

    @Override
    public void handle(ServerConnection connection) {
        PlaybackSession session = null;
        for(PlaybackSession sessionInLoop : PlaybackSession.getAvailableSessions()) {
            if(!sessionInLoop.acknowledgedByServer || sessionInLoop.id != sessionId) continue;
            session = sessionInLoop;
            break;
        }
        if(session == null) return;

        boolean shouldReloadSorting = false;

        if(position != null) {
            session.lastSharedPosition = position;
            session.lastSharedPositionTime = time;
            session.recalculatePosition(VOID_BUFFER, Instant.now());
        }
        if(track != null) {
            for(Track t : Library.INSTANCE.tracks) {
                if(!t.getFile().getName().equals(track)) continue;
                if(Audio.INSTANCE.currentSession == session) {
                    Audio.INSTANCE.startPlaying(VOID_BUFFER, t, false);
                } else {
                    session.setCurrentTrack(t);
                }
            }
        }
        if(shuffle != null) {
            session.setShuffle(VOID_BUFFER, shuffle);
        }
        if(repeat != null) {
            session.setRepeat(VOID_BUFFER, repeat);
        }
        if(playing != null) {
            if(position == null) {
                if(session.getOwnerId() != ServerConnection.INSTANCE.clientId) {
                    session.recalculatePosition(VOID_BUFFER, time);
                }

                session.lastSharedPosition = session.getPosition();

                if(session.getOwnerId() == ServerConnection.INSTANCE.clientId) {
                    session.lastSharedPositionTime = Instant.now();
                    connection.send(new PlaybackSessionUpdateMessage(sessionId, null, null, null, null, session.lastSharedPosition, null, null, null, null, session.lastSharedPositionTime));
                } else {
                    session.lastSharedPositionTime = time;
                    if(playing) {
                        // can get redundant but it's fine. If I didn't set this to true the next line would be useless
                        session.setPlaying(VOID_BUFFER, true);
                        session.recalculatePosition(VOID_BUFFER, Instant.now());
                    }
                }
            }

            session.setPlaying(VOID_BUFFER, playing);
        }
        if(owner != null) {
            session.setOwnerId(VOID_BUFFER, owner);
        }
        if(filters != null) {
            session.setLibraryFilters(VOID_BUFFER, asFilterObject(filters, session));
            Main.libraryFiltersWidget.reloadPanels();
            Main.libraryFiltersWidget.reloadElements();
            Library.INSTANCE.reloadFilters(VOID_BUFFER);
            shouldReloadSorting = true;
        }
        if(sortingHeader != null) try {
            session.setSortingHeader(VOID_BUFFER, TrackDataHeader.getById(sortingHeader));
            shouldReloadSorting = true;
        } catch(IndexOutOfBoundsException ignored) {
        }
        if(sortingOrder != null) {
            session.setSortingOrder(VOID_BUFFER, sortingOrder);
            shouldReloadSorting = true;
        }

        if(shouldReloadSorting) {
            Library.INSTANCE.reloadSorting();
        }

        Main.playBarWidget.repaint();
    }

    //@Override
    public static PlaybackSessionUpdateMessage fromJson(JsonObject json) throws MessageException {

        final List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters = json.has("filters") ? new ArrayList<>() : null;

        if(json.has("filters")) {
            json.getAsJsonArray("filters").asList().stream()
                    .map(elem -> elem.getAsJsonObject())
                    .forEach(filterObj -> {
                        Pair<String, List<Pair<String, LibraryFilterOption.State>>> filter = new Pair<>(filterObj.get("key").getAsString(), new ArrayList<>());

                        filterObj.getAsJsonArray("value").asList().stream()
                                .map(elem -> elem.getAsJsonObject())
                                .forEach(optionObj -> {
                                    filter.value.add(new Pair<>(optionObj.get("key").getAsString(), LibraryFilterOption.State.valueOf(optionObj.get("value").getAsString())));
                                });
                        assert filters != null;
                        filters.add(filter);
                    });
        }

        return new PlaybackSessionUpdateMessage(
                json.get("sessionId").getAsInt(),
                json.has("track") ? json.get("track").getAsString() : null,
                json.has("shuffle") ? PlaybackSession.ShuffleOption.valueOf(json.get("shuffle").getAsString()) : null,
                json.has("repeat") ? PlaybackSession.RepeatOption.valueOf(json.get("repeat").getAsString()) : null,
                json.has("playing") ? json.get("playing").getAsBoolean() : null,
                json.has("position") ? json.get("position").getAsInt() : null,
                json.has("owner") ? json.get("owner").getAsInt() : null,
                filters,
                json.has("sortingHeader") ? json.get("sortingHeader").getAsInt() : null,
                json.has("sortingOrder") ? Order.valueOf(json.get("sortingOrder").getAsString()) : null,
                json.has("time") ? Instant.ofEpochMilli(json.get("time").getAsLong()) : Instant.now()
        );
    }

    public static void doUpdate(
            PlaybackSessionUpdateMessage buffer,
            int sessionId,
            String track,
            PlaybackSession.ShuffleOption shuffle,
            PlaybackSession.RepeatOption repeat,
            Boolean playing,
            Integer position,
            List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters,
            Integer owner,
            Integer sortingHeader,
            Order sortingOrder
    ) {
        if(buffer != null) {
            buffer.sessionId = sessionId;
            if(track != null) buffer.track = track;
            if(shuffle != null) buffer.shuffle = shuffle;
            if(repeat != null) buffer.repeat = repeat;
            if(playing != null) buffer.playing = playing;
            if(position != null) buffer.position = position;
            if(filters != null) buffer.filters = filters;
            if(owner != null) buffer.owner = owner;
            if(sortingHeader != null) buffer.sortingHeader = sortingHeader;
            if(sortingOrder != null) buffer.sortingOrder = sortingOrder;
        } else {
            if(ServerConnection.INSTANCE != null) {
                ServerConnection.INSTANCE.send(new PlaybackSessionUpdateMessage(sessionId, track, shuffle, repeat, playing, position, owner, filters, sortingHeader, sortingOrder, Instant.now()));
            }
        }
    }

    public static List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> getFiltersFromSession(PlaybackSession session) {
        List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> list = new ArrayList<>();
        for(LibraryFilter filter : session.getLibraryFilters()) {
            Pair<String, List<Pair<String, LibraryFilterOption.State>>> filterPair = new Pair<>(filter.key, new ArrayList<>());
            for(LibraryFilterOption option : filter.getOptions()) {
                filterPair.value.add(new Pair<>(option.value, option.getState()));
            }
            list.add(filterPair);
        }
        return list;
    }

    public static List<LibraryFilter> asFilterObject(List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters, PlaybackSession session) {
        List<LibraryFilter> filtersInSession = new ArrayList<>();
        for(Pair<String, List<Pair<String, LibraryFilterOption.State>>> filter : filters) {
            LibraryFilter filterInSession = new LibraryFilter(Library.INSTANCE, session, filter.key);
            List<LibraryFilterOption> options = new ArrayList<>();
            for(Pair<String, LibraryFilterOption.State> option : filter.value) {
                LibraryFilterOption optionInSession = new LibraryFilterOption(filterInSession, option.key);
                optionInSession.setState(option.value, false);
                options.add(optionInSession);
            }
            filterInSession.setOptions(options);
            filtersInSession.add(filterInSession);
        }
        return filtersInSession;
    }
}

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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.Main;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Track;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.util.Pair;
import dev.blackilykat.widgets.filters.LibraryFilterOption;
import dev.blackilykat.widgets.tracklist.Order;
import dev.blackilykat.widgets.tracklist.TrackDataHeader;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static dev.blackilykat.messages.PlaybackSessionUpdateMessage.VOID_BUFFER;

/**
 * Contains a list of all sessions that currently exist. When the server sends this, clients must ensure their session
 * list is identical to the one contained in this message, whether that's by removing, adding or modifying its existing
 * session list. If clients need a session to keep existing regardless of whether it was previously on the server,
 * they can keep it and send a {@link PlaybackSessionCreateMessage}, ensuring there is no mismatching information
 * between the client and the server.
 */
public class PlaybackSessionListMessage extends Message {
    public static final String MESSAGE_TYPE = "PLAYBACK_SESSION_LIST";
    public List<PlaybackSessionElement> sessions = new ArrayList<>();

    public PlaybackSessionListMessage(Collection<PlaybackSessionElement> sessions) {
        this.sessions.addAll(sessions);
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public void fillContents(JsonObject object) {
    }

    @Override
    public void handle(ServerConnection connection) {
        if(!this.sessions.isEmpty()) {
            PlaybackSession[] existingSessions = PlaybackSession.getAvailableSessions();
            for(PlaybackSession existingSession : existingSessions) {
                boolean stillExists = false;
                for(PlaybackSessionElement listedSession : this.sessions) {
                    if(existingSession.id == listedSession.id) {
                        stillExists = true;
                        applyToPlaybackSession(existingSession, listedSession);
                    }
                }
                if(!stillExists) {
                    existingSession.unregister();
                }
            }
            boolean shouldReselect = PlaybackSession.getAvailableSessions().length == 0;

            for(PlaybackSessionElement listedSession : this.sessions) {
                boolean existed = false;
                for(PlaybackSession existingSession : existingSessions) {
                    if(existingSession.id == listedSession.id) {
                        existed = true;
                        break;
                    }
                }
                if(!existed) {
                    PlaybackSession session = new PlaybackSession(Audio.INSTANCE, listedSession.id);
                    applyToPlaybackSession(session, listedSession);
                    session.register();
                }

                if(listedSession.id > PlaybackSession.idCounter) {
                    PlaybackSession.idCounter = listedSession.id;
                }
            }

            if(shouldReselect) {
                Audio.INSTANCE.reselectSession(true);
            }
        } else {
            connection.send(new PlaybackSessionCreateMessage(0, null));
            Audio.INSTANCE.currentSession.setOwnerId(null, connection.clientId);
        }
        Main.libraryFiltersWidget.reloadPanels();
        Main.libraryFiltersWidget.reloadElements();
    }

    private static void applyToPlaybackSession(PlaybackSession session, PlaybackSessionElement element) {
        assert ServerConnection.INSTANCE != null;
        boolean allowedToOverride = session.getOwnerId() == ServerConnection.oldClientId || session.getOwnerId() == -1;
        boolean hasNewerInfo = (element.lastUpdateTime == null && session.lastSharedPositionTime != null) || (session.lastSharedPositionTime != null && session.lastSharedPositionTime.isAfter(element.lastUpdateTime));

        if(hasNewerInfo && allowedToOverride) {
            PlaybackSessionUpdateMessage buffer = new PlaybackSessionUpdateMessage(session.id);
            session.recalculatePosition(buffer, Instant.now());
            session.setOwnerId(buffer, ServerConnection.INSTANCE.clientId);
            PlaybackSessionUpdateMessage.doUpdate(buffer,
                    session.id,
                    session.getCurrentTrack() != null ? session.getCurrentTrack().getFile().getName() : null,
                    session.getShuffle(),
                    session.getRepeat(),
                    session.getPlaying(),
                    session.getPosition(),
                    PlaybackSessionUpdateMessage.getFiltersFromSession(session),
                    ServerConnection.INSTANCE.clientId,
                    session.getSortingHeader() != null ? session.getSortingHeader().id : null,
                    session.getSortingOrder());

            ServerConnection.INSTANCE.send(buffer);
        } else {
            Track track = null;
            for(Track t : Library.INSTANCE.tracks) {
                if(t.getFile().getName().equals(element.track)) {
                    track = t;
                    break;
                }
            }
            if(Audio.INSTANCE.currentSession == session) {
                Audio.INSTANCE.startPlaying(VOID_BUFFER, track, false);
            } else {
                session.setCurrentTrack(track);
            }
            session.setShuffle(VOID_BUFFER, element.shuffle);
            session.setRepeat(VOID_BUFFER, element.repeat);
            session.setPlaying(VOID_BUFFER, element.playing);
            session.setOwnerId(VOID_BUFFER, element.owner);
            session.lastSharedPosition = element.lastPositionUpdate;
            session.lastSharedPositionTime = element.lastUpdateTime;
            session.recalculatePosition(VOID_BUFFER, Instant.now());
            session.setLibraryFilters(VOID_BUFFER, PlaybackSessionUpdateMessage.asFilterObject(element.filters, session));
            if(!Main.songListWidget.dataHeaders.isEmpty()) {
                session.setSortingHeader(VOID_BUFFER, TrackDataHeader.getById(element.sortingHeader));
            }
            session.setSortingOrder(VOID_BUFFER, element.sortingOrder);
        }
        session.acknowledgedByServer = true;
    }

    //@Override
    public static Message fromJson(JsonObject json) throws MessageException {
        JsonArray arr = json.get("sessions").getAsJsonArray();
        List<PlaybackSessionElement> elements = new ArrayList<>();
        for(JsonElement jsonElement : arr) {
            JsonObject obj = jsonElement.getAsJsonObject();

            JsonElement idElem = obj.get("id");
            int id = idElem == null ? -1 : idElem.getAsInt();

            JsonElement trackElem = obj.get("track");
            String track = trackElem == null ? null : trackElem.getAsString();

            JsonElement shuffleElem = obj.get("shuffle");
            PlaybackSession.ShuffleOption shuffle = shuffleElem == null ? null : PlaybackSession.ShuffleOption.valueOf(shuffleElem.getAsString());

            JsonElement repeatElem = obj.get("repeat");
            PlaybackSession.RepeatOption repeat = repeatElem == null ? null : PlaybackSession.RepeatOption.valueOf(repeatElem.getAsString());

            JsonElement playingElem = obj.get("playing");
            boolean playing = playingElem == null ? false : playingElem.getAsBoolean();

            JsonElement lastPositionUpdateElem = obj.get("lastPositionUpdate");
            int lastPositionUpdate = lastPositionUpdateElem == null ? 0 : lastPositionUpdateElem.getAsInt();

            JsonElement ownerElem = obj.get("owner");
            int owner = ownerElem == null ? -1 : ownerElem.getAsInt();

            JsonElement lastUpdateTimeElem = obj.get("lastUpdateTime");
            Instant lastUpdateTime = lastUpdateTimeElem == null ? null : Instant.ofEpochMilli(lastUpdateTimeElem.getAsLong());

            final List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters = obj.has("filters") ? new ArrayList<>() : null;

            if(obj.has("filters")) {
                obj.getAsJsonArray("filters").asList().stream()
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

            JsonElement sortingHeaderElem = obj.get("sortingHeader");
            int sortingHeader = sortingHeaderElem == null ? 1 : sortingHeaderElem.getAsInt();

            JsonElement sortingOrderElem = obj.get("sortingOrder");
            Order sortingOrder = sortingOrderElem == null ? Order.ASCENDING : Order.valueOf(sortingOrderElem.getAsString());

            PlaybackSessionElement element = new PlaybackSessionElement(
                    id,
                    track,
                    shuffle,
                    repeat,
                    playing,
                    lastPositionUpdate,
                    owner,
                    filters,
                    sortingHeader,
                    sortingOrder,
                    lastUpdateTime
            );
            elements.add(element);
        }
        return new PlaybackSessionListMessage(elements);
    }

    public static class PlaybackSessionElement {
        public int id;
        public String track;
        public PlaybackSession.ShuffleOption shuffle;
        public PlaybackSession.RepeatOption repeat;
        public boolean playing;
        public int lastPositionUpdate;
        public int owner;
        public Instant lastUpdateTime;
        public List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters;
        public int sortingHeader;
        public Order sortingOrder;

        public PlaybackSessionElement(
                int id,
                String track,
                PlaybackSession.ShuffleOption shuffle,
                PlaybackSession.RepeatOption repeat,
                boolean playing,
                int lastPositionUpdate,
                int owner,
                List<Pair<String, List<Pair<String, LibraryFilterOption.State>>>> filters,
                int sortingHeader,
                Order sortingOrder,
                Instant lastUpdateTime
        ) {
            this.id = id;
            this.track = track;
            this.shuffle = shuffle;
            this.repeat = repeat;
            this.playing = playing;
            this.lastPositionUpdate = lastPositionUpdate;
            this.owner = owner;
            this.filters = filters;
            this.lastUpdateTime = lastUpdateTime;
            this.sortingHeader = sortingHeader;
            this.sortingOrder = sortingOrder;
        }
    }
}

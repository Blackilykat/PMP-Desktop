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

package dev.blackilykat.messages;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Track;
import dev.blackilykat.messages.exceptions.MessageException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
            }

            if(shouldReselect) {
                PlaybackSession toSelect = null;
                int highestRanking = -1;
                for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
                    int currentRanking;
                    if(session.getOwnerId() != -1) {
                        if(session.getPlaying()) {
                            currentRanking = 5;
                        } else if(session.getCurrentTrack() != null) {
                            currentRanking = 4;
                        } else {
                            currentRanking = 3;
                        }
                    } else {
                        // this case is very unlikely to happen and is probably unwanted, but it's currently possible so it should be handled
                        if(session.getPlaying()) {
                            currentRanking = 2;
                        } else if(session.getCurrentTrack() != null) {
                            currentRanking = 1;
                        } else {
                            currentRanking = 0;
                        }
                    }
                    if(currentRanking > highestRanking) {
                        toSelect = session;
                        highestRanking = currentRanking;
                    }
                }
                assert toSelect != null;
                Audio.INSTANCE.setCurrentSession(toSelect);
            }
        } else {
            connection.send(new PlaybackSessionCreateMessage(0, null));
            Audio.INSTANCE.currentSession.setOwnerId(connection.clientId);
        }
    }

    private static void applyToPlaybackSession(PlaybackSession session, PlaybackSessionElement element) {
        assert ServerConnection.INSTANCE != null;
        boolean allowedToOverride = session.getOwnerId() == ServerConnection.oldClientId || session.getOwnerId() == -1;
        boolean hasNewerInfo = session.lastSharedPositionTime != null && session.lastSharedPositionTime.isAfter(element.lastUpdateTime);

        if(hasNewerInfo && allowedToOverride) {
            session.recalculatePosition(Instant.now());
            session.setOwnerId(ServerConnection.INSTANCE.clientId);
            PlaybackSessionUpdateMessage.doUpdate(session.id, session.getCurrentTrack().getFile().getName(), session.getShuffle(), session.getRepeat(), session.getPlaying(), session.getPosition(), ServerConnection.INSTANCE.clientId);
        } else {
            Track track = null;
            for(Track t : Library.INSTANCE.tracks) {
                if(t.getFile().getName().equals(element.track)) {
                    track = t;
                    break;
                }
            }
            PlaybackSessionUpdateMessage.messageBuffer = new PlaybackSessionUpdateMessage(-1, null, null, null, null, null, null, null);
            if(Audio.INSTANCE.currentSession == session) {
                Audio.INSTANCE.startPlaying(track, false);
            } else {
                session.setCurrentTrack(track);
            }
            session.setShuffle(element.shuffle);
            session.setRepeat(element.repeat);
            session.setPlaying(element.playing);
            session.setOwnerId(element.owner);
            session.lastSharedPosition = element.lastPositionUpdate;
            session.lastSharedPositionTime = element.lastUpdateTime;
            session.recalculatePosition(Instant.now());
            PlaybackSessionUpdateMessage.messageBuffer = null;
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

            PlaybackSessionElement element = new PlaybackSessionElement(
                    id,
                    track,
                    shuffle,
                    repeat,
                    playing,
                    lastPositionUpdate,
                    owner,
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

        public PlaybackSessionElement(int id, String track, PlaybackSession.ShuffleOption shuffle, PlaybackSession.RepeatOption repeat,
                                      boolean playing, int lastPositionUpdate, int owner, Instant lastUpdateTime) {
            this.id = id;
            this.track = track;
            this.shuffle = shuffle;
            this.repeat = repeat;
            this.playing = playing;
            this.lastPositionUpdate = lastPositionUpdate;
            this.owner = owner;
            this.lastUpdateTime = lastUpdateTime;
        }
    }
}

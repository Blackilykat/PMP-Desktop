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

import com.google.gson.JsonObject;
import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Track;
import dev.blackilykat.messages.exceptions.MessageException;

import java.time.Instant;

/**
 * Updates an existing playback session which is known by both the server and the client.
 */
public class PlaybackSessionUpdateMessage extends Message {
    public static final String MESSAGE_TYPE = "PLAYBACK_SESSION_UPDATE";
    /**
     * A buffer message where updates should be stored. Used to avoid sending multiple updates when the information
     * could be packed into a single one.
     * If this is null, sessions should immediately create their own message and send it without interacting with this.
     * If this is not null, sessions should only store the updated info in here without sending anything.
     */
    public static PlaybackSessionUpdateMessage messageBuffer = null;

    public String track;
    public PlaybackSession.ShuffleOption shuffle;
    public PlaybackSession.RepeatOption repeat;
    public Boolean playing;
    public Integer position;
    public Integer owner;
    public Instant time;
    public int sessionId;


    /**
     * @param sessionId The session to update
     * @param track The new track that's playing, null if unchanged
     * @param shuffle The new shuffle option, null if unchanged
     * @param repeat The new repeat option, null if unchanged
     * @param playing Whether it's currently playing or not, null if unchanged
     * @param position The new position, null if unchanged (This should not be sent during normal progression of a track,
     *                 but only at jumps)
     */
    public PlaybackSessionUpdateMessage(int sessionId, String track, PlaybackSession.ShuffleOption shuffle, PlaybackSession.RepeatOption repeat, Boolean playing, Integer position, Integer owner, Instant time) {
        this.sessionId = sessionId;
        this.track = track;
        this.shuffle = shuffle;
        this.repeat = repeat;
        this.playing = playing;
        this.position = position;
        this.owner = owner;
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
    }

    @Override
    public void handle(ServerConnection connection) {
        // prevent updates echoing back to the server
        messageBuffer = new PlaybackSessionUpdateMessage(-1, null, null, null, null, null, null, null);
        for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
            if(!session.acknowledgedByServer || session.id != sessionId) continue;
            if(position != null) {
                session.lastSharedPosition = position;
                session.lastSharedPositionTime = time;
                session.recalculatePosition();
            }
            if(track != null) {
                for(Track t : Library.INSTANCE.tracks) {
                    if(!t.getFile().getName().equals(track)) continue;
                    if(Audio.INSTANCE.currentSession == session) {
                        Audio.INSTANCE.startPlaying(t, false);
                    } else {
                        session.setCurrentTrack(t);
                    }
                }
            }
            if(shuffle != null) {
                session.setShuffle(shuffle);
            }
            if(repeat != null) {
                session.setRepeat(repeat);
            }
            if(playing != null) {
                session.setPlaying(playing);
            }
            if(owner != null) {
                session.setOwnerId(owner);
            }
        }
        messageBuffer = null;
    }

    //@Override
    public static PlaybackSessionUpdateMessage fromJson(JsonObject json) throws MessageException {
        return new PlaybackSessionUpdateMessage(
                json.get("sessionId").getAsInt(),
                json.has("track") ? json.get("track").getAsString() : null,
                json.has("shuffle") ? PlaybackSession.ShuffleOption.valueOf(json.get("shuffle").getAsString()) : null,
                json.has("repeat") ? PlaybackSession.RepeatOption.valueOf(json.get("repeat").getAsString()) : null,
                json.has("playing") ? json.get("playing").getAsBoolean() : null,
                json.has("position") ? json.get("position").getAsInt() : null,
                json.has("owner") ? json.get("owner").getAsInt() : null,
                json.has("time") ? Instant.ofEpochMilli(json.get("time").getAsLong()) : Instant.now()
        );
    }

    public static void doUpdate(int sessionId, String track, PlaybackSession.ShuffleOption shuffle, PlaybackSession.RepeatOption repeat, Boolean playing, Integer position, Integer owner) {
        if(messageBuffer != null) {
            messageBuffer.sessionId = sessionId;
            if(track != null) messageBuffer.track = track;
            if(shuffle != null) messageBuffer.shuffle = shuffle;
            if(repeat != null) messageBuffer.repeat = repeat;
            if(playing != null) messageBuffer.playing = playing;
            if(position != null) messageBuffer.position = position;
            if(owner != null) messageBuffer.owner = owner;
        } else {
            if(ServerConnection.INSTANCE != null) {
                ServerConnection.INSTANCE.send(new PlaybackSessionUpdateMessage(sessionId, track, shuffle, repeat, playing, position, owner, Instant.now()));
            }
        }
    }
}

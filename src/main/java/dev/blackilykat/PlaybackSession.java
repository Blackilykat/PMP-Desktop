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

import java.util.ArrayList;
import java.util.List;

public class PlaybackSession {
    private static final List<PlaybackSession> availableSessions = new ArrayList<>();
    private static final List<SessionRegisterListener> registerListeners = new ArrayList<>();

    public TrackQueueManager queueManager;
    private boolean playing;
    private int position;

    public PlaybackSession(Library library) {
        this.queueManager = new TrackQueueManager(library);
        playing = false;
        position = 0;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position, boolean isJump) {
        this.position = position;
    }

    public boolean getPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void register() {
        availableSessions.add(this);
        registerListeners.forEach(l -> l.run(this));
    }

    public static PlaybackSession[] getAvailableSessions() {
        return availableSessions.toArray(new PlaybackSession[0]);
    }

    public static void registerRegisterListener(SessionRegisterListener listener) {
        registerListeners.add(listener);
    }

    public interface SessionRegisterListener {
        void run(PlaybackSession session);
    }
}

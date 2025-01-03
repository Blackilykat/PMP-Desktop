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

package dev.blackilykat.menubar.playback;

import dev.blackilykat.Audio;
import dev.blackilykat.Library;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.TrackQueueManager;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class ChangeSessionMenu extends JMenu {
    private int counter = 0;
    public ChangeSessionMenu() {
        super("Change session");

        JMenuItem createSessionItem = new JMenuItem("Create session");
        createSessionItem.addActionListener(e -> {
            PlaybackSession session = new PlaybackSession(Library.INSTANCE);
            session.register();
        });
        this.add(createSessionItem);

        for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
            addSessionButton(session);
        }
        PlaybackSession.registerRegisterListener(session -> {
            addSessionButton(session);
        });
    }
    private void addSessionButton(PlaybackSession session) {
        JMenuItem item = new JMenuItem("Session " + ++counter);
        item.addActionListener(e -> {
            synchronized(Audio.INSTANCE.audioLock) {
                // avoid changing stuff null WHILE it's processing
                if(Audio.INSTANCE.currentSession.queueManager.currentTrack != null) {
                    Audio.INSTANCE.currentSession.queueManager.currentTrack.pcmData = null;
                }
                Audio.INSTANCE.currentSession = session;
                Audio.INSTANCE.startPlaying(session.queueManager.currentTrack, false, false);
                Audio.INSTANCE.setPlaying(session.getPlaying());

                // update icons
                TrackQueueManager mgr = Audio.INSTANCE.currentSession.queueManager;
                mgr.setShuffle(mgr.getShuffle());
                mgr.setRepeat(mgr.getRepeat());
            }
        });
        this.add(item);
    }

}

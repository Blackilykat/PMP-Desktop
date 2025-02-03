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
import dev.blackilykat.Main;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.ServerConnection;
import dev.blackilykat.Track;
import dev.blackilykat.messages.PlaybackSessionCreateMessage;
import dev.blackilykat.messages.PlaybackSessionUpdateMessage;

import java.time.Instant;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

public class ChangeSessionMenu extends JMenu {
    private int counter = 0;
    public ChangeSessionMenu() {
        super("Change session");

        JMenuItem createSessionItem = new JMenuItem("Create session");
        createSessionItem.addActionListener(e -> {
            PlaybackSession session = new PlaybackSession(Library.INSTANCE, ++counter);
            session.register();
            if(ServerConnection.INSTANCE != null) {
                ServerConnection.INSTANCE.send(new PlaybackSessionCreateMessage(counter, null));
            }
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
        JMenuItem item = new JMenuItem(getItemText(session));
        item.addActionListener(e -> {
            System.out.println("Switching to session " + session.toString());
            if(Audio.INSTANCE.currentSession.getOwnerId() == ServerConnection.INSTANCE.clientId) {
                Audio.INSTANCE.currentSession.setOwnerId(-1);
            }
            if(session.getOwnerId() == -1 && ServerConnection.INSTANCE != null) {
                session.setOwnerId(ServerConnection.INSTANCE.clientId);
            }

            PlaybackSessionUpdateMessage.messageBuffer = new PlaybackSessionUpdateMessage(0, null, null,
                    null, null, null, null, Instant.now());
            // avoid changing stuff null WHILE it's processing
            synchronized(Audio.INSTANCE.audioLock) {
                Track oldTrack = Audio.INSTANCE.currentSession.getCurrentTrack();
                if(oldTrack != null) {
                    oldTrack.pcmData = null;
                }
                if(ServerConnection.INSTANCE != null && session.getOwnerId() != ServerConnection.INSTANCE.clientId) {
                    session.recalculatePosition(Instant.now());
                }
                Audio.INSTANCE.currentSession = session;
                Audio.INSTANCE.startPlaying(session.getCurrentTrack(), false);
                Audio.INSTANCE.setPlaying(session.getPlaying());

                // update icons
                session.setShuffle(session.getShuffle());
                session.setRepeat(session.getRepeat());
            }
            PlaybackSessionUpdateMessage.messageBuffer = null;
            Main.playBarWidget.repaint();
        });
        session.registerUpdateListener(s -> {
            item.setText(getItemText(s));
            // rezise the menu accordingly
            if(ChangeSessionMenu.this.isPopupMenuVisible()) {
                ChangeSessionMenu.this.setPopupMenuVisible(false);
                ChangeSessionMenu.this.setPopupMenuVisible(true);
            }
        });
        this.add(item);
    }

    private static String getItemText(PlaybackSession session) {
        StringBuilder builder = new StringBuilder("Session ").append(session.id);
        Track currentTrack = session.getCurrentTrack();
        if(currentTrack != null) {
            builder.append(": ").append(currentTrack.title);
        }
        int owner = session.getOwnerId();
        if(owner != -1) {
            if(owner != ServerConnection.INSTANCE.clientId) {
                builder.append(" on Client ").append(owner);
            } else {
                builder.append(" on this client  (").append(owner).append(")");
            }
        }
        return builder.toString();
    }

}

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

package dev.blackilykat.widgets.playbar;

import dev.blackilykat.Audio;
import dev.blackilykat.Track;
import dev.blackilykat.TrackQueueManager;
import dev.blackilykat.util.Icons;
import dev.blackilykat.widgets.Widget;

import javax.swing.JButton;
import javax.swing.SpringLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayBarWidget extends Widget {
    private static boolean playing = false;
    public static JButton playPauseButton = new JButton(Icons.svgIcon(Icons.PLAY, 16, 16));
    public static JButton nextTrackButton = new JButton(Icons.svgIcon(Icons.FORWARD, 16, 16));
    public static JButton previousTrackButton = new JButton(Icons.svgIcon(Icons.BACKWARD, 16, 16));
    public static JButton shuffleButton = new JButton(Icons.svgIcon(Icons.SHUFFLE_OFF, 16, 16));
    public static JButton repeatButton = new JButton(Icons.svgIcon(Icons.REPEAT_OFF, 16, 16));
    public static TimeBar timeBar = new TimeBar();

    public PlayBarWidget() {
        super();

        this.add(playPauseButton);
        this.add(nextTrackButton);
        this.add(previousTrackButton);
        this.add(shuffleButton);
        this.add(repeatButton);
        this.add(timeBar);

        layout.putConstraint(SpringLayout.NORTH, playPauseButton, 10, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, playPauseButton, 0 , SpringLayout.HORIZONTAL_CENTER, this);

        layout.putConstraint(SpringLayout.WEST, nextTrackButton, 10, SpringLayout.EAST, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, nextTrackButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.WEST, repeatButton, 10, SpringLayout.EAST, nextTrackButton);
        layout.putConstraint(SpringLayout.SOUTH, repeatButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.EAST, previousTrackButton, -10, SpringLayout.WEST, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, previousTrackButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.EAST, shuffleButton, -10, SpringLayout.WEST, previousTrackButton);
        layout.putConstraint(SpringLayout.SOUTH, shuffleButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, timeBar, 0, SpringLayout.HORIZONTAL_CENTER, this);
        layout.putConstraint(SpringLayout.NORTH, timeBar, 0, SpringLayout.SOUTH, playPauseButton);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.getParent().getWidth(), 80);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 80);
    }

    public static class PlayPauseButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }

    public static boolean getPlaying() {
        return playing;
    }

    public static void setPlaying(boolean playing) {
        PlayBarWidget.playing = playing;
        if(playing) {
            playPauseButton.setIcon(Icons.svgIcon(Icons.PAUSE, 16, 16));
        } else {
            playPauseButton.setIcon(Icons.svgIcon(Icons.PLAY , 16, 16));
        }
    }

    static {
        playPauseButton.addActionListener(e -> {
            Audio.INSTANCE.setPlaying(!getPlaying());
        });
        shuffleButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.queueManager.setShuffle(switch(Audio.INSTANCE.currentSession.queueManager.getShuffle()) {
                case OFF -> TrackQueueManager.ShuffleOption.ON;
                case ON -> TrackQueueManager.ShuffleOption.OFF;
            });
        });
        repeatButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.queueManager.setRepeat(switch(Audio.INSTANCE.currentSession.queueManager.getRepeat()) {
                case OFF -> TrackQueueManager.RepeatOption.ALL;
                case ALL -> TrackQueueManager.RepeatOption.TRACK;
                case TRACK -> TrackQueueManager.RepeatOption.OFF;
            });
        });
        nextTrackButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.queueManager.nextTrack();
            Track currentTrack = Audio.INSTANCE.currentSession.queueManager.getCurrentTrack();
            if(currentTrack != null) {
                Audio.INSTANCE.startPlaying(currentTrack, true, true);
            }
        });
        previousTrackButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.queueManager.previousTrack();
            Track currentTrack = Audio.INSTANCE.currentSession.queueManager.getCurrentTrack();
            if(currentTrack != null) {
                Audio.INSTANCE.startPlaying(currentTrack, true, true);
            }
        });
    }
}

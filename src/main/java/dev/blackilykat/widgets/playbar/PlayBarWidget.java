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

package dev.blackilykat.widgets.playbar;

import dev.blackilykat.Audio;
import dev.blackilykat.PlaybackSession;
import dev.blackilykat.Track;
import dev.blackilykat.util.Icons;
import dev.blackilykat.widgets.Widget;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PlayBarWidget extends Widget {
    private static boolean playing = false;
    private static final int ICON_SIZE = 16;
    public static JButton playPauseButton = new JButton(Icons.svgIcon(Icons.PLAY, ICON_SIZE, ICON_SIZE));
    public static JButton nextTrackButton = new JButton(Icons.svgIcon(Icons.FORWARD, ICON_SIZE, ICON_SIZE));
    public static JButton previousTrackButton = new JButton(Icons.svgIcon(Icons.BACKWARD, ICON_SIZE, ICON_SIZE));
    public static JButton shuffleButton = new JButton(Icons.svgIcon(Icons.SHUFFLE_OFF, ICON_SIZE, ICON_SIZE));
    public static JButton repeatButton = new JButton(Icons.svgIcon(Icons.REPEAT_OFF, ICON_SIZE, ICON_SIZE));
    public static TimeBar timeBar = new TimeBar();
    public static TrackLabel trackLabel = new TrackLabel(Audio.INSTANCE);

    public PlayBarWidget() {
        super();

        this.add(playPauseButton);
        this.add(nextTrackButton);
        this.add(previousTrackButton);
        this.add(shuffleButton);
        this.add(repeatButton);
        this.add(timeBar);
        this.add(trackLabel);

        layout.putConstraint(SpringLayout.NORTH, playPauseButton, 10, SpringLayout.NORTH, this);
        layout.putConstraint(SpringLayout.SOUTH, playPauseButton, 30, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, playPauseButton, 0 , SpringLayout.HORIZONTAL_CENTER, this);

        layout.putConstraint(SpringLayout.WEST, nextTrackButton, 10, SpringLayout.EAST, playPauseButton);
        layout.putConstraint(SpringLayout.NORTH, nextTrackButton, 0, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, nextTrackButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.WEST, repeatButton, 10, SpringLayout.EAST, nextTrackButton);
        layout.putConstraint(SpringLayout.NORTH, repeatButton, 0, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, repeatButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.EAST, previousTrackButton, -10, SpringLayout.WEST, playPauseButton);
        layout.putConstraint(SpringLayout.NORTH, previousTrackButton, 0, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, previousTrackButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.EAST, shuffleButton, -10, SpringLayout.WEST, previousTrackButton);
        layout.putConstraint(SpringLayout.NORTH, shuffleButton, 0, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, shuffleButton, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, timeBar, 0, SpringLayout.HORIZONTAL_CENTER, this);
        layout.putConstraint(SpringLayout.NORTH, timeBar, 0, SpringLayout.SOUTH, playPauseButton);

        layout.putConstraint(SpringLayout.WEST, trackLabel, 10, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.EAST, trackLabel, -10, SpringLayout.WEST, shuffleButton);
        layout.putConstraint(SpringLayout.NORTH, trackLabel, 0, SpringLayout.NORTH, playPauseButton);
        layout.putConstraint(SpringLayout.SOUTH, trackLabel, 0, SpringLayout.SOUTH, playPauseButton);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(this.getParent().getWidth(), 90);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(300, 90);
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
            playPauseButton.setIcon(Icons.svgIcon(Icons.PAUSE, ICON_SIZE, ICON_SIZE));
        } else {
            playPauseButton.setIcon(Icons.svgIcon(Icons.PLAY , ICON_SIZE, ICON_SIZE));
        }
    }

    static {
        playPauseButton.addActionListener(e -> {
            Audio.INSTANCE.setPlaying(null, !getPlaying());
        });
        shuffleButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.setShuffle(null, switch(Audio.INSTANCE.currentSession.getShuffle()) {
                case OFF -> PlaybackSession.ShuffleOption.ON;
                case ON -> PlaybackSession.ShuffleOption.OFF;
            });
        });
        repeatButton.addActionListener(e -> {
            Audio.INSTANCE.currentSession.setRepeat(null, switch(Audio.INSTANCE.currentSession.getRepeat()) {
                case OFF -> PlaybackSession.RepeatOption.ALL;
                case ALL -> PlaybackSession.RepeatOption.TRACK;
                case TRACK -> PlaybackSession.RepeatOption.OFF;
            });
        });
        nextTrackButton.addActionListener(e -> {
            Audio.INSTANCE.nextTrack();
        });
        previousTrackButton.addActionListener(e -> {
            Audio.INSTANCE.previousTrack();
        });
    }
}

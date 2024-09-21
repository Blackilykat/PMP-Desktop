package dev.blackilykat.widgets.playbar;

import dev.blackilykat.Audio;
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
    public static JButton shuffleButton = new JButton(Icons.svgIcon(Icons.SHUFFLE, 16, 16));
    public static JButton repeatButton = new JButton(Icons.svgIcon(Icons.REPEAT, 16, 16));
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
            Audio.INSTANCE.setPlaying(!getPlaying());
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
        playPauseButton.addActionListener(new PlayPauseButtonListener());
    }
}

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

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.time.Instant;

public class TimeBar extends JSlider {
    private static final int FRAMERATE_CAP = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode().getRefreshRate();
    private static final long FRAMETIME_CAP_MS = (long) ((1.0 / FRAMERATE_CAP) * 1000);
    private Instant lastRepainted = null;

    @Override
    public int getValue() {
        return Audio.INSTANCE.currentSession.getPosition();
    }

    @Override
    public void setValue(int n) {
        Audio.INSTANCE.currentSession.setPosition(n - (n % Audio.INSTANCE.audioFormat.getFrameSize()), true);
        System.out.println("Changed to " + (n % 4)  + " (" + n  + ")");
    }

    @Override
    public int getMinimum() {
        return 0;
    }

    @Override
    public int getMaximum() {
        Track currentTrack = Audio.INSTANCE.currentSession.getCurrentTrack();
        return currentTrack == null ? 0 : currentTrack.pcmData.length;
    }

    public void update() {
        Instant now = Instant.now();
        if(lastRepainted == null || lastRepainted.plusMillis(FRAMETIME_CAP_MS).isBefore(now)) {
            repaint();
            lastRepainted = now;
        }
    }

    @Override
    public void updateUI() {

        TimeBarUi ui = new TimeBarUi();
        setUI(ui);

        updateLabelUIs();
    }

    @Override
    public Dimension getPreferredSize() {
        int parentWidth = getParent().getWidth();
        return new Dimension(parentWidth-20, 40);
    }

    public static class TimeBarUi extends BasicSliderUI {
        @Override
        protected BasicSliderUI.TrackListener createTrackListener(JSlider slider) {
            return new TrackListener();
        }

        private static int mouseX = 0;
        private static int mouseY = 0;
        private static boolean mouseDown = false;

        public static final int TICK_WIDTH = 10;

        @Override
        protected void calculateTrackRect() {
            trackRect.width = contentRect.width - 120;
            trackRect.height = 2;
            trackRect.x = 60;
            trackRect.y = contentRect.y + (contentRect.height / 2);
        }

        // no
        @Override
        public void paintThumb(Graphics g) {}

        @Override
        public void paintTrack(Graphics g) {
            super.paintTrack(g);
            Track currentTrack = Audio.INSTANCE.currentSession.getCurrentTrack();
            double percent;
            if(currentTrack == null || currentTrack.pcmData == null) {
                percent = 0;
            } else {
                percent = ((double) Audio.INSTANCE.currentSession.getPosition()) / currentTrack.pcmData.length;
            }
            int offset = (int) (percent * trackRect.width);

            Color color = new Color(0x000000);
            if(!mouseDown) g.setColor(color);
            else g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0x77));
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintTick(g, offset);

            if(mouseDown) {
                g.setColor(color);
                paintTick(g, Math.max(0, Math.min((int) trackRect.getWidth(), mouseX - (int) trackRect.getX())));
            }

            /*
            Not sure why, but sometimes loaded doesn't reach the length. I suspect it's some floating point precision
            error while calculating the song length from the FLAC StreamInfo header. Everything plays fine and audio
            is able to read everything.
             */
            if(currentTrack != null && currentTrack.pcmData != null && (double) currentTrack.loaded / currentTrack.pcmData.length < 0.99) {
                g.setColor(new Color(255, 0, 0, 50));
                percent = ((double) currentTrack.loaded) / currentTrack.pcmData.length;
                percent = Math.min(1, Math.max(0, percent));
                offset = (int) (percent * trackRect.width);
                paintTick(g, offset);
            }


            // idea complains about integer division in floating point context but these are all gonna be integer numbers
            int currentTimeSeconds = (int) (Audio.INSTANCE.currentSession.getPosition() / Audio.INSTANCE.audioFormat.getFrameSize() / Audio.INSTANCE.audioFormat.getSampleRate());
            // hope no one needs hour marks ()
            int currentTimeMinutes = currentTimeSeconds / 60;
            currentTimeSeconds %= 60;

            int totalTimeSeconds;
            if(currentTrack == null || currentTrack.pcmData == null) {
                totalTimeSeconds = 0;
            } else  {
                totalTimeSeconds = (int) (currentTrack.pcmData.length / Audio.INSTANCE.audioFormat.getFrameSize() / Audio.INSTANCE.audioFormat.getSampleRate());
            }
            // hope no one needs hour marks ()
            int totalTimeMinutes = totalTimeSeconds / 60;
            totalTimeSeconds %= 60;

            String passed = String.format("%d:%02d", currentTimeMinutes, currentTimeSeconds);
            String total = String.format("%d:%02d", totalTimeMinutes, totalTimeSeconds);

            g.setColor(new Color(0x000000));
            g.setFont(new Font("Arial", 0, 20));
            FontMetrics fontMetrics = g.getFontMetrics();
            g.drawString(passed, 0, getStringHeightCentered(fontMetrics, slider.getHeight()/2));
            g.drawString(total, slider.getWidth() - fontMetrics.stringWidth(total), getStringHeightCentered(fontMetrics, slider.getHeight()/2));
        }

        private int getStringHeightCentered(FontMetrics metrics, int height) {
            return ((height - metrics.getHeight()/2)/2) + metrics.getAscent();
        }

        public void paintTick(Graphics g, int offset) {
            g.fillRoundRect((int) trackRect.getX() + offset - (TICK_WIDTH / 2), 0, TICK_WIDTH, slider.getHeight()-1, TICK_WIDTH, TICK_WIDTH);
        }


        public class TrackListener extends BasicSliderUI.TrackListener {

            @Override
            public void mousePressed(MouseEvent e) {
                mouseDown = true;
                ((TimeBar)slider).update();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDown = false;
                Track currentTrack = Audio.INSTANCE.currentSession.getCurrentTrack();

                if(currentTrack == null) return;

                double pos = e.getX() - trackRect.getX();
                double percent = pos / trackRect.width;
                percent = Math.min(1, Math.max(0, percent));
                int songPosition = ((int) (percent * currentTrack.pcmData.length));
                songPosition -= songPosition % Audio.INSTANCE.audioFormat.getFrameSize();
                Audio.INSTANCE.currentSession.setPosition(songPosition, true);
                ((TimeBar)slider).update();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateMousePos(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                updateMousePos(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateMousePos(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateMousePos(e);
                ((TimeBar)slider).update();
            }

            private void updateMousePos(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        }
    }
}
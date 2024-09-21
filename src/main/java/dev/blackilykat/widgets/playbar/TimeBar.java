package dev.blackilykat.widgets.playbar;

import dev.blackilykat.Audio;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;

public class TimeBar extends JSlider {
    @Override
    public int getValue() {
        return Audio.INSTANCE.getPosition();
    }

    @Override
    public void setValue(int n) {
        Audio.INSTANCE.setPosition(n - (n % Audio.INSTANCE.audioFormat.getFrameSize()));
        System.out.println("Changed to " + (n % 4)  + " (" + n  + ")");
    }

    @Override
    public int getMinimum() {
        return 0;
    }

    @Override
    public int getMaximum() {
        return Audio.INSTANCE.song.length;
    }

    public void update() {
        repaint();
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
            double percent = ((double) Audio.INSTANCE.getPosition()) / Audio.INSTANCE.song.length;
            if(Audio.INSTANCE.song.length == 0) percent = 0;
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
            if((double) Audio.INSTANCE.loaded / Audio.INSTANCE.song.length < 0.99) {
                g.setColor(new Color(255, 0, 0, 50));
                percent = ((double) Audio.INSTANCE.loaded) / Audio.INSTANCE.song.length;
                percent = Math.min(1, Math.max(0, percent));
                offset = (int) (percent * trackRect.width);
                paintTick(g, offset);
            }


            // idea complains about integer division in floating point context but these are all gonna be integer numbers
            int currentTimeSeconds = (int) (Audio.INSTANCE.getPosition() / Audio.INSTANCE.audioFormat.getFrameSize() / Audio.INSTANCE.audioFormat.getSampleRate());
            // hope no one needs hour marks ()
            int currentTimeMinutes = currentTimeSeconds / 60;
            currentTimeSeconds %= 60;

            int totalTimeSeconds = (int) (Audio.INSTANCE.song.length / Audio.INSTANCE.audioFormat.getFrameSize() / Audio.INSTANCE.audioFormat.getSampleRate());
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
                double pos = e.getX() - trackRect.getX();
                double percent = pos / trackRect.width;
                System.out.println("Position percent: " + percent);
                percent = Math.min(1, Math.max(0, percent));
                int songPosition = ((int) (percent * Audio.INSTANCE.song.length));
                System.out.println("Position before edit: " + songPosition);
                songPosition -= songPosition % Audio.INSTANCE.audioFormat.getFrameSize();
                System.out.println("Position after edit: " + songPosition);
                Audio.INSTANCE.setPosition(songPosition);
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
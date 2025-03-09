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
import dev.blackilykat.util.Pair;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TrackLabel extends JPanel {
    private static final String ARTIST_SEPARATOR = ", ";
    private static final String ARTIST_CUTOFF = "...";
    /**
     * Diameter of the dot that separates the title from the artists (in px)
     */
    private static final int DOT_SIZE = 4;
    /**
     * Spacing before and after the dot that separates the title from the artists (in px)
     */
    private static final int DOT_SPACING = 7;

    private Track track = null;
    public final Audio audio;
    public String title = "";
    public List<String> artists = new ArrayList<>();

    public TrackLabel(Audio audio) {
        this.audio = audio;
        PlaybackSession.SessionListener listener = session -> {
            if(audio.currentSession != session) return;
            this.setTrack(session.getCurrentTrack());
            this.repaint();
        };
        for(PlaybackSession session : PlaybackSession.getAvailableSessions()) {
            session.registerUpdateListener(listener);
        }
        PlaybackSession.registerRegisterListener(session -> {
            session.registerUpdateListener(listener);
        });
    }

    @Override
    public void paint(Graphics g) {
        if(track == null) return;
        ((Graphics2D) g).setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int width = this.getWidth();
        int height = this.getHeight();

        g.setColor(this.getBackground());
        g.fillRect(0, 0, width, height);

        g.setColor(Color.BLACK);
        Font font = new Font("Arial", Font.BOLD, 18);
        g.setFont(font);

        int x = 0;
        FontMetrics fm = g.getFontMetrics();
        int fontHeight = fm.getHeight();
        int y = height - (fontHeight / 4);

        g.drawString(title, x, y);
        x += fm.stringWidth(title);

        int dotPos = x + DOT_SPACING;
        x += DOT_SPACING * 2;

        g.setFont(new Font("Arial", Font.PLAIN, 20));
        fm = g.getFontMetrics();
        fontHeight = fm.getHeight();
        y = height - (fontHeight / 4);

        boolean paintedAnyArtists = false;
        for(Iterator<String> iterator = artists.iterator(); iterator.hasNext(); ) {
            String artist = iterator.next();
            int artistWidth = fm.stringWidth(artist);
            int lookAheadPos = x + artistWidth;
            if(iterator.hasNext()) lookAheadPos += fm.stringWidth(ARTIST_SEPARATOR) + fm.stringWidth(ARTIST_CUTOFF);

            if(lookAheadPos < width) {
                g.drawString(artist, x, y);
                x += artistWidth;
                paintedAnyArtists = true;
            } else {
                if(paintedAnyArtists) {
                    g.drawString(ARTIST_CUTOFF, x, y);
                }
                break;
            }

            if(iterator.hasNext()) {
                g.drawString(ARTIST_SEPARATOR, x, y);
                x += fm.stringWidth(ARTIST_SEPARATOR);
            }
        }
        if(paintedAnyArtists) {
            g.fillOval(dotPos - DOT_SIZE / 2, y - fontHeight / 3 - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
        }
    }

    public void setTrack(Track track) {
        this.track = track;
        artists.clear();

        if(track == null) return;
        for(Pair<String, String> metadatum : track.metadata) {
            if(metadatum.key.equals("title")) {
                this.title = metadatum.value;
                continue;
            }
            if(metadatum.key.equals("artist")) {
                artists.add(metadatum.value);
            }
        }
    }
}

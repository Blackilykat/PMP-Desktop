package dev.blackilykat.widgets.tracklist;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class TrackDataHeaderContainer extends JPanel implements MouseListener, MouseMotionListener {
    public SongListWidget songListWidget;

    public TrackDataHeaderContainer(SongListWidget songListWidget) {
        this.songListWidget = songListWidget;
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.GRAY);
        int pos = 0;
        for(TrackDataHeader header : songListWidget.dataHeaders) {
            pos += header.width;
            g.fillRect(pos - 1, 10, 2, this.getHeight() - 20);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
        if(songListWidget.draggedHeader == null) return;
        songListWidget.draggedHeader.width = Math.max(e.getX() - songListWidget.draggedHeader.containedComponent.getX(), 20);

        songListWidget.draggedHeader = null;
        songListWidget.dragResizeLine = -1;
        songListWidget.repaint();
        songListWidget.refresh();
        songListWidget.revalidate();
        songListWidget.scrollPaneContents.revalidate();
        for(Component child : songListWidget.scrollPaneContents.getComponents()) {
            if(!(child instanceof TrackPanel trackPanel)) continue;
            for(Component grandchild : trackPanel.getComponents()) {
                if(!(grandchild instanceof TrackDataEntryWidget widget)) continue;
                grandchild.revalidate();
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(songListWidget.draggedHeader == null) return;

        int actualX = e.getX() - songListWidget.draggedHeader.containedComponent.getX();
        int oldPos = songListWidget.dragResizeLine;
        if(actualX < 20) {
            // can safely assume other headers' containedComponents are non-null since the user is dragging on this one
            songListWidget.dragResizeLine = songListWidget.draggedHeader.containedComponent.getX() + 20;
        } else {
            songListWidget.dragResizeLine = e.getX();
        }
        // repainting everything uses an unreasonable amount of gpu
        songListWidget.repaint(oldPos, 0, 1, songListWidget.getHeight());
        songListWidget.repaint(songListWidget.dragResizeLine, 0, 1, songListWidget.getHeight());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        int totalWidth = 0;
        for(TrackDataHeader dataHeader : songListWidget.dataHeaders) {
            totalWidth += dataHeader.width;
        }

        if(e.getX() < totalWidth + 20) {
            songListWidget.draggedHeader = songListWidget.dataHeaders.getLast();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}
}

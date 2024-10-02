package dev.blackilykat.widgets;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Utility panel that will always have its preferred size as:<br />
 * Width: width of the parent<br />
 * Height: sum of the children's heights<br />
 * Designed to be used with {@link javax.swing.JScrollPane}
 */
public class ScrollablePanel extends JPanel {

    public ScrollablePanel() {
        super();
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Dimension getPreferredSize() {
        int height = 0;
        for (Component component : this.getComponents()) {
            height += component.getHeight();
        }
        return new Dimension(this.getParent().getWidth(), height);
    }
}

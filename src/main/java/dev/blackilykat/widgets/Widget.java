package dev.blackilykat.widgets;

import javax.swing.JPanel;
import javax.swing.SpringLayout;

public class Widget extends JPanel {
    public SpringLayout layout = new SpringLayout();
    public Widget() {
        this.setLayout(layout);
    }
}

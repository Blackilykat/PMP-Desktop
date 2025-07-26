package dev.blackilykat.menubar.debug;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import static dev.blackilykat.Main.LOGGER;

public class EnterLogMenuItem extends JMenuItem {
    public EnterLogMenuItem() {
        super("Enter log...");
        this.addActionListener(e -> {
            String res = JOptionPane.showInputDialog("Enter the log...");
            LOGGER.warn("USER-ENTERED LOG: {}", res);
        });
    }
}

package dev.blackilykat.menubar.connection;

import dev.blackilykat.ServerConnection;

import javax.swing.JMenuItem;

public class DisconnectFromServerMenuItem extends JMenuItem {
    public DisconnectFromServerMenuItem() {
        super("Disconnect from server");
        this.addActionListener(e -> {
            if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                ServerConnection.INSTANCE.disconnect();
            }
        });
    }
}

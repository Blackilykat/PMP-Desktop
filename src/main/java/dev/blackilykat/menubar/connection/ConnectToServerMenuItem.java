package dev.blackilykat.menubar.connection;

import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;

import javax.swing.JMenuItem;
import java.io.IOException;

public class ConnectToServerMenuItem extends JMenuItem {
    public ConnectToServerMenuItem() {
        super("Connect to server");
        this.addActionListener(e -> {
            if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                System.out.println("Already connected to server, aborting...");
                return;
            }
            String ip = Storage.getServerIp();
            int mainPort = Storage.getServerMainPort();
            int filePort = Storage.getServerFilePort();
            try {
                ServerConnection.INSTANCE = new ServerConnection(ip, mainPort, filePort);
                ServerConnection.INSTANCE.start();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        });
    }
}

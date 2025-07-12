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

package dev.blackilykat.menubar.connection;

import dev.blackilykat.ServerConnection;
import dev.blackilykat.Storage;

import javax.swing.JMenuItem;
import java.io.IOException;

import static dev.blackilykat.Main.LOGGER;

public class ConnectToServerMenuItem extends JMenuItem {
    public ConnectToServerMenuItem() {
        super("Connect to server");
        this.setEnabled(ServerConnection.INSTANCE == null || !ServerConnection.INSTANCE.connected);
        this.addActionListener(e -> {
            if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                LOGGER.warn("Already connected to server, aborting...");
                return;
            }
            String ip = Storage.getServerIp();
            int mainPort = Storage.getServerMainPort();
            int filePort = Storage.getServerFilePort();
            try {
                ServerConnection.INSTANCE = new ServerConnection(ip, mainPort, filePort);
            } catch(IOException ex) {
                LOGGER.warn("Could not connect to server", ex);
            }
        });
        ServerConnection.addConnectListener(connection -> {
            ConnectToServerMenuItem.this.setEnabled(false);
        });
        ServerConnection.addDisconnectListener(connection -> {
            ConnectToServerMenuItem.this.setEnabled(true);
        });
    }
}

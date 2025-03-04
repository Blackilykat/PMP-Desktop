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

import javax.swing.JMenuItem;

public class DisconnectFromServerMenuItem extends JMenuItem {
    public DisconnectFromServerMenuItem() {
        super("Disconnect from server");
        this.setEnabled(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected);
        this.addActionListener(e -> {
            if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                ServerConnection.INSTANCE.disconnect(-1);
            }
        });
        ServerConnection.addConnectListener(connection -> {
            DisconnectFromServerMenuItem.this.setEnabled(true);
        });
        ServerConnection.addDisconnectListener(connection -> {
            DisconnectFromServerMenuItem.this.setEnabled(false);
        });
    }
}

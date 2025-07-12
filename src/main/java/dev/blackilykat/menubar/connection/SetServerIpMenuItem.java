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
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.io.IOException;

import static dev.blackilykat.Main.LOGGER;

public class SetServerIpMenuItem extends JMenuItem {

    public SetServerIpMenuItem() {
        super("Set server IP");
        this.addActionListener(e -> {
            JTextField ipField = new JTextField("localhost");
            JTextField mainPortField = new JTextField("5000");
            JTextField filePortField = new JTextField("5001");
            while(true) {
                int response = JOptionPane.showOptionDialog(
                        null,
                        new Object[]{
                                "Server IP: ",
                                ipField,
                                "Main port: ",
                                mainPortField,
                                "File port: ",
                                filePortField
                        },
                        "Set server IP",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        null
                );
                if(response == JOptionPane.CANCEL_OPTION) return;
                try {
                    int mainPort = Integer.parseInt(mainPortField.getText());
                    int filePort = Integer.parseInt(filePortField.getText());
                    String ip = ipField.getText();
                    LOGGER.info("Set server ip to {}:{} and {}:{}", ip, mainPort, ip, filePort);

                    Storage.setServerPublicKey(null);
                    Storage.setServerIp(ip);
                    Storage.setServerMainPort(mainPort);
                    Storage.setServerFilePort(filePort);

                    if(ServerConnection.RETRY_TASK != null) {
                        ServerConnection.RETRY_TASK.cancel();
                    }

                    if(ServerConnection.INSTANCE != null && ServerConnection.INSTANCE.connected) {
                        ServerConnection.INSTANCE.disconnect(-1);
                    }
                    ServerConnection.INSTANCE = new ServerConnection(ip, mainPort, filePort);
                } catch(NumberFormatException f) {
                    continue;
                } catch(IOException ex) {
                    LOGGER.warn("Could not connect to server", ex);
                    break;
                }
                break;
            }
        });
    }
}

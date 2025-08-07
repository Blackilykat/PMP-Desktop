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

package dev.blackilykat;

import dev.blackilykat.messages.LibraryActionMessage;

import java.io.IOException;

import static dev.blackilykat.Main.LOGGER;

public class LibraryActionSenderThread extends Thread {
    private static boolean allowed = false;
    private static final Object allowedLock = new Object();

    public static void setAllowed(boolean value) {
        allowed = value;
        synchronized(allowedLock) {
            allowedLock.notifyAll();
        }
    }

    public LibraryActionSenderThread() {
        super("Library action sender thread");
        ServerConnection.addConnectListener(conn -> {
            // wait for library hashes
            setAllowed(false);
        });
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        LOGGER.info("Starting library action sender thread");
        try {
            while(true) {
                synchronized(ServerConnection.loggedInLock) {
                    while(ServerConnection.INSTANCE == null || !ServerConnection.INSTANCE.loggedIn) {
                        ServerConnection.loggedInLock.wait();
                    }
                }
                synchronized(allowedLock) {
                    if(!allowed) {
                        while(!allowed) {
                            allowedLock.wait();
                        }
                        continue;
                    }
                }
                LOGGER.debug("Library action sender thread ready to send an action");
                LibraryAction pendingAction = Storage.blockingPeekPendingLibraryAction();
                if(!allowed) continue;

                try {
                    if(pendingAction.actionType == LibraryAction.Type.ADD || pendingAction.actionType == LibraryAction.Type.REPLACE) {
                        LibraryActionMessage msg = pendingAction.toMessage();

                        ServerConnection.INSTANCE.send(msg);
                        synchronized(msg) {
                            try {
                                msg.wait();
                            } catch(InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        ServerConnection.INSTANCE.uploadTrack(pendingAction.fileName, msg.actionId, pendingAction.actionType == LibraryAction.Type.REPLACE);
                    } else {
                        ServerConnection.INSTANCE.send(pendingAction.toMessage());
                    }
                } catch(IOException e) {
                    LOGGER.warn("Failed to send an action");
                    continue;
                }

                Storage.popPendingLibraryAction();
            }
        } catch(InterruptedException e) {
            LOGGER.warn("Library action sender thread interrupted");
        } catch(Exception e) {
            LOGGER.error("Unknown exception", e);
        }
    }
}

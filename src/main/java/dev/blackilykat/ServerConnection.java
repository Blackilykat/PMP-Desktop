/*
 * PMP-Desktop - A desktop client for Personal Music Platform, a
 * self-hosted platform to play music and make sure everything is
 * always synced across devices.
 * Copyright (C) 2024 Blackilykat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.blackilykat;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.blackilykat.messages.*;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.messages.exceptions.MessageInvalidContentsException;
import dev.blackilykat.messages.exceptions.MessageMissingContentsException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConnection {
    public static ServerConnection INSTANCE;
    public Socket socket;
    public InputStream inputStream;
    public OutputStream outputStream;
    public StringBuffer inputBuffer = new StringBuffer();
    private int messageIdCounter = 0;
    public BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    public int clientId = -1;
    public boolean connected = false;
    public MessageSendingThread messageSendingThread = new MessageSendingThread();
    public InputReadingThread inputReadingThread = new InputReadingThread();

    public ServerConnection(InetAddress address, int port) throws IOException {
        this.socket = new Socket(address, port);
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    public void start() {
        messageSendingThread.start();
        inputReadingThread.start();
    }

    public void disconnect() {
        System.out.println("Disconnecting from server!");
        connected = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public void send(Message message) {
        messageQueue.add(message);
    }

    /**
     * Downloads a track from the server
     * @param name the name of the track's file
     */
    public void downloadTrack(String name) {
        try {
            System.out.println("Attempting to download track " + name + "...");
            File destination = new File(Storage.LIBRARY, name);
            URL url = new URL("http://localhost:5001/" + name);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            InputStream connectionInputStream = connection.getInputStream();
            Files.copy(connectionInputStream, destination.toPath());
            if(connection.getResponseCode() == 200) {
                System.out.println("Successfully downloaded track " + name);
            } else {
                System.err.printf("Unexpected response code %d!", connection.getResponseCode());
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Uploads a track to the server
     * @param name the name of the track's file
     * @param replace if this is supposed to replace an existing file. In practice, switches between POST and PUT http
     *                request methods.
     */
    public void uploadTrack(String name, int actionId, boolean replace) {
        try {
            System.out.println("Attempting to upload track " + name + "...");
            File source = new File(Storage.LIBRARY, name);
            URL url = new URI("http://localhost:5001/" + name + "?action_id=" + actionId + "&client_id=" + clientId).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod(replace ? "PUT" : "POST");
            OutputStream connectionOutputStream = connection.getOutputStream();
            Files.copy(source.toPath(), connectionOutputStream);
            connectionOutputStream.close();
            if(connection.getResponseCode() == 200) {
                System.out.println("Successfully uploaded track " + name);
            } else {
                System.out.println("Unexpected response code " + connection.getResponseCode());
            }
        } catch(IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendAddTrack(String name) {
        LibraryActionMessage action = LibraryActionMessage.create(LibraryActionMessage.Type.ADD, name);
        this.send(action);
        synchronized(action) {
            try {
                action.wait();
            } catch(InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //TODO might wanna multi thread here
        this.uploadTrack(name, action.actionId, false);
    }

    /**
     * Increases {@link #messageIdCounter}. It's a method because two threads could try to do this at the same time.
     */
    public synchronized void increaseMessageIdCounter() {
        messageIdCounter++;
    }

    public int getMessageIdCounter() {
        return messageIdCounter;
    }

    private class MessageSendingThread extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Message message = messageQueue.take();
                    outputStream.write((message.withMessageId(getMessageIdCounter()).toJson() + "\n")
                            .getBytes(StandardCharsets.UTF_8));
                    synchronized(message) {
                        message.notifyAll();
                    }
                    increaseMessageIdCounter();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException ignored) {
            } finally {
                disconnect();
            }
        }
    }

    private class InputReadingThread extends Thread {
        @Override
        public void run() {
            try {
                int read;
                while(!Thread.interrupted()) {
                    read = inputStream.read();
                    if(read == -1) break;
                    if(read != ((int) '\n')) {
                        inputBuffer.append((char) read);
                    } else if(!inputBuffer.isEmpty()) {
                        String message = inputBuffer.toString();
                        try {
                            JsonObject json = Json.fromJsonObject(message);
                            String messageType;
                            if(json.has("message_type")) {
                                messageType = json.get("message_type").getAsString();
                            } else {
                                throw new MessageMissingContentsException("Missing message_type field!");
                            }

                            Message parsedMessage = switch(messageType.toUpperCase()) {
                                case WelcomeMessage.MESSAGE_TYPE -> WelcomeMessage.fromJson(json);
                                case DisconnectMessage.MESSAGE_TYPE -> DisconnectMessage.fromJson(json);
                                case ErrorMessage.MESSAGE_TYPE -> ErrorMessage.fromJson(json);
                                case LibraryActionMessage.MESSAGE_TYPE -> LibraryActionMessage.fromJson(json);
                                case LibraryHashesMessage.MESSAGE_TYPE -> LibraryHashesMessage.fromJson(json);
                                case TestMessage.MESSAGE_TYPE -> TestMessage.fromJson(json);
                                case LibraryActionRequestMessage.MESSAGE_TYPE -> LibraryActionRequestMessage.fromJson(json);
                                default -> {
                                    throw new MessageInvalidContentsException("Unknown message_type '"+messageType+"'");
                                }
                            };
                            parsedMessage.handle(ServerConnection.this);

                            System.out.println("Received message w/ type " + parsedMessage.getMessageType());

                            increaseMessageIdCounter();
                        } catch (JsonSyntaxException | UnsupportedOperationException e) {
                            increaseMessageIdCounter();
                            System.err.printf("Invalid message format on message %d: %s", getMessageIdCounter()-1, e.getMessage());
                        } catch (MessageInvalidContentsException e) {
                            increaseMessageIdCounter();
                            System.err.printf("Invalid message contents on message %d: %s", getMessageIdCounter()-1, e.getMessage());
                        } catch (MessageMissingContentsException e) {
                            increaseMessageIdCounter();
                            System.err.printf("Missing message contents on message %d: %s", getMessageIdCounter()-1, e.getMessage());
                        } catch (MessageException ignored) {
                            //unreachable
                        }


                        inputBuffer.setLength(0);
                    }
                }
            } catch (IOException e) {

                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }
    }
}

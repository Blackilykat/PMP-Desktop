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

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.blackilykat.messages.*;
import dev.blackilykat.messages.exceptions.MessageException;
import dev.blackilykat.messages.exceptions.MessageInvalidContentsException;
import dev.blackilykat.messages.exceptions.MessageMissingContentsException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerConnection {
    public static final int DEFAULT_RECONNECT_TIMEOUT_SECONDS = 5000;
    public static final Timer RETRY_TIMER = new Timer("Server reconnect attempt timer");

    public static ServerConnection INSTANCE;
    private static List<ConnectionListener> onConnectListeners = new ArrayList<>();
    private static List<ConnectionListener> onDisconnectListeners = new ArrayList<>();
    public static int oldClientId = -1;

    public final String ip;
    public final int mainPort;
    public final int filePort;
    public SSLSocket socket;
    public InputStream inputStream;
    public OutputStream outputStream;
    private int messageIdCounter = 0;
    public BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    public int clientId = -1;
    public int deviceId = -1;
    public boolean connected = false;
    public MessageSendingThread messageSendingThread = new MessageSendingThread();
    public InputReadingThread inputReadingThread = new InputReadingThread();
    public Key serverPublicKey = null;
    public SSLContext sslContext = null;

    public ServerConnection(String ip, int mainPort, int filePort) throws IOException {
        System.out.printf("Connecting to server on %s:%d and %s:%d...\n", ip, mainPort, ip, filePort);
        this.ip = ip;
        this.mainPort = mainPort;
        this.filePort = filePort;

        serverPublicKey = Storage.getServerPublicKey();

        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    if(chain.length != 1) throw new CertificateException("Unexpected chain length, should only contain one certificate but contains " + chain.length);

                    //TODO add some way for the user to confirm the public key is correct the first time they connect
                    /// it's a safe enough assumption for now but you can never be too sure
                    if(serverPublicKey != null) {
                        if(!chain[0].getPublicKey().equals(serverPublicKey)) {
                            throw new CertificateException("Mismatching public keys!");
                        }
                    } else {
                        serverPublicKey = chain[0].getPublicKey();
                        Storage.setServerPublicKey(serverPublicKey);
                    }
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    checkClientTrusted(chain, authType);
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

            }}, SecureRandom.getInstanceStrong());
        } catch(NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }

        try {
            this.socket = (SSLSocket) sslContext.getSocketFactory().createSocket(InetAddress.getByName(ip), mainPort);
            this.inputStream = socket.getInputStream();
            this.outputStream = socket.getOutputStream();
        } catch(IOException e) {
            disconnect(DEFAULT_RECONNECT_TIMEOUT_SECONDS);
            throw e;
        }

        startSending();
        startReading();

        int deviceId = Storage.getDeviceId();
        String token = Storage.getToken();
        if(deviceId < 1 || token == null) {
            JPasswordField passwordField = new JPasswordField(16);
            JPanel panel = new JPanel();
            panel.add(new JLabel("Enter the password: "));
            panel.add(passwordField);
            int response = JOptionPane.showOptionDialog(null, panel, "Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, 0);
            if(response != 0) {
                this.disconnect(-1);
                return;
            }

            // if this fails see https://stackoverflow.com/a/7800008
            // Since this is a desktop application designed to be connected to a network the edge cases are unlikely
            String hostname = InetAddress.getLocalHost().getHostName();

            {
                char[] password = passwordField.getPassword();
                this.send(new LoginMessage(new String(password), hostname));
                Arrays.fill(password, (char) 0);
            }
        } else {
            this.send(new LoginMessage(token, deviceId));
        }

    }

    public void startReading() {
        inputReadingThread.start();
    }
    public void startSending() {
        messageSendingThread.start();
    }

    public void disconnect(long reconnectInMilliseconds) {
        System.out.println("Disconnecting from server!");
        connected = false;
        if(clientId != -1) {
            oldClientId = clientId;
        }
        messageSendingThread.interrupt();
        inputReadingThread.interrupt();
        try {
            socket.close();
        } catch (IOException | NullPointerException ignored) {}
        callDisconnectListeners(this);
        if(reconnectInMilliseconds >= 0) {
            RETRY_TIMER.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        ServerConnection.INSTANCE = new ServerConnection(ip, mainPort, filePort);
                    } catch(IOException ignored) {}
                }
            }, reconnectInMilliseconds);
        }
    }

    public void send(Message message) {
        messageQueue.add(message);
    }

    /**
     * Downloads a track from the server
     * @param name the name of the track's file
     */
    public void downloadTrack(String name) throws IOException {
        try {
            System.out.println("Attempting to download track " + name + "...");
            File destination = new File(Storage.LIBRARY, name);

            // HttpsURLConnection hangs with insecure HTTP servers
            if(supportsInsecureHTTP(ip, filePort)) {
                throw new IOException("HTTP server is insecure");
            }
            URL url = new URI("https://" + ip + ":" + filePort + "/" + URLEncoder.encode(name, StandardCharsets.UTF_8)).toURL();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            InputStream connectionInputStream = connection.getInputStream();
            Files.copy(connectionInputStream, destination.toPath());
            if(connection.getResponseCode() == 200) {
                System.out.println("Successfully downloaded track " + name);
            } else {
                System.err.printf("Unexpected response code %d!", connection.getResponseCode());
            }
        } catch(URISyntaxException e) {
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

            if(supportsInsecureHTTP(ip, filePort)) {
                throw new IOException("HTTP server is insecure");
            }
            URL url = new URI( "https://" + ip + ":" + filePort + "/" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "?action_id=" + actionId + "&client_id=" + clientId).toURL();
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setSSLSocketFactory(sslContext.getSocketFactory());
            connection.setHostnameVerifier((hostname, session) -> true);
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
        if(connected) {
            LibraryActionMessage action = LibraryActionMessage.create(LibraryAction.Type.ADD, name);
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
        } else {
            Storage.pushPendingLibraryAction(new LibraryAction(name, LibraryAction.Type.ADD));
        }
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
                    String messageStr = (message.withMessageId(getMessageIdCounter()).toJson() + "\n");
                    if(!message.getMessageType().equals(LoginMessage.MESSAGE_TYPE)) {
                        System.out.println("Sending message: " + messageStr);
                    }
                    outputStream.write(messageStr.getBytes(StandardCharsets.UTF_8));
                    synchronized(message) {
                        message.notifyAll();
                    }
                    increaseMessageIdCounter();
                }
            } catch (IOException e) {
                if(!connected) return;
                throw new RuntimeException(e);
            } catch (InterruptedException ignored) {
            } finally {
                if(connected) disconnect(DEFAULT_RECONNECT_TIMEOUT_SECONDS);
            }
        }
    }

    private class InputReadingThread extends Thread {
        @Override
        public void run() {
            Queue<Byte> inputBuffer = new ArrayDeque<>();
            try {
                int read;
                while(!Thread.interrupted()) {
                    read = inputStream.read();
                    if(read == -1) break;
                    if(read != ((int) '\n')) {
                        inputBuffer.add((byte) read);
                    } else if(!inputBuffer.isEmpty()) {
                        byte[] msg = new byte[inputBuffer.size()];
                        for(int i = 0; i < msg.length; i++) {
                            msg[i] = inputBuffer.poll();
                        }
                        String message = new String(msg, StandardCharsets.UTF_8);
                        try {
                            JsonObject json = Json.fromJsonObject(message);
                            JsonObject printedJson = json.deepCopy();
                            if(printedJson.has("token")) printedJson.addProperty("token", "REDACTED");
                            System.out.println("Received message: " + printedJson);
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
                                case PlaybackSessionCreateMessage.MESSAGE_TYPE -> PlaybackSessionCreateMessage.fromJson(json);
                                case PlaybackSessionUpdateMessage.MESSAGE_TYPE -> PlaybackSessionUpdateMessage.fromJson(json);
                                case PlaybackSessionListMessage.MESSAGE_TYPE -> PlaybackSessionListMessage.fromJson(json);
                                case DataHeaderListMessage.MESSAGE_TYPE -> DataHeaderListMessage.fromJson(json);
                                case LoginMessage.MESSAGE_TYPE -> LoginMessage.fromJson(json);
                                case LatestHeaderIdMessage.MESSAGE_TYPE -> LatestHeaderIdMessage.fromJson(json);
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
                            System.err.println("Incorrect message: " + inputBuffer.toString());
                        } catch (MessageInvalidContentsException e) {
                            increaseMessageIdCounter();
                            System.err.printf("Invalid message contents on message %d: %s", getMessageIdCounter()-1, e.getMessage());
                        } catch (MessageMissingContentsException e) {
                            increaseMessageIdCounter();
                            System.err.printf("Missing message contents on message %d: %s", getMessageIdCounter()-1, e.getMessage());
                        } catch (MessageException ignored) {
                            //unreachable
                        }
                    }
                }
            } catch (IOException e) {
                if(!connected) return;
                throw new RuntimeException(e);
            } finally {
                if(connected) disconnect(DEFAULT_RECONNECT_TIMEOUT_SECONDS);
            }
        }
    }

    /**
     * @return true if an insecure HTTP connection can be established
     */
    public static boolean supportsInsecureHTTP(String host, int port) {
        try {
            URL httpUrl = new URI("http://" + host + ":" + port).toURL();
            HttpURLConnection httpConnection = (HttpURLConnection) httpUrl.openConnection();
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();
            httpConnection.getResponseCode(); // should throw here if https
            httpConnection.disconnect();
        } catch(IOException | URISyntaxException e) {
            return false;
        }
        return true;
    }

    public static void addConnectListener(ConnectionListener listener) {
        onConnectListeners.add(listener);
    }

    public static void addDisconnectListener(ConnectionListener listener) {
        onDisconnectListeners.add(listener);
    }

    public static void callConnectListeners(ServerConnection connection) {
        onConnectListeners.forEach(l -> l.run(connection));
    }

    public static void callDisconnectListeners(ServerConnection connection) {
        onDisconnectListeners.forEach(l -> l.run(connection));
    }

    public interface ConnectionListener {
        void run(ServerConnection connection);
    }
}

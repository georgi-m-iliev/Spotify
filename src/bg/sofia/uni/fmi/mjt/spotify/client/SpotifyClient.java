package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.commons.NetworkTools;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.*;

import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.NoFreePortAvailableException;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.PlaybackFailedException;
import bg.sofia.uni.fmi.mjt.spotify.commons.logger.SpotifyLogger;
import com.google.gson.Gson;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

public class SpotifyClient {
    private static final Gson GSON = new Gson();
    private static final int BUFFER_SIZE = 1024;

    private final InetSocketAddress serverAddress;
    private int playPort;

    private SocketChannel socketChannel;
    private final StreamTransport transport;
    private final ByteBuffer buffer;
    private AccessKey accessKey;
    private boolean connected;
    private Thread playbackThread;

    public SpotifyClient(int serverPort, String serverHost, StreamTransport transport) {
        serverAddress = new InetSocketAddress(serverHost, serverPort);
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        accessKey = null;
        this.transport = transport;
    }

    public void enter() {
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String command = sc.nextLine();
                try {
                    ClientCommandType commandType = ClientCommandType.getCommandType(command.split(" ")[0]);
                    switch (commandType) {
                        case CONNECT:
                            connect();
                            break;
                        case DISCONNECT:
                            disconnect();
                            break;
                        case EXIT:
                            if (connected) {
                                disconnect();
                            }
                            System.out.println("Exiting...");
                            return;
                        default:
                            if (!connected) {
                                System.out.println("You are not connected to the server.");
                                continue;
                            }
                            processCommand(command, commandType);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid command. Please try again.");
                    SpotifyLogger.getLogger().log(
                            Level.WARNING,
                            String.format("Invalid command: %s.", command),
                            e);
                } catch (IOException e) {
                    if (e.getMessage().equals("Connection reset by peer")) {
                        System.out.println("Server has closed the connection. Try again.");
                        SpotifyLogger.getLogger().log(Level.INFO, "Server has closed the connection.", e);
                        connected = false;
                    }
                    else {
                        System.out.println("Server error. Try again");
                        SpotifyLogger.getLogger().log(Level.SEVERE, "Server error occurred.", e);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("An unexpected error occurred.");
            SpotifyLogger.getLogger().log(Level.SEVERE, "Unexpected occurred.", e);
        }
    }

    private void connect() {
        try {
            socketChannel = SocketChannel.open(serverAddress);
            System.out.println("Connected to server.");
            SpotifyLogger.getLogger().log(Level.INFO, "Successfully connected to server " + serverAddress);
            connected = true;
        } catch (IOException e) {
            System.out.println("Couldn't connect to server.");
            SpotifyLogger.getLogger().log(Level.SEVERE, "Failed to connect to server.", e);
        }
    }

    private void disconnect() {
        try {
            socketChannel.close();
            System.out.println("Disconnected from server.");
            SpotifyLogger.getLogger().log(Level.INFO, "Disconnected from server " + serverAddress);
            connected = false;
        } catch (IOException e) {
            System.out.println("Disconnect from server failed.");
            SpotifyLogger.getLogger().log(Level.SEVERE, "Failed to disconnect from server.", e);
        }
    }

    private void processCommand(String command, ClientCommandType commandType) throws IOException {
        switch (commandType) {
            case LOGOUT:
                if (accessKey == null) {
                    System.out.println("You aren't logged in.");
                } else {
                    accessKey = null;
                    System.out.println("You have been logged out.");
                }
                return;
            case LOGIN, REGISTER:
                if (accessKey != null) {
                    System.out.println("You are already logged in.");
                    return;
                }
                break;
            case PLAY:
                if (transport == StreamTransport.UDP) {
                    try {
                        playPort = NetworkTools.findFreePort();
                    } catch (NoFreePortAvailableException e) {
                        System.out.println("Playback failed. Please try again later!");
                        SpotifyLogger.getLogger().log(Level.SEVERE, "Playback failed.", e);
                        return;
                    }
                    command += String.format(" %s %s", transport, playPort);
                } else if (transport == StreamTransport.TCP) {
                    command += String.format(" %s", transport);
                }
                break;
        }
        ClientRequest request = ClientRequest.of(command, accessKey);
        sendRequest(request, socketChannel);
        if (commandType == ClientCommandType.DISCONNECT) {
            return;
        }

        CommandResponse response = getResponse(socketChannel);
        processCommandResponse(commandType, response);
    }

    private void processCommandResponse(ClientCommandType commandType, CommandResponse response) {
        if (response == null) {
            System.out.println("Server error occurred. Please try again later.");
            SpotifyLogger.getLogger().log(Level.WARNING, "Server returned an empty response.");
            return;
        }

        updateAccessKey(response);

        if (response.status().equals("ERROR")) {
            handleError(response);
            return;
        }

        switch (commandType) {
            case SHOW_PLAYLIST, SEARCH:
                printSongList(response);
                break;
            case PLAY:
                try {
                    playSong(response);
                } catch (LineUnavailableException e) {
                    System.out.println("Couldn't open audio device for playback.");
                    SpotifyLogger.getLogger().log(Level.SEVERE, "Couldn't open audio device for playback.", e);
                } catch (PlaybackFailedException e) {
                    System.out.println("Playback failed. Please try again later!");
                    SpotifyLogger.getLogger().log(Level.SEVERE, "Playback failed.", e);
                }
                System.out.println(response.message());
                break;
            case STOP:
                if (playbackThread == null) {
                    System.out.println("No song is currently playing.");
                    return;
                }
                playbackThread.interrupt();
                System.out.println("Playback stopped.");
                break;
            case TOP:
                printTopSongsList(response.data());
                break;
            default:
                System.out.println(response.message());
                break;
        }
    }

    private void updateAccessKey(CommandResponse response) {
        if (response.accessKey() != null) {
            accessKey = response.accessKey();
        }
    }

    private void handleError(CommandResponse response) {
        System.out.println(response.message());
        SpotifyLogger.getLogger().log(Level.WARNING,
                "Server responded with an error: " + response.message());
    }

    private CommandResponse getResponse(SocketChannel socketChannel) throws IOException {
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        String reply = new String(byteArray, StandardCharsets.UTF_8); // buffer drain

        return GSON.fromJson(reply, CommandResponse.class);
    }

    private void sendRequest(ClientRequest request, SocketChannel socketChannel) throws IOException {
        buffer.clear();
        buffer.put(GSON.toJson(request).getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        socketChannel.write(buffer);
    }

    private void printSongList(CommandResponse response) {
        System.out.println(response.message());
        if (response.data().isEmpty()) {
            return;
        }
        Map<Integer, Song> songs = response.data();
        String leftAlignFormat = "| %-5d| %-22s | %-35s |%n";
        System.out.format("+------+------------------------+-------------------------------------+%n");
        System.out.format("| ID   | Artist                 | Song name                           |%n");
        System.out.format("+------+------------------------+-------------------------------------+%n");
        for (Map.Entry<Integer, Song> entry : songs.entrySet()) {
            Song song = entry.getValue();
            System.out.format(leftAlignFormat, entry.getKey(), song.artist(), song.name());
        }
        System.out.format("+------+------------------------+-------------------------------------+%n");
    }

    private void playSong(CommandResponse response) throws LineUnavailableException, PlaybackFailedException {
        if (response.transport() == StreamTransport.TCP) {
            playPort = Integer.parseInt(response.status().split("-")[2]);
        }

        Runnable streamClient = switch (response.transport()) {
            case TCP -> new TCPStreamClient(serverAddress.getAddress(), playPort, response.audioFormat());
            case UDP -> new UDPStreamClient(playPort, response.audioFormat());
        };

        playbackThread = new Thread(streamClient);
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    private void printTopSongsList(Map<Integer, Song> songs) {
        String leftAlignFormat = "| %-5d| %-22s | %-35s | %-7d|%n";
        System.out.format("+------+------------------------+-------------------------------------+--------+%n");
        System.out.format("| ID   | Artist                 | Song name                           | Plays  |%n");
        System.out.format("+------+------------------------+-------------------------------------+--------|%n");
        for (Map.Entry<Integer, Song> entry : songs.entrySet()) {
            Song song = entry.getValue();
            System.out.format(leftAlignFormat, entry.getKey(), song.artist(), song.name(), song.streams());
        }
        System.out.format("+------+------------------------+-------------------------------------+--------|%n");
    }

    public static void main(String[] args) {
        new SpotifyClient(9090, "192.168.1.101", StreamTransport.TCP).enter();
    }
}

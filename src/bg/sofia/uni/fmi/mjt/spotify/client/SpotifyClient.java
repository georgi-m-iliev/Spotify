package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.commons.NetworkTools;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.ClientRequest;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.StreamTransport;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.NoFreePortAvailableException;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.PlaybackFailedException;
import com.google.gson.Gson;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Scanner;

public class SpotifyClient {
    static Gson gson = new Gson();
    private static final String serverHost = "192.168.1.101";
    private static final int serverPort = 9090;

    private final InetSocketAddress serverAddress;
    private final StreamTransport transport;
    private final ByteBuffer buffer;
    private AccessKey accessKey;
    private boolean connected;
    private Thread playbackThread;
    private int playPort;

    public SpotifyClient() {
        serverAddress = new InetSocketAddress(serverHost, serverPort);
        buffer = ByteBuffer.allocate(1024);
        accessKey = null;
        transport = StreamTransport.UDP;
    }

    public void enter() {
        connected = true;
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress);
             Scanner sc = new Scanner(System.in)) {
            System.out.println("Connected to server.");

            while (connected) {
                System.out.print("> ");
                String command = sc.nextLine();

                processCommand(command, socketChannel);
            }

        } catch (IOException e) {
            System.err.println("Client error occurred. Please restart.");
        }
    }

    private void processCommand(String command, SocketChannel socketChannel) throws IOException {
        String commandName = command.split(" ")[0];
        switch (commandName) {
            case "disconnect":
                System.out.println("Disconnected from server.");
                connected = false;
            case "logout":
                if (accessKey == null) {
                    System.out.println("You aren't logged in.");
                }
                else {
                    accessKey = null;
                    System.out.println("You have been logged out.");
                }
                return;
            case "login":
            case "register":
                if (accessKey != null) {
                    System.out.println("You are already logged in.");
                    return;
                }
                break;
            case "play":
                if (transport == StreamTransport.UDP) {
                    try {
                        playPort = NetworkTools.findFreePort();
                    } catch (NoFreePortAvailableException e) {
                        System.out.println("Playback failed. Please try again later!");
                        return;
                    }
                    command += String.format(" %s %s", transport, playPort);
                }
                else if (transport == StreamTransport.TCP) {
                    command += String.format(" %s", transport);
                }
                break;
        }
        ClientRequest request = ClientRequest.of(command, accessKey);
        sendRequest(request, socketChannel);
        if (commandName.equals("disconnect")) {
            return;
        }

        CommandResponse response = getResponse(socketChannel);
        processCommandResponse(commandName, response);
    }

    private void processCommandResponse(String command, CommandResponse response) {
        if (response == null) {
            System.out.println("Server error occurred. Please try again later.");
            return;
        }

        updateAccessKey(response);

        if (response.status().equals("ERROR")) {
            handleError(response);
            return;
        }

        switch (command) {
            case "search":
                printSearchResults(response);
                break;
            case "play":
                try {
                    playSong(response);
                } catch (LineUnavailableException e) {
                    System.out.println("Couldn't open audio device for playback.");
                } catch (PlaybackFailedException e) {
                    System.out.println("Playback failed. Please try again later!");
                }
                System.out.println(response.message());
                break;
            case "stop":
                if (playbackThread == null) {
                    System.out.println("No song is currently playing.");
                    return;
                }
                playbackThread.interrupt();
                System.out.println("Playback stopped.");
                break;
            case "top":
                printTopSongsList(response.data());
                break;
            case "show-playlist":
                printPlaylist(response);
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
    }

    private CommandResponse getResponse(SocketChannel socketChannel) throws IOException {
        buffer.clear();
        socketChannel.read(buffer);
        buffer.flip();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        String reply = new String(byteArray, StandardCharsets.UTF_8); // buffer drain

        return gson.fromJson(reply, CommandResponse.class);
    }

    private void sendRequest(ClientRequest request, SocketChannel socketChannel) throws IOException {
        buffer.clear();
        buffer.put(gson.toJson(request).getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        socketChannel.write(buffer);
    }

    private void printSongList(Map<Integer, Song> songs) {
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

    private void printSearchResults(CommandResponse response) {
        System.out.println(response.message());
        if (response.data().isEmpty()) {
            return;
        }
        printSongList(response.data());
    }

    private void printPlaylist(CommandResponse response) {
        System.out.println(response.message());
        if (response.data().isEmpty()) {
            return;
        }
        printSongList(response.data());
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
        new SpotifyClient().enter();
    }
}

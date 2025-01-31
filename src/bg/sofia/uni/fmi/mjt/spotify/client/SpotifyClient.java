package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.ClientRequest;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandResponse;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

public class SpotifyClient {
    static Gson gson = new Gson();
    private static final String serverHost = "localhost";
    private static final int serverPort = 9090;

    private final InetSocketAddress serverAddress;
    private final ByteBuffer buffer;
    private AccessKey accessKey;

    public SpotifyClient() {
        serverAddress = new InetSocketAddress(serverHost, serverPort);
        buffer = ByteBuffer.allocate(512);
        accessKey = null;
    }

    public void enter() {
        try (SocketChannel socketChannel = SocketChannel.open(serverAddress);
             Scanner sc = new Scanner(System.in)) {
            System.out.println("Connected to server.");

            while (true) {
                System.out.print("> ");

                ClientRequest request = ClientRequest.of(sc.nextLine(), accessKey);
                sendRequest(request, socketChannel);

                if (request.command().strip().startsWith("disconnect")) {
                    break;
                }

                CommandResponse response = getResponse(socketChannel);
                if (response == null) {
                    System.out.println("Server error occurred. Please try again later.");
                    continue;
                }
                updateAccessKey(response);

                if (response.status().equals("ERROR")) {
                    handleError(response);
                }
                else {
                    processCommandResponse(request.command().strip().split(" ")[0], response);
                }
            }

        } catch (IOException e) {
            System.err.println("Client error occurred. Please restart.");
            // TODO: log error
        }
    }

    private void processCommandResponse(String command, CommandResponse response) {
        switch (command) {
            case "search":
                printSearchResults(response);
                break;
            case "top":
                printSongList(response.data());
                break;
            case "show-playlist":
                printPlaylist(response);
                break;
            case "logout":
                accessKey = null;
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

    private void printSongList(List<Song> songs) {
        String leftAlignFormat = "| %-5d| %-25s | %-22s |%n";
        System.out.format("+------+---------------------------+------------------------+%n");
        System.out.format("| ID   | Song name                 | Artist                 |%n");
        System.out.format("+------+---------------------------+------------------------+%n");
        for (Song song : songs) {
            System.out.format(leftAlignFormat, song.index(), song.name(), song.artist());
        }
        System.out.format("+------+---------------------------+------------------------+%n");
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

    public static void main(String[] args) {
        new SpotifyClient().enter();
    }
}

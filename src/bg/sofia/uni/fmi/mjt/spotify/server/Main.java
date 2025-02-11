package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.SongLoadingFailureException;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws SongLoadingFailureException {
        SpotifyServer server = new SpotifyServer("localhost", 9090, Path.of("resources"));

        String command;
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.print("> ");
            command = sc.nextLine();
            switch (command) {
                case "shutdown":
                    System.out.println("Stopping server...");
                    server.stop();
                    break;
                case "start":
                    server.start();
                    System.out.println("Server started.");
                    break;
                case "exit":
                    System.out.println("Exiting...");
                    running = false;
                    break;
                default:
                    System.out.println("Unknown command");
            }
        }
        sc.close();
    }

}

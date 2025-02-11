package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.exceptions.InvalidCommandException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.SocketAddress;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {
    @Test
    void testCommandBuilder() {
        String command = "search term1 term2 term3";
        String expectedCommand = "search";
        String[] expectedArguments = new String[]{"term1", "term2", "term3"};
        AccessKey expectedAccessKey = new AccessKey("user", "token");
        InetAddress expectedOriginAddress = InetAddress.getLoopbackAddress();
        SocketAddress expectedOriginSocket = null;

        try {
            Command result = Command.builder()
                    .command(command)
                    .accessKey(expectedAccessKey)
                    .originAddress(expectedOriginAddress)
                    .originSocket(expectedOriginSocket)
                    .build();

            assertEquals(expectedCommand, result.command().getCommandName());
            assertArrayEquals(expectedArguments, result.arguments());
            assertEquals(expectedAccessKey, result.accessKey());
            assertEquals(expectedOriginAddress, result.originAddress());
            assertEquals(expectedOriginSocket, result.originSocket());
        } catch (InvalidCommandException e) {
            throw new AssertionError("Invalid command", e);
        }
    }

    @Test
    public void testCommandBuilderWithInvalidCommand() {
        String command = "nocommandlikethis arg1 arg2";
        AccessKey expectedAccessKey = new AccessKey("user", "token");
        InetAddress expectedOriginAddress = InetAddress.getLoopbackAddress();
        SocketAddress expectedOriginSocket = null;

        assertThrows(InvalidCommandException.class, () ->
                Command.builder()
                        .command(command)
                        .accessKey(expectedAccessKey)
                        .originAddress(expectedOriginAddress)
                        .originSocket(expectedOriginSocket)
                        .build()
        );
    }
}

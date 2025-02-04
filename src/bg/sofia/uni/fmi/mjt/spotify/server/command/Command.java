package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.CommandType;
import bg.sofia.uni.fmi.mjt.spotify.server.exceptions.InvalidCommandException;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public record Command(
        CommandType command,
        String[] arguments,
        AccessKey accessKey,
        InetAddress originAddress,
        SocketAddress originSocket) {

    public static CommandBuilder creator() {
        return new CommandBuilder();
    }

    public static class CommandBuilder {

        private CommandType command;
        private String[] arguments;
        private AccessKey accessKey;
        private InetAddress originAddress;
        private SocketAddress originSocket;


        public CommandBuilder command(String command) throws InvalidCommandException {
            List<String> tokens = getCommandArguments(command);
            String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);
            try {
                this.command = CommandType.fromString(tokens.getFirst());
            } catch (IllegalArgumentException e) {
                throw new InvalidCommandException("Invalid command", e);
            }
            this.arguments = args;
            return this;
        }

        public CommandBuilder accessKey(AccessKey accessKey) {
            this.accessKey = accessKey;
            return this;
        }

        public CommandBuilder originAddress(InetAddress originAddress) {
            this.originAddress = originAddress;
            return this;
        }

        public CommandBuilder originSocket(SocketAddress originSocket) {
            this.originSocket = originSocket;
            return this;
        }

        public Command build() {
            return new Command(command, arguments, accessKey, originAddress, originSocket);
        }

        private static List<String> getCommandArguments(String input) {
            List<String> tokens = new ArrayList<>();
            StringBuilder sb = new StringBuilder();

            boolean insideQuote = false;

            for (char c : input.toCharArray()) {
                if (c == '"') {
                    insideQuote = !insideQuote;
                }
                if (c == ' ' && !insideQuote) { // when space is not inside quote split
                    tokens.add(sb.toString().replace("\"", "")); // token is ready, let's add it to list
                    sb.delete(0, sb.length()); // and reset StringBuilder`s content
                } else {
                    sb.append(c); //else add character to token
                }
            }
            // let's not forget about last token that doesn't have space after it
            tokens.add(sb.toString().replace("\"", ""));

            return tokens;
        }
    }
}
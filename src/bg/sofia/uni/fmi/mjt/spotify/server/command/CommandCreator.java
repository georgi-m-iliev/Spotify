package bg.sofia.uni.fmi.mjt.spotify.server.command;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.AccessKey;
import bg.sofia.uni.fmi.mjt.spotify.server.exceptions.InvalidCommandException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class CommandCreator {
    static final Gson gson = new Gson();
    // straight out of https://stackoverflow.com/a/14656159 with small enhancement
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

    public static Command newCommand(String clientInput, AccessKey accessKey) {
        List<String> tokens = CommandCreator.getCommandArguments(clientInput);
        String[] args = tokens.subList(1, tokens.size()).toArray(new String[0]);

        try {
            return new Command(CommandType.fromString(tokens.getFirst()), args, accessKey);
        } catch (IllegalArgumentException e) {
            throw new InvalidCommandException("Invalid command", e);
        }
    }
}
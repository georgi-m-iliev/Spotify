package bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters;

import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Playlist;
import bg.sofia.uni.fmi.mjt.spotify.commons.dto.Song;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistAdapter implements JsonSerializer<Playlist>, JsonDeserializer<Playlist> {
    List<Song> songs;

    public PlaylistAdapter(List<Song> songs) {
        this.songs = songs;
    }

    @Override
    public JsonElement serialize(Playlist playlist, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", playlist.id().toString());
        jsonObject.addProperty("name", playlist.name());
        List<String> songIDs = playlist.songs().stream().map(Song::id).toList();
        JsonArray songIDsArray = new JsonArray();
        songIDs.forEach(songIDsArray::add);
        jsonObject.add("songs", songIDsArray);
        return jsonObject;
    }

    @Override
    public Playlist deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        UUID id = UUID.fromString(jsonObject.get("id").getAsString());
        String name = jsonObject.get("name").getAsString();
        JsonArray songIDsArray = jsonObject.get("songs").getAsJsonArray();
        List<String> songIDs = songIDsArray.asList().stream().map(JsonElement::getAsString).toList();

        List<Song> availableSongs = songs.stream().filter(song -> songIDs.contains(song.id())).toList();
        List<Song> playlistSongs = new ArrayList<>(availableSongs);
        return new Playlist(id, name, playlistSongs);
    }
}

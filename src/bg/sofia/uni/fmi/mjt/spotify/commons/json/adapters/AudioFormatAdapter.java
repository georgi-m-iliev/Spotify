package bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Type;

public class AudioFormatAdapter implements JsonSerializer<AudioFormat>, JsonDeserializer<AudioFormat> {
    @Override
    public AudioFormat deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new AudioFormat(
                new AudioFormat.Encoding(jsonElement.getAsJsonObject().get("encoding").getAsString()),
                jsonElement.getAsJsonObject().get("sampleRate").getAsFloat(),
                jsonElement.getAsJsonObject().get("sampleSizeInBits").getAsInt(),
                jsonElement.getAsJsonObject().get("channels").getAsInt(),
                jsonElement.getAsJsonObject().get("frameSize").getAsInt(),
                jsonElement.getAsJsonObject().get("frameRate").getAsFloat(),
                jsonElement.getAsJsonObject().get("bigEndian").getAsBoolean()
        );
    }

    @Override
    public JsonElement serialize(AudioFormat audioFormat, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("encoding", String.valueOf(audioFormat.getEncoding()));
        jsonObject.addProperty("sampleRate", audioFormat.getSampleRate());
        jsonObject.addProperty("sampleSizeInBits", audioFormat.getSampleSizeInBits());
        jsonObject.addProperty("channels", audioFormat.getChannels());
        jsonObject.addProperty("frameSize", audioFormat.getFrameSize());
        jsonObject.addProperty("frameRate", audioFormat.getFrameRate());
        jsonObject.addProperty("bigEndian", audioFormat.isBigEndian());

        return jsonObject;
    }
}

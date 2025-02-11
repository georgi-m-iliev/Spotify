package bg.sofia.uni.fmi.mjt.spotify.commons.json.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;

import static org.junit.jupiter.api.Assertions.*;

class AudioFormatAdapterTest {
    private static final  Gson gson = new GsonBuilder()
            .registerTypeAdapter(AudioFormat.class, new AudioFormatAdapter())
            .create();

    private static final String encoding = AudioFormat.Encoding.PCM_SIGNED.toString();
    private static final float sampleRate = 44100;
    private static final int sampleSizeInBits = 16;
    private static final int channels = 2;
    private static final int frameSize = 4;
    private static final float frameRate = 44100;
    private static final boolean bigEndian = false;

    @Test
    public void testDeserialize() {
        String json = String.format("""
                {
                    "encoding": "%s",
                    "sampleRate": %s,
                    "sampleSizeInBits": %s,
                    "channels": %s,
                    "frameSize": %s,
                    "frameRate": %s,
                    "bigEndian": %s
                }
                """, encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        AudioFormat actual = gson.fromJson(json, AudioFormat.class);
        assertEquals(encoding, actual.getEncoding().toString(), "Encoding is not the same");
        assertEquals(sampleRate, actual.getSampleRate(), "Sample rate is not the same");
        assertEquals(sampleSizeInBits, actual.getSampleSizeInBits(), "Sample size in bits is not the same");
        assertEquals(channels, actual.getChannels(), "Channels are not the same");
        assertEquals(frameSize, actual.getFrameSize(), "Frame size is not the same");
        assertEquals(frameRate, actual.getFrameRate(), "Frame rate is not the same");
        assertEquals(bigEndian, actual.isBigEndian(), "Big endian is not the same");
    }

    @Test
    public void testSerialize() {
        AudioFormat format = new AudioFormat(
            new AudioFormat.Encoding(encoding),
            sampleRate,
            sampleSizeInBits,
            channels,
            frameSize,
            frameRate,
            bigEndian
        );

        String expected = String.format("""
                    {
                        "encoding": "%s",
                        "sampleRate": %s,
                        "sampleSizeInBits": %s,
                        "channels": %s,
                        "frameSize": %s,
                        "frameRate": %s,
                        "bigEndian": %s
                    }
                    """, encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        String actual = gson.toJson(format);
        assertEquals(expected.replaceAll("\\s", ""), actual, "Serialized is not the same as object");
    }
}
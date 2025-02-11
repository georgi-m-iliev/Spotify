package bg.sofia.uni.fmi.mjt.spotify.commons.dto;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandResponseTest {
    @Test
    void builder() {
        String expectedStatus = "status";
        String expectedMessage = "message";
        AccessKey expectedAccessKey = new AccessKey("user", "token");
        Map<Integer, Song> expectedData = Map.of();
        AudioFormat expectedAudioFormat = new AudioFormat(44100, 16, 2, true, false);
        StreamTransport expectedTransport = StreamTransport.UDP;

        CommandResponse result = CommandResponse.builder()
                .status(expectedStatus)
                .message(expectedMessage)
                .accessKey(expectedAccessKey)
                .data(expectedData)
                .audioFormat(expectedAudioFormat)
                .transport(expectedTransport)
                .build();

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedMessage, result.message());
        assertEquals(expectedAccessKey, result.accessKey());
        assertEquals(expectedData, result.data());
        assertEquals(expectedAudioFormat, result.audioFormat());
        assertEquals(expectedTransport, result.transport());
    }

    @Test
    public void testBuildError() {
        String expectedStatus = "Error";
        String expectedMessage = "Error message";

        CommandResponse result = CommandResponse.builder()
                .status(expectedStatus)
                .message(expectedMessage)
                .build();

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedMessage, result.message());
    }

    @Test
    public void testBuildOK() {
        String expectedStatus = "OK";
        String expectedMessage = "OK message";

        CommandResponse result = CommandResponse.builder()
                .status(expectedStatus)
                .message(expectedMessage)
                .build();

        assertEquals(expectedStatus, result.status());
        assertEquals(expectedMessage, result.message());
    }
}
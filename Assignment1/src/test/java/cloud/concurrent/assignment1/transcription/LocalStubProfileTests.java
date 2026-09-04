package cloud.concurrent.assignment1.transcription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Proves the local profile replaces the paid provider through Spring dependency injection. */
// Activate the profile that enables LocalStubTranscriptionService.
@ActiveProfiles("local-stub")
// Load the real Spring application context, but skip starting an HTTP server for this test.
@SpringBootTest(properties = "spring.main.web-application-type=none")
class LocalStubProfileTests {

    // @Autowired asks Spring to inject the active implementation of this interface.
    @Autowired
    private TranscriptionService transcriptionService;

    @Test
    void localProfileUsesTheDeterministicStubWithoutAnApiKey() {
        // First prove dependency injection selected the local class rather than OpenAI.
        assertThat(transcriptionService).isInstanceOf(LocalStubTranscriptionService.class);

        // var lets Java infer that the local variable's type is AudioRecording.
        var recording = new AudioRecording(
                new byte[] {1, 2, 3},
                "audio/webm",
                "recording.webm");

        // The deterministic result confirms the stub works without a key or network call.
        assertThat(transcriptionService.transcribe(recording).text())
                .isEqualTo("Local demonstration: received 3 audio bytes.");
    }
}

package cloud.concurrent.assignment1.transcription;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Provides a free, deterministic local substitute for the external transcription provider
 *
 * This profile demonstrates Spring's environment-specific dependency injection. It lets a
 * dev exercise the browser-to-controller workflow without an API key, while the default
 * profile used by TITAN continues to select {@link OpenAiTranscriptionService}
 */

// @Service tells Spring to create and manage one object of this class
@Service
// @Profile limits this service to runs started with "local-stub" profile
@Profile("local-stub")

// "implements" = class provides every method required by TranscriptionService. controller can use either implementation
public class LocalStubTranscriptionService implements TranscriptionService {

    // @Override verify that this matches the interface method exactly
    @Override
    public TranscriptionResult transcribe(AudioRecording recording) {

        // no network request for demonstration purposes
        String message = "Local demonstration: received " + recording.bytes().length + " audio bytes.";
        return new TranscriptionResult(message, 0, 0);
    }
}

package cloud.concurrent.assignment1.transcription;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Provides a free, deterministic local substitute for the external transcription provider.
 *
 * <p>This profile demonstrates Spring's environment-specific dependency injection. It lets a
 * developer exercise the browser-to-controller workflow without an API key, while the default
 * profile used by TITAN continues to select {@link OpenAiTranscriptionService}.</p>
 */
// @Service tells Spring to create and manage one object of this class.
@Service
// @Profile limits this service to runs started with the "local-stub" profile.
@Profile("local-stub")
// "implements" promises that this class provides every method required by
// TranscriptionService. This allows the controller to use either implementation.
public class LocalStubTranscriptionService implements TranscriptionService {

    // @Override asks Java to verify that this matches the interface method exactly.
    @Override
    public TranscriptionResult transcribe(AudioRecording recording) {
        // This implementation deliberately performs no network request and spends no tokens.
        String message = "Local demonstration: received "
                + recording.bytes().length
                + " audio bytes.";
        return new TranscriptionResult(message, 0, 0);
    }
}

package cloud.concurrent.assignment1.transcription;

/**
 * Describes the capability the controller needs without tying it to a particular cloud provider.
 *
 * <p>This interface is also the dependency-injection seam used by controller tests: a small stub
 * can replace the real OpenAI service without spending API credit or making a network request.</p>
 */
// An interface describes what an object can do, but not how it performs the work.
// Different classes can implement this same contract for production and local testing.
public interface TranscriptionService {

    /**
     * Converts one audio recording into text.
     * Interface methods without a body are implicitly {@code public abstract},
     * so every concrete implementation must provide this method.
     */
    TranscriptionResult transcribe(AudioRecording recording);
}

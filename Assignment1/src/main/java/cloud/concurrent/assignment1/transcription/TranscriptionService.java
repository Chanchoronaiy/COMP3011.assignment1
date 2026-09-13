package cloud.concurrent.assignment1.transcription;

/**
 * Describes the capability the controller needs without tying it to a particular cloud provider.
 *
 * This interface is also the dependency-injection seam used by controller tests: a small stub
 * can replace the real OpenAI service without spending API credit or making a network request.
 */
// INTERFACE describes the methods a class must implement. does not provide any implementation itself
// Different classes can implement this same contract for production and local testing
// They are implicitly {@code public abstract}, every concrete implementation must provide this method

public interface TranscriptionService {
    TranscriptionResult transcribe(AudioRecording recording);
}

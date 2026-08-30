package cloud.concurrent.assignment1.transcription;

import cloud.concurrent.assignment1.statistics.RequestStatistics;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Accepts browser audio and returns its transcription as JSON. */
// @RestController makes returned records become HTTP JSON response bodies.
@RestController
// All routes in this controller begin with /api/v1/record.
@RequestMapping("/api/v1/record")
public class TranscriptionController {

    // static means one class-level value is shared by every controller object.
    // final means the reference cannot be reassigned after it is initialised.
    // Multiplication makes the 25 MiB limit easier to understand than 26,214,400.
    private static final int MAX_RECORDING_BYTES = 25 * 1024 * 1024;
    // Map.ofEntries creates an immutable MIME-type allow-list. It cannot be changed
    // accidentally while concurrent request threads are reading from it.
    private static final Map<String, String> FILE_EXTENSIONS = Map.ofEntries(
            Map.entry("audio/webm", "webm"),
            Map.entry("audio/ogg", "ogg"),
            Map.entry("application/ogg", "ogg"),
            Map.entry("audio/wav", "wav"),
            Map.entry("audio/x-wav", "wav"),
            Map.entry("audio/mpeg", "mp3"),
            Map.entry("audio/mp3", "mp3"),
            Map.entry("audio/mp4", "m4a"),
            Map.entry("audio/x-m4a", "m4a"));

    private final TranscriptionService transcriptionService;
    // RequestStatistics uses atomic counters, so the same object is safe across threads.
    private final RequestStatistics statistics;

    public TranscriptionController(
            TranscriptionService transcriptionService,
            RequestStatistics statistics) {
        // Spring supplies both dependencies. Depending on the interface lets the default
        // OpenAI service be replaced by the local stub profile or a test double.
        this.transcriptionService = transcriptionService;
        this.statistics = statistics;
    }

    /**
     * Uses a raw request body to avoid an unnecessary second multipart encoding in the browser.
     * The backend creates the multipart body required by OpenAI after validating the recording.
     */
    // This method receives POST /api/v1/record/upload.
    @PostMapping("/upload")
    public TranscriptionResponse uploadRecording(
            // @RequestBody places the raw uploaded bytes in this array. required=false lets
            // our validation return a controlled error when the body is missing.
            @RequestBody(required = false) byte[] audioBytes,
            // @RequestHeader reads the browser's Content-Type header in the same way.
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType) {
        // Validate before counting the request or contacting the paid external API.
        AudioRecording recording = validate(audioBytes, contentType);
        statistics.recordStarted(recording.bytes().length);
        // nanoTime measures elapsed time and is not affected by changes to the wall clock.
        long startNanoseconds = System.nanoTime();

        try {
            // This call blocks while the cloud provider responds. Spring's virtual-thread
            // configuration allows many such blocking requests without one platform thread each.
            TranscriptionResult result = transcriptionService.transcribe(recording);
            statistics.recordSucceeded(
                    result.inputTokens(),
                    result.outputTokens(),
                    System.nanoTime() - startNanoseconds);
            return new TranscriptionResponse(result.text());
        } catch (RuntimeException exception) {
            // The shared statistics must be balanced on both success and failure paths.
            statistics.recordFailed(System.nanoTime() - startNanoseconds);
            // Rethrow the same exception so the shared API handler can choose the HTTP response.
            throw exception;
        }
    }

    // private means validation is an internal controller detail, not another HTTP endpoint.
    private AudioRecording validate(byte[] audioBytes, String contentType) {
        // || means OR. Short-circuiting prevents .length from being read when the array is null.
        if (audioBytes == null || audioBytes.length == 0) {
            throw new InvalidRecordingException("Please record some audio before uploading.");
        }

        if (audioBytes.length > MAX_RECORDING_BYTES) {
            throw new InvalidRecordingException("The audio recording is larger than 25 MB.");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new InvalidRecordingException("The audio recording has no content type.");
        }

        // Browser MIME types can include parameters such as "codecs=opus".
        // split(";", 2)[0] keeps the part before the first semicolon; [0] accesses
        // the first item in the resulting String array.
        String baseContentType = contentType
                .split(";", 2)[0]
                .trim()
                // Locale.ROOT makes lower-casing predictable on every server locale.
                .toLowerCase(Locale.ROOT);
        // The map both acts as an allow-list and supplies the filename extension OpenAI expects.
        String extension = FILE_EXTENSIONS.get(baseContentType);

        if (extension == null) {
            throw new InvalidRecordingException("This audio format is not supported.");
        }

        return new AudioRecording(
                audioBytes,
                contentType,
                // + joins the fixed filename prefix with the validated extension.
                "recording." + extension);
    }

    /**
     * Only the transcription is sent to the browser; provider metadata remains server-side.
     * Spring serialises this record as JSON containing exactly one {@code text} property.
     */
    public record TranscriptionResponse(String text) {
    }
}

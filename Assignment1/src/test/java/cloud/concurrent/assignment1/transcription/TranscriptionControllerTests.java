package cloud.concurrent.assignment1.transcription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.concurrent.assignment1.statistics.RequestStatistics;
import cloud.concurrent.assignment1.web.ApiExceptionHandler;
import cloud.concurrent.assignment1.web.ApiRequestLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Tests the REST controller with a stub instead of contacting the paid cloud API. */
// @ExtendWith adds JUnit functionality; this extension captures log output for assertions
@ExtendWith(OutputCaptureExtension.class)
class TranscriptionControllerTests {

    private RequestStatistics statistics;
    private StubTranscriptionService stubService;
    private MockMvc mockMvc;

    // runs before every @Test so one test cannot affect another test's state
    @BeforeEach
    void setUp() {
        statistics = new RequestStatistics();
        stubService = new StubTranscriptionService();
        TranscriptionController controller =
                new TranscriptionController(stubService, statistics);

        // Build an in-memory HTTP layer with the real error handler and logging filter
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new ApiRequestLoggingFilter())
                .build();
    }

    @Test
    void validRecordingReturnsTheStubbedTranscription(CapturedOutput output) throws Exception {
        // contentType describes the audio format; content supplies the pretend audio bytes
        mockMvc.perform(post("/api/v1/record/upload")
                        .contentType("audio/webm;codecs=opus")
                        .content(new byte[] {1, 2, 3, 4}))
                .andExpect(status().isOk())
                .andExpect(header().exists(ApiRequestLoggingFilter.REQUEST_ID_HEADER))
                .andExpect(jsonPath("$.text").value("hello from the stub"));

        assertThat(output).contains(
                "method=POST path=/api/v1/record/upload status=200 durationMs=");

        // test also checks that usage information reached the thread-safe counters
        var snapshot = statistics.snapshot();
        assertThat(snapshot.successfulRequests()).isEqualTo(1);
        assertThat(snapshot.totalInputTokens()).isEqualTo(12);
        assertThat(snapshot.totalOutputTokens()).isEqualTo(4);
        assertThat(snapshot.averageProcessingMilliseconds()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void emptyRecordingReturnsAHelpfulBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/record/upload")
                        .contentType(MediaType.valueOf("audio/webm"))
                        .content(new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Please record some audio before uploading."));
    }

    @Test
    void unsupportedRecordingFormatIsRejectedBeforeTheServiceRuns() throws Exception {
        mockMvc.perform(post("/api/v1/record/upload")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not audio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This audio format is not supported."));

        // Rejection should happen during validation, before the provider is contacted
        assertThat(stubService.callCount).isZero();
    }

    @Test
    void providerFailureReturnsASafeErrorAndReleasesTheActiveCounter() throws Exception {
        // Telling the test double to fail simulates an unavailable cloud provider.
        stubService.fail = true;

        mockMvc.perform(post("/api/v1/record/upload")
                        .contentType(MediaType.valueOf("audio/webm"))
                        .content(new byte[] {1, 2, 3}))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("Speech transcription is temporarily unavailable."));

        var snapshot = statistics.snapshot();
        assertThat(snapshot.failedRequests()).isEqualTo(1);
        assertThat(snapshot.activeRequests()).isZero();
    }

    /** A deterministic test double makes the controller tests fast and repeatable. */
    // static = test double does not need an outer test object. final prevents extension.
    private static final class StubTranscriptionService implements TranscriptionService {

        private int callCount;
        private boolean fail;

        @Override
        public TranscriptionResult transcribe(AudioRecording recording) {
            callCount++;
            if (fail) {
                throw new TranscriptionServiceException("provider details", null); // exception shows the controller's failure path.
            }
            return new TranscriptionResult("hello from the stub", 12, 4);
        }
    }
}

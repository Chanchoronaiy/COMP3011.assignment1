package cloud.concurrent.assignment1.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.concurrent.assignment1.transcription.AudioRecording;
import cloud.concurrent.assignment1.transcription.TranscriptionResult;
import cloud.concurrent.assignment1.transcription.TranscriptionService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Sends more than 200 real HTTP requests to the embedded server at the same time. */
// RANDOM_PORT starts a real embedded server on any free port, avoiding a clash with port 8080.
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "logging.level.root=WARN")
class ConcurrentHttpRequestTests {

    private static final int CONCURRENT_REQUESTS = 220;

    // Spring writes the chosen random port into this field after the test server starts.
    @LocalServerPort
    private int port;

    @Autowired
    private BlockingStubTranscriptionService stubService;

    @Test
    void serverHandlesMoreThanTwoHundredSimultaneousBlockingRequests() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        // Each future represents an HTTP response that will arrive asynchronously later.
        List<CompletableFuture<HttpResponse<String>>> responses = new ArrayList<>();

        for (int index = 0; index < CONCURRENT_REQUESTS; index++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:" + port + "/api/v1/record/upload"))
                    .header("Content-Type", "audio/webm")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(new byte[] {1, 2, 3}))
                    .build();
            // sendAsync returns immediately, allowing all 220 requests to overlap.
            responses.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()));
        }

        boolean everyRequestReachedTheBlockingService = false;
        try {
            everyRequestReachedTheBlockingService =
                    stubService.awaitEveryRequest(Duration.ofSeconds(10));
        } finally {
            // Always release handlers, even when the assertion is about to fail.
            stubService.releaseRequests();
        }

        // allOf creates one future that completes after every response future has completed.
        CompletableFuture
                .allOf(responses.toArray(CompletableFuture[]::new))
                .get(10, TimeUnit.SECONDS);

        assertThat(everyRequestReachedTheBlockingService).isTrue();
        assertThat(stubService.highestConcurrentRequests())
                .isGreaterThanOrEqualTo(CONCURRENT_REQUESTS);
        assertThat(responses)
                .allSatisfy(response -> assertThat(response.join().statusCode()).isEqualTo(200));
    }

    /** Replaces the paid provider while preserving genuinely blocking server-side work. */
    static final class BlockingStubTranscriptionService implements TranscriptionService {

        // This latch reaches zero only when all 220 handlers are blocked inside the stub.
        private final CountDownLatch allRequestsArrived =
                new CountDownLatch(CONCURRENT_REQUESTS);
        // This one is a gate that releases every blocked handler at the same time.
        private final CountDownLatch releaseRequests = new CountDownLatch(1);
        // AtomicInteger keeps increments and reads safe when many threads access them together.
        private final AtomicInteger activeRequests = new AtomicInteger();
        private final AtomicInteger highestConcurrentRequests = new AtomicInteger();

        @Override
        public TranscriptionResult transcribe(AudioRecording recording) {
            int active = activeRequests.incrementAndGet();
            highestConcurrentRequests.accumulateAndGet(active, Math::max);
            allRequestsArrived.countDown();

            try {
                releaseRequests.await();
                return new TranscriptionResult("stub transcription", 2, 1);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Stub request was interrupted", exception);
            } finally {
                activeRequests.decrementAndGet();
            }
        }

        boolean awaitEveryRequest(Duration timeout) throws InterruptedException {
            return allRequestsArrived.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void releaseRequests() {
            releaseRequests.countDown();
        }

        int highestConcurrentRequests() {
            return highestConcurrentRequests.get();
        }
    }

    /** Dependency injection chooses this @Primary stub instead of the real OpenAI service. */
    // This configuration exists only while the test application context is running.
    @TestConfiguration(proxyBeanMethods = false)
    static class StubConfiguration {

        @Bean
        // @Primary makes this stub win when Spring finds both transcription implementations.
        @Primary
        BlockingStubTranscriptionService blockingStubTranscriptionService() {
            return new BlockingStubTranscriptionService();
        }
    }
}

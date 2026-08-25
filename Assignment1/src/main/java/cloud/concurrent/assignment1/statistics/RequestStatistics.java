package cloud.concurrent.assignment1.statistics;

import org.springframework.stereotype.Component;

/**
 * Collects process-wide request and token measurements.
 *
 * <p>This is the simplest thread-safe version. The {@code synchronized} keyword lets only one
 * thread at a time execute a synchronized method on this object. That mutual exclusion prevents
 * two request threads from reading and overwriting the same counter at the same moment.</p>
 */
// @Component tells Spring to create and share one RequestStatistics object across the application.
@Component
public class RequestStatistics {

    // These fields are shared mutable state: many request threads can update the same values.
    private long totalRequests;
    private long successfulRequests;
    private long failedRequests;
    private long totalAudioBytes;
    private long totalInputTokens;
    private long totalOutputTokens;
    private long totalProcessingNanoseconds;
    private int activeRequests;
    private int highestConcurrentRequests;

    // synchronized locks this RequestStatistics object for the entire method call.
    public synchronized void recordStarted(long audioBytes) {
        // ++ is shorthand for adding one to the current counter value.
        totalRequests++;
        totalAudioBytes += audioBytes;
        activeRequests++;
        highestConcurrentRequests = Math.max(highestConcurrentRequests, activeRequests);
    }

    // The same object lock protects all related success updates as one critical section.
    public synchronized void recordSucceeded(
            long inputTokens,
            long outputTokens,
            long processingNanoseconds) {
        successfulRequests++;
        totalInputTokens += inputTokens;
        totalOutputTokens += outputTokens;
        totalProcessingNanoseconds += processingNanoseconds;
        activeRequests--;
    }

    // A failed request must also reduce activeRequests so the shared count stays balanced.
    public synchronized void recordFailed(long processingNanoseconds) {
        failedRequests++;
        totalProcessingNanoseconds += processingNanoseconds;
        activeRequests--;
    }

    // Reading is synchronized too; otherwise another thread could change fields mid-snapshot.
    public synchronized StatisticsSnapshot snapshot() {
        long completedRequests = successfulRequests + failedRequests;
        double averageProcessingMilliseconds = completedRequests == 0
                ? 0
                : totalProcessingNanoseconds / 1_000_000.0 / completedRequests;

        return new StatisticsSnapshot(
                totalRequests,
                successfulRequests,
                failedRequests,
                activeRequests,
                highestConcurrentRequests,
                totalAudioBytes,
                totalInputTokens,
                totalOutputTokens,
                averageProcessingMilliseconds);
    }
}

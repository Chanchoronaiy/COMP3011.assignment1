package cloud.concurrent.assignment1.statistics;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * Collects request measurements without putting one large lock around the application.
 *
 * LongAdder spreads frequent counter updates across internal cells. That makes it a better fit
 * than one synchronized integer when more than 200 request threads may update the same counter
 * allows concurrent updates without a single synchronized method becoming a bottleneck
 */
@Component
public class RequestStatistics {

    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final LongAdder totalAudioBytes = new LongAdder();
    private final LongAdder totalInputTokens = new LongAdder();
    private final LongAdder totalOutputTokens = new LongAdder();
    private final LongAdder totalProcessingNanoseconds = new LongAdder();
    // AtomicInteger is used because the exact updated active count is needed immediately
    private final AtomicInteger activeRequests = new AtomicInteger();
    // LongAccumulator applies Long::max atomically, preserving the largest observed count
    private final LongAccumulator highestConcurrentRequests =
            new LongAccumulator(Long::max, 0);

    /* records a request after its input has passed validation. */
    public void recordStarted(long audioBytes) {
        totalRequests.increment();
        totalAudioBytes.add(audioBytes);

        int active = activeRequests.incrementAndGet();
        highestConcurrentRequests.accumulate(active);
    }

    /** records provider result and releases this request from the active count */
    public void recordSucceeded(
            long inputTokens,
            long outputTokens,
            long processingNanoseconds) {
        successfulRequests.increment();
        totalInputTokens.add(inputTokens);
        totalOutputTokens.add(outputTokens);
        totalProcessingNanoseconds.add(processingNanoseconds);
        activeRequests.decrementAndGet();
    }

    /** records a failed provider call and releases this request from the active count */
    public void recordFailed(long processingNanoseconds) {
        failedRequests.increment();
        totalProcessingNanoseconds.add(processingNanoseconds);
        activeRequests.decrementAndGet();
    }

    /** returns an immutable point-in-time view suitable for a JSON response. */
    public StatisticsSnapshot snapshot() {
        // sum() reads each LongAdder's current value. 
        // With concurrent requests, this is a point-in-time monitoring view rather than one transactionally locked snapshot
        long completedRequests = successfulRequests.sum() + failedRequests.sum();
        double averageProcessingMilliseconds = completedRequests == 0
                ? 0
                : totalProcessingNanoseconds.sum() / 1_000_000.0 / completedRequests;

        return new StatisticsSnapshot(
                totalRequests.sum(),
                successfulRequests.sum(),
                failedRequests.sum(),
                activeRequests.get(),
                highestConcurrentRequests.get(),
                totalAudioBytes.sum(),
                totalInputTokens.sum(),
                totalOutputTokens.sum(),
                averageProcessingMilliseconds);
    }
}

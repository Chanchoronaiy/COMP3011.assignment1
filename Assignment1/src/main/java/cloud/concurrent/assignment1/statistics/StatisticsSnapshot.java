package cloud.concurrent.assignment1.statistics;

/**
 * Stores one point-in-time copy of the request statistics.
 *
 * {@code record} keyword is for a small data-only type. Java automatically creates
 * a constructor, private final fields, value accessors, {@code equals}, {@code hashCode}, and
 * {@code toString}. E.g {@code snapshot.totalRequests()} 
 *
 * RECORD is immutable: its values cannot be changed after construction. A controller can
 * read and return this snapshot without modifying the live thread-safe counters
 */
public record StatisticsSnapshot(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        int activeRequests,
        long highestConcurrentRequests,
        long totalAudioBytes,
        long totalInputTokens,
        long totalOutputTokens,
        double averageProcessingMilliseconds) {
}

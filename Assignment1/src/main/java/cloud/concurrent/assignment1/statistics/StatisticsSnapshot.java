package cloud.concurrent.assignment1.statistics;

/**
 * Stores one point-in-time copy of the request statistics.
 *
 * <p>The {@code record} keyword is useful for a small data-only type. Java automatically creates
 * a constructor, private final fields, value accessors, {@code equals}, {@code hashCode}, and
 * {@code toString}. For example, {@code snapshot.totalRequests()} reads the first component.</p>
 *
 * <p>The record is immutable: its values cannot be changed after construction. A controller can
 * therefore read and return this snapshot without modifying the live thread-safe counters.</p>
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

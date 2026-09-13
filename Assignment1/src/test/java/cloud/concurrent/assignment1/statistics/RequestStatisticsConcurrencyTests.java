package cloud.concurrent.assignment1.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;

/** Regression test for counter updates made by more than 200 concurrent threads. */
class RequestStatisticsConcurrencyTests {

    private static final int THREAD_COUNT = 240;

    @Test
    void countersRemainCorrectWhenManyThreadsFinishTogether() throws InterruptedException {
        RequestStatistics statistics = new RequestStatistics();
        // latch reaches zero only after all 240 threads have been created and are ready
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        // second latch acts like a starting gate so the threads overlap in time.
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int index = 0; index < THREAD_COUNT; index++) {
            // virtual thread is lightweight, so Java can run many blocking tasks efficiently
            Thread thread = Thread.ofVirtual().start(() -> {
                ready.countDown();
                // Each thread waits here until main test opens the starting gate
                await(startTogether);
                statistics.recordStarted(100);
                statistics.recordSucceeded(2, 1, 1_000_000);
            });
            threads.add(thread);
        }

        // await() pauses this test until every worker is ready; countDown() opens the gate
        ready.await();
        startTogether.countDown();

        for (Thread thread : threads) {
            // join() ensures every update is finished before the final totals are inspected
            thread.join();
        }

        // If counters were not thread-safe, one or more of these totals could be too small.
        StatisticsSnapshot snapshot = statistics.snapshot();
        assertThat(snapshot.totalRequests()).isEqualTo(THREAD_COUNT);
        assertThat(snapshot.successfulRequests()).isEqualTo(THREAD_COUNT);
        assertThat(snapshot.failedRequests()).isZero();
        assertThat(snapshot.activeRequests()).isZero();
        assertThat(snapshot.totalAudioBytes()).isEqualTo(THREAD_COUNT * 100L);
        assertThat(snapshot.totalInputTokens()).isEqualTo(THREAD_COUNT * 2L);
        assertThat(snapshot.totalOutputTokens()).isEqualTo(THREAD_COUNT);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            // restore the flag so callers can observe that this virtual thread was interrupted
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrency test was interrupted", exception);
        }
    }
}

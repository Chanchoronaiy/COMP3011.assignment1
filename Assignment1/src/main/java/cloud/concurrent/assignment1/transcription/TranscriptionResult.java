package cloud.concurrent.assignment1.transcription;

/**
 * Text and token usage returned by the cloud transcription provider.
 *
 * {@code String text} stores the transcript. The two {@code long} values
 * store whole-number token counts. {@code long} supports much larger totals
 * than {@code int}, which is useful when usage is accumulated across requests
 * 
 * record is suitable here bc this class only carries result data
 */

// Java creates text(), inputTokens(), and outputTokens() accessor methods
public record TranscriptionResult(String text, long inputTokens, long outputTokens) {
}

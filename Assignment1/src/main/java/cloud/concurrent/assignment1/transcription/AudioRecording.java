package cloud.concurrent.assignment1.transcription;

/**
 * The validated audio data passed from the controller to the transcription service.
 *
 * <p>A record groups related data without manually writing fields, a constructor,
 * accessors, {@code equals()}, {@code hashCode()}, and {@code toString()}.</p>
 *
 * <p>{@code byte[]} is an array containing the uploaded file's raw bytes. Arrays
 * are mutable even when stored in a record, so the application treats this array
 * as read-only after validation and does not modify it between threads.</p>
 */
// Java creates the accessors bytes(), contentType(), and fileName() automatically.
// Component names use lower camelCase, following standard Java variable naming.
public record AudioRecording(byte[] bytes, String contentType, String fileName) {
}

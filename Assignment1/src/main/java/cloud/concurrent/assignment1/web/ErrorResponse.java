package cloud.concurrent.assignment1.web;

import java.time.Instant;

/**
 * Standard error body matching the five fields defined in {@code assignment1api.yaml}.
 *
 * RECORD used bc this type only carries response data. 
 * Spring auto turns component names into JSON property names. {@link Instant} stores the timestamp in a timezone-independent UTC form
 *  {@code status} stores the numeric HTTP code, and {@code path} identifies the endpoint that failed.</p>
 *
 * Using one shared type keeps errors predictable for the browser and avoids exposing Java
 * exception details, credentials, audio, or transcribed text in API responses
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path) {
}

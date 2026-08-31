package cloud.concurrent.assignment1.web;

import cloud.concurrent.assignment1.admin.ShutdownInProgressException;
import cloud.concurrent.assignment1.transcription.InvalidRecordingException;
import cloud.concurrent.assignment1.transcription.MissingApiKeyException;
import cloud.concurrent.assignment1.transcription.TranscriptionServiceException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converts failures into the standard JSON error shape defined by the assignment API. */
// @RestControllerAdvice applies these handlers across every REST controller.
// It keeps error-conversion code out of the individual endpoint methods.
@RestControllerAdvice
public class ApiExceptionHandler {

    // The class-wide logger records server diagnostics without sending them to clients.
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // @ExceptionHandler routes this exception type to this particular method.
    @ExceptionHandler(InvalidRecordingException.class)
    // ResponseEntity<ErrorResponse> means the HTTP response body has our standard error shape.
    ResponseEntity<ErrorResponse> handleInvalidRecording(
            InvalidRecordingException exception,
            HttpServletRequest request) {
        // @ExceptionHandler selects this method when a controller throws the matching type.
        // HTTP 400 means the client supplied an invalid recording.
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MissingApiKeyException.class)
    ResponseEntity<ErrorResponse> handleMissingApiKey(
            MissingApiKeyException exception,
            HttpServletRequest request) {
        // Log only the configuration name, never the secret value.
        LOGGER.error("OPENAI_API_KEY is missing; transcription requests cannot be processed");
        // HTTP 503 means the server cannot currently provide this configured service.
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), request);
    }

    @ExceptionHandler(TranscriptionServiceException.class)
    ResponseEntity<ErrorResponse> handleProviderFailure(
            TranscriptionServiceException exception,
            HttpServletRequest request) {
        // Do not log audio, transcribed text, the API key, or the provider response body.
        String causeType = exception.getCause() == null
                ? "unavailable"
                : exception.getCause().getClass().getSimpleName();
        // {} is an SLF4J placeholder. The logger inserts causeType without manually
        // concatenating strings or printing the provider's potentially sensitive body.
        LOGGER.warn("Cloud transcription request failed: causeType={}", causeType);
        // HTTP 502 means this server received a failure from an upstream service.
        return error(
                HttpStatus.BAD_GATEWAY,
                "Speech transcription is temporarily unavailable.",
                request);
    }

    @ExceptionHandler(ShutdownInProgressException.class)
    ResponseEntity<ErrorResponse> handleShutdownInProgress(
            ShutdownInProgressException exception,
            HttpServletRequest request) {
        // HTTP 409 reports that a new shutdown conflicts with one already in progress.
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    // Exception is broader than the custom types above, so this acts as the final fallback.
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpectedFailure(
            Exception exception,
            HttpServletRequest request) {
        // Keep one final catch-all so unexpected Java errors still use the documented JSON shape.
        LOGGER.error("Unexpected request failure", exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred.",
                request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        // Centralising construction keeps every failure response consistent with the YAML API.
        // ResponseEntity's fluent builder first selects a status and then attaches the body.
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()));
    }
}

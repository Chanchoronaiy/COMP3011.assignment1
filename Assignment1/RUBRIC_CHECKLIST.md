# Assignment 1 rubric evidence checklist

## REST API, backend, and speech-to-text

- [x] Spring REST upload route: `TranscriptionController`
- [x] OpenAI `/v1/audio/transcriptions` integration: `OpenAiTranscriptionService`
- [x] Required `gpt-4o-mini-transcribe` model: `application.properties`
- [x] Automatic text response after recording stops: `static/app.js`
- [x] Input validation for empty, oversized, and unsupported recordings
- [x] Shared five-field error response: `ApiExceptionHandler` and `ErrorResponse`
- [x] Exact uptime, shutdown, and global-statistics fields from the supplied YAML
- [x] REST-controller regression tests with stubbed transcription
- [x] Provider-boundary test with a local mock HTTP server
- [ ] Real transcription using the runtime key (must be checked in TITAN/local authorised runtime)

## Concurrency and Spring engineering

- [x] Java virtual threads enabled for blocking cloud I/O
- [x] Tomcat capacity configured above 200 requests
- [x] Constructor injection through the `TranscriptionService` interface
- [x] Default cloud service plus an explicit `local-stub` Spring profile
- [x] Thread-safe global state using `LongAdder`, `AtomicInteger`, and `LongAccumulator`
- [x] Race-regression test using 240 concurrent virtual threads
- [x] Embedded-server regression test with 220 simultaneous blocking HTTP requests
- [x] Graceful shutdown with bounded drain time and clean process exit implementation
- [x] Reject new work while an accepted shutdown is draining existing requests
- [x] Confirm shutdown timing and exit status in the final TITAN environment

## Frontend, accessibility, and client performance

- [x] Exact start/stop labels and intuitive disabled-button states
- [x] Visible recording indicator and elapsed-time display
- [x] `aria-live` status, alert errors, semantic headings, and keyboard focus management
- [x] Microphone denied/missing/unsupported error handling
- [x] Server failure, invalid JSON, and timeout handling with `async`/`await`
- [x] Automatic upload, visible transcription, and reset for another recording
- [x] Automatic stop below one minute
- [x] Mono audio, Opus preference, and 32 kbit/s bitrate to reduce upload size
- [x] Microphone permission and recording demonstrated in the target browser with `local-stub`

## Code quality, comments, testing, and security

- [x] Lower-case packages and standard Java naming/layout conventions
- [x] Comments and Javadoc explain intent, concurrency choices, and course concepts
- [x] Environment-only API key; no secret in client code or default configuration
- [x] Privacy-safe structured request logging with correlation identifiers
- [x] Logging regression assertion
- [x] README documents architecture, profiles, build, API, tests, security, and limitations
- [x] AI and external-resource assistance acknowledged
- [x] Single executable Spring Boot fat JAR build configured
- [x] Inspect final repository visibility/history and submit the readable repository URL
- [x] Upload final JAR and pass all TITAN checks

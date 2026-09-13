# COMP3011 Assignment 1 — Speech to Text

This Spring Boot application records up to one minute of microphone audio in the browser, sends
the recording to a Java REST endpoint, and uses OpenAI's speech-to-text API to produce a
transcription.

## Architecture and course concepts

```text
Browser microphone
      |
      | compressed raw audio over HTTP
      v
TranscriptionController
      |
      | constructor-injected TranscriptionService interface
      v
OpenAiTranscriptionService -----> OpenAI /v1/audio/transcriptions
      |
      v
Thread-safe global statistics
```

The `TranscriptionService` interface separates the REST controller from the cloud provider.
Spring constructor injection selects the real service normally, a local stub under the
`local-stub` profile, and deterministic test doubles in regression tests. This keeps tests free
from paid network calls and makes configuration differences explicit rather than commenting code
in and out.

The server enables Java virtual threads because each transcription request spends most of its time
waiting for blocking cloud I/O. The shared counters use `LongAdder` and atomic classes so hundreds
of request threads can update statistics safely without one application-wide lock.

## Requirements

- Java 21 or newer
- A valid OpenAI API key in the `OPENAI_API_KEY` environment variable
- A current browser with microphone support (Chrome is recommended)

The API key is read only by the Java server. It is never placed in the HTML or JavaScript, returned
in a response, or written to application logs.

## Run locally with the real provider

From the `Assignment1` directory:

```bash
read -s "OPENAI_API_KEY?Paste your OpenAI API key (hidden): "
echo
export OPENAI_API_KEY
./mvnw spring-boot:run
```

The `read -s` prompt hides the key and keeps it out of the command itself. Open
<http://localhost:8080> and allow microphone access when prompted. Do not commit a real key to the
repository or paste it into a browser file. After stopping the server with `Control+C`, run
`unset OPENAI_API_KEY` to remove the key from the current terminal session.

## Run locally without an API key

The `local-stub` Spring profile exercises the complete browser upload and REST flow without
contacting OpenAI:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-stub
```

The returned message reports how many audio bytes reached the backend. It is deliberately labelled
as a local demonstration and is not a real transcription. TITAN uses the default profile and
therefore selects `OpenAiTranscriptionService`.

## Build the executable JAR

```bash
./mvnw clean test
./mvnw clean package
java -jar target/assignment1-0.0.1-SNAPSHOT.jar
```

The Spring Boot Maven plugin creates one executable fat JAR containing the application and its
runtime dependencies. This is the JAR intended for the TITAN upload.

## REST API contract

| Method | Path | Purpose | Successful result |
| --- | --- | --- | --- |
| `POST` | `/api/v1/record/upload` | Accept raw browser audio | `200` with `text` |
| `GET` | `/api/v1/admin/uptime` | Report UTC start, current UTC time, and uptime | `200` with exactly three prescribed fields |
| `POST` | `/api/v1/admin/shutdown` | Start graceful process shutdown | `202` with the prescribed message |
| `GET` | `/api/v1/global/stats` | Report global token totals | `200` with exactly `inputTokens` and `outputTokens` |

A second shutdown request returns the YAML-prescribed `409` error. During graceful draining, new
non-shutdown work receives `422` so in-flight work can finish within TITAN's five-second window.
All controller errors use the shared `timestamp`, `status`, `error`, `message`, and `path` shape.

## Frontend behaviour

- Exact accessible **Start audio recording** and **Stop recording** controls
- Visible and screen-reader-announced ready, recording, uploading, success, and failure states
- Automatic upload when recording stops and automatic reset for another recording
- Automatic stop at 59 seconds to remain under the one-minute limit
- `async`/`await`, request timeout handling, microphone permission errors, and provider errors
- Preferred mono Opus recording at 32 kbit/s to reduce network load and transcription latency
- Microphone tracks released immediately after recording to protect privacy and device resources

Browser audio support differs by platform. The client prefers WebM/Opus, then WebM, Ogg/Opus,
and MP4. Chrome is the safest demonstration browser. The server validates type, emptiness, and the
25 MB provider limit before any cloud request.

## Regression-testing approach

The suite runs without a real API key. Every test has a specific failure it is designed to prevent:

| Test class | Why it exists and expected result | Assurance provided |
| --- | --- | --- |
| `Assignment1ApplicationTests` | Spring must create the full default application context | Configuration and dependency wiring are complete |
| `LocalStubProfileTests` | Activating `local-stub` must select the local implementation | Profiles replace environment-specific dependencies correctly |
| `TranscriptionControllerTests` | Valid audio returns stub text; empty or unsupported audio returns `400` | REST validation, statistics, safe errors, correlation header, and completion logging remain correct |
| `OpenAiTranscriptionServiceTests` | A mock provider must receive the model, bearer header, and multipart audio and return mapped text/tokens | The real OpenAI HTTP boundary is correctly formed without spending API credit |
| `AdministrationControllerTests` | Uptime has exactly three fields; first shutdown is `202`; second is `409` | Administration routes match the supplied YAML contract |
| `StatisticsControllerTests` | Statistics returns exactly the two token fields | Internal measurements cannot leak into the prescribed response |
| `RequestStatisticsConcurrencyTests` | 240 virtual threads finish together and every total remains exact | A regression in thread-safe counters exposes a race condition |
| `ConcurrentHttpRequestTests` | 220 blocking requests must all enter the controller simultaneously and return `200` | One Java process handles more than 200 overlapping blocking HTTP operations |

The two concurrency tests are intentionally different: one attacks shared mutable state, while the
other exercises the real embedded HTTP server and controller path.

## Logging and security

Each `/api/` response contains an `X-Request-Id`. The server logs only the request method, path,
status, request identifier, and duration. The regression suite verifies the correlation header and
completion log. Request bodies, audio, transcribed text, API keys, authorization headers, and cloud
response bodies are deliberately excluded. Expected validation failures are returned safely;
unexpected technical exceptions remain server-side.

## Source-code conventions

The project follows the supplied Java conventions: lower-case package names, `UpperCamelCase`
classes, `lowerCamelCase` variables and methods, four-space indentation, and Javadoc for public
types and important public operations. Comments explain design reasons and course concepts instead
of translating every Java statement into English.

## Development assistance and references

OpenAI Codex was used for Java/Spring research, design review, implementation suggestions,
documentation, and test generation. All submitted code must be reviewed and understood by the
student, who remains responsible for explaining its behaviour. The implementation also refers to
the supplied course materials, assignment API YAML, Java naming-convention document, Spring Boot
documentation, and the official OpenAI transcription API documentation.


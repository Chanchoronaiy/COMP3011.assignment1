# COMP3011 Assignment 1 — Speech to Text

This Spring Boot application records up to one minute of microphone audio in the browser, sends
the recording to a Java REST endpoint, and uses OpenAI's speech-to-text API to produce a
transcription.

## ⚙️ Architecture and course concepts

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
Spring constructor injection selects the real service normally, and a local stub under the
`local-stub` profile, and deterministic test doubles in regression tests, keeping away from paid network calls.

The server enables Java virtual threads because each transcription request spends most of its time
waiting for blocking cloud I/O. The shared counters use `LongAdder` and atomic classes so hundreds
of request threads can update statistics safely without one application-wide lock.

## 💻 Techonology Requirements

- Java 21 or newer
- A valid OpenAI API key in the `OPENAI_API_KEY` environment variable
- A current browser with microphone support (Chrome is recommended)

## 🛠️ Running the project

From `Assignment1` dirctory:

```bash
read -s "OPENAI_API_KEY?Paste your OpenAI API key (hidden): "
echo
export OPENAI_API_KEY
./mvnw spring-boot:run
```

The `read -s` prompt hides the key and keeps it out of the command itself. Open
<http://localhost:8080> and allow microphone access when prompted. DO NOT commit a real key to the
repository or paste it into a browser file.

## Runing th eporject LOCALLY without API key

The `local-stub` Spring profile shows complete browser upload and REST flow without
contacting OpenAI:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local-stub
```

The returned message reports how many audio bytes reached the backend. TITAN uses the default profile and
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
- Automatic stop at 59 seconds to remain under the one-minute limit
- `async`/`await`, request timeout handling, microphone permission errors, and provider errors
- Preferred mono Opus recording at 32 kbit/s to reduce network load and transcription latency
- Microphone tracks released immediately after recording to protect privacy and device resources


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


## Development assistance and references

OpenAI Codex was used as assistance for Java/Spring research, design review, implementation suggestions,
and helping in test generation. The theory references the course materials, corss-checked with the assignment API YAML, Java naming-convention document.


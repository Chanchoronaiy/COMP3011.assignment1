# COMP3011 Assignment 1 — Speech to Text

This Spring Boot application records microphone audio in the browser, sends the recording to a Java
REST endpoint, and uses OpenAI's speech-to-text API to produce a transcription.

## Architecture

```text
Browser microphone
      |
      v
TranscriptionController
      |
      v
TranscriptionService -----> OpenAI transcription API
```

The service interface separates the REST controller from the cloud provider and allows tests to use
local stubs. Java virtual threads handle blocking cloud I/O, while atomic counters protect shared
statistics.

## Run locally

```bash
read -s "OPENAI_API_KEY?Paste your OpenAI API key (hidden): "
echo
export OPENAI_API_KEY
./mvnw spring-boot:run
```

The hidden prompt keeps the secret out of the command itself. After stopping the server with
`Control+C`, run `unset OPENAI_API_KEY` to remove it from the current terminal session.

Open <http://localhost:8080> and allow microphone access when prompted.

## Build

```bash
./mvnw clean package
java -jar target/assignment1-0.0.1-SNAPSHOT.jar
```

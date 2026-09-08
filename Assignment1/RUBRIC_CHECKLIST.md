# Assignment 1 rubric evidence checklist

This checklist maps backend and concurrency requirements to concrete implementation evidence.

## REST API and speech-to-text

- [x] Spring REST upload route
- [x] OpenAI `/v1/audio/transcriptions` integration
- [x] Required `gpt-4o-mini-transcribe` model
- [x] Automatic transcription response
- [x] Input validation and shared error responses
- [x] YAML-compliant uptime, shutdown, and global-statistics routes
- [x] Controller and provider-boundary regression tests
- [ ] Real transcription using the runtime key

## Concurrency and Spring engineering

- [x] Java virtual threads for blocking cloud I/O
- [x] Tomcat capacity above 200 requests
- [x] Constructor injection through `TranscriptionService`
- [x] Thread-safe global counters
- [x] 240-thread shared-state regression test
- [x] 220-simultaneous-request embedded-server test
- [x] Graceful shutdown and draining implementation
- [ ] Confirm final behaviour in TITAN

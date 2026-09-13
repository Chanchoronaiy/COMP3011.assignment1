//THE RECORDING DESIGN IS MADE WITH THE HELP OF CHATGPT

"use strict";

// -------------------- PAGE ELEMENT REFERENCES --------------------
// references to HTML elements that JS needs to update
// querySelector connects JS variables to elements identified in index.html
const startButton = document.querySelector("#start-button");
const stopButton = document.querySelector("#stop-button");
const statusText = document.querySelector("#status-text");
const recordingTime = document.querySelector("#recording-time");
const errorMessage = document.querySelector("#error-message");
const transcription = document.querySelector("#transcription");
const resultState = document.querySelector("#result-state");

// -------------------- RECORDING CONFIGURATION --------------------
// time, timeout, and audio-quality limits 
const MAX_RECORDING_SECONDS = 59;
const UPLOAD_TIMEOUT_MILLISECONDS = 15_000;
const AUDIO_BITS_PER_SECOND = 32_000;

// -------------------- CHANGING RECORDING STATE --------------------
// These variables rmb the active recorder, audio, and timer between functions.
// hold changing state across the START, STOP and UPLOAD event handlers
let mediaRecorder;
let microphoneStream;
let audioChunks = [];
let timerId;
let recordingStartedAt;

// -------------------- RECORDING BUTTON EVENTS --------------------
startButton.addEventListener("click", startRecording);
stopButton.addEventListener("click", stopRecording);

// -------------------- START RECORDING --------------------
// requests microphone access and begins collecting compressed audio chunks
/**
 * requests microphone permission only after the user clicks. Browsers block microphone access
 * when it is attempted automatically during page loading.
 */
async function startRecording() {
    clearError();

    if (!navigator.mediaDevices?.getUserMedia || !window.MediaRecorder) {
        showError("This browser does not support audio recording. Please use a current browser.");
        return;
    }

    try {
        // 'Await' pauses this async function until permission succeeds or throws an error
        microphoneStream = await navigator.mediaDevices.getUserMedia({
            audio: {
                channelCount: 1,
                echoCancellation: true,
                noiseSuppression: true
            }
        });
        const mimeType = chooseSupportedMimeType();
        const options = { audioBitsPerSecond: AUDIO_BITS_PER_SECOND };
        if (mimeType) {
            options.mimeType = mimeType;
        }

        // Mono Opus at a speech-friendly bitrate reduces upload time and network usage
        mediaRecorder = new MediaRecorder(microphoneStream, options);
        audioChunks = [];

        // browser emits several small Blob chunks instead of keeping one large recording
        mediaRecorder.addEventListener("dataavailable", (event) => {
            if (event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });

        // final data available event happens immediately before stop
        mediaRecorder.addEventListener("stop", uploadRecording, { once: true });
        mediaRecorder.start(250);
        beginTimer();
        setPageState("recording", "Recording in progress");
    } catch (error) {
        releaseMicrophone();
        showError(microphoneErrorMessage(error));
        setPageState("ready", "Ready to record");
    }
}

// -------------------- STOP RECORDING --------------------
// STOPS the recorder, releases the microphone, changes the page to uploading
/**  Stops the recorder; its stop event then starts the automatic upload */
function stopRecording() {
    // Guard clauses safely leave the function when there is no active recorder to stop.
    if (!mediaRecorder || mediaRecorder.state === "inactive") {
        return;
    }

    stopButton.disabled = true;
    mediaRecorder.stop();
    stopTimer();
    releaseMicrophone();
    setPageState("uploading", "Transcribing your recording…");
}

// ----------- AUDIO UPLOAD AND TRANSCRIPTION DISPLAY --------------
// Sends the finished audio to Java and displays either the returned text or an error
/** Uploads the 'Blob' directly; the Java server adds the multipart wrapper required by OpenAI. */
async function uploadRecording() {
    const contentType = mediaRecorder.mimeType || audioChunks[0]?.type || "audio/webm";
    const recording = new Blob(audioChunks, { type: contentType });
    // AbortController prevents a failed network request from waiting forever.
    const abortController = new AbortController();
    const timeoutId = window.setTimeout(
        () => abortController.abort(),
        UPLOAD_TIMEOUT_MILLISECONDS);

    try {
        // fetch sends the recorded bytes to our Java controller, not directly to OpenAI
        const response = await fetch("/api/v1/record/upload", {
            method: "POST",
            headers: { "Content-Type": recording.type },
            body: recording,
            signal: abortController.signal
        });

        // A resolved fetch Promise can still contain an HTTP error, so response.ok is checked
        const responseBody = await parseJsonSafely(response);
        if (!response.ok) {
            throw new Error(
                responseBody.message
                || responseBody.error
                || "The server could not transcribe this recording.");
        }

        transcription.textContent = responseBody.text || "No speech was detected.";
        transcription.classList.add("has-result");
        resultState.textContent = "Transcription complete";
        setPageState("ready", "Ready to record again");
        transcription.focus();
    } catch (error) {
        const message = error.name === "AbortError"
            ? "Transcription took too long. Please try a shorter recording."
            : error.message;
        showError(message);
        resultState.textContent = "Transcription failed";
        setPageState("ready", "Ready to try again");
    } finally {
        // finally performs cleanup whether the upload succeeded, failed or timed out
        window.clearTimeout(timeoutId);
        audioChunks = [];
        mediaRecorder = undefined;
    }
}

// -------------------- AUDIO FORMAT SELECTION --------------------
// prefers small Opus recordings but falls back to formats supported by the browser
/** Selects only formats accepted by the assignment's transcription provider. */
function chooseSupportedMimeType() {
    const preferredTypes = [
        "audio/webm;codecs=opus",
        "audio/webm",
        "audio/ogg;codecs=opus",
        "audio/mp4"
    ];

    return preferredTypes.find((type) => MediaRecorder.isTypeSupported(type)) || "";
}

// -------------------- RECORDING TIMER --------------------
// displays elapsed time and automatically stops before the one-minute limit
function beginTimer() {
    recordingStartedAt = Date.now();
    recordingTime.hidden = false;
    updateTimer();
    timerId = window.setInterval(updateTimer, 250);
}

function updateTimer() {
    const seconds = Math.floor((Date.now() - recordingStartedAt) / 1000);
    recordingTime.textContent = `0:${String(seconds).padStart(2, "0")}`;

    // A small margin keeps the recording below the stated one-minute limit
    if (seconds >= MAX_RECORDING_SECONDS) {
        stopRecording();
    }
}

function stopTimer() {
    window.clearInterval(timerId);
    recordingTime.hidden = true;
}

// -------------------- MICROPHONE CLEANUP --------------------
// Releases the physical microphone as soon as it is no longer required
function releaseMicrophone() {
    // Stopping every track turns off the physical microphone indicator and releases the device.
    microphoneStream?.getTracks().forEach((track) => track.stop());
    microphoneStream = undefined;
}

// ------------ PAGE STATE AND BUTTON AVAILABILITY -------------
// Keeps the status message and enabled Start/Stop button in sync.
function setPageState(state, message) {
    // One state value controls both the visible CSS design and which actions are permitted.
    document.body.dataset.state = state;
    statusText.textContent = message;
    startButton.disabled = state !== "ready";
    stopButton.disabled = state !== "recording";
}

// ------------ ERROR DISPLAY -------------
// shows browser errors and CLEAR SOLD ERRORS before another attempt
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
    errorMessage.focus();
}

function clearError() {
    errorMessage.textContent = "";
    errorMessage.hidden = true;
}

function microphoneErrorMessage(error) {
    if (error.name === "NotAllowedError") {
        return "Microphone permission was denied. Allow access and try again.";
    }

    if (error.name === "NotFoundError") {
        return "No microphone was found on this device.";
    }

    return "The microphone could not be started. Please try again.";
}

// ------------ SAFE SERVER RESPONSE READING -------------
// Prevents a non-JSON proxy/server response from causing a second confusing error
async function parseJsonSafely(response) {
    try {
        return await response.json();
    } catch {
        // Not every server/proxy error includes JSON.  empty object lets the caller fall back
        return {};
    }
}

// -------------------- INITIAL PAGE STATE --------------------
// The page begins ready for its first recording.
setPageState("ready", "Ready to record");

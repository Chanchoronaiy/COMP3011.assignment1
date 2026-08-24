"use strict";

const startButton = document.querySelector("#start-button");
const stopButton = document.querySelector("#stop-button");
const statusText = document.querySelector("#status-text");
const errorMessage = document.querySelector("#error-message");
const transcription = document.querySelector("#transcription");
const resultState = document.querySelector("#result-state");

let mediaRecorder;
let microphoneStream;
let audioChunks = [];

startButton.addEventListener("click", startRecording);
stopButton.addEventListener("click", stopRecording);

async function startRecording() {
    errorMessage.hidden = true;

    try {
        microphoneStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        mediaRecorder = new MediaRecorder(microphoneStream);
        audioChunks = [];

        mediaRecorder.addEventListener("dataavailable", (event) => {
            if (event.data.size > 0) {
                audioChunks.push(event.data);
            }
        });
        mediaRecorder.addEventListener("stop", uploadRecording, { once: true });
        mediaRecorder.start();
        setPageState("recording", "Recording in progress");
    } catch {
        showError("The microphone could not be started. Please try again.");
    }
}

function stopRecording() {
    if (!mediaRecorder || mediaRecorder.state === "inactive") {
        return;
    }

    mediaRecorder.stop();
    microphoneStream.getTracks().forEach((track) => track.stop());
    setPageState("uploading", "Transcribing your recording…");
}

async function uploadRecording() {
    const contentType = mediaRecorder.mimeType || "audio/webm";
    const recording = new Blob(audioChunks, { type: contentType });

    try {
        const response = await fetch("/api/v1/record/upload", {
            method: "POST",
            headers: { "Content-Type": recording.type },
            body: recording
        });
        const responseBody = await response.json();

        if (!response.ok) {
            throw new Error(responseBody.message || "The recording could not be transcribed.");
        }

        transcription.textContent = responseBody.text;
        resultState.textContent = "Transcription complete";
        setPageState("ready", "Ready to record again");
    } catch (error) {
        showError(error.message);
        setPageState("ready", "Ready to try again");
    }
}

function setPageState(state, message) {
    document.body.dataset.state = state;
    statusText.textContent = message;
    startButton.disabled = state !== "ready";
    stopButton.disabled = state !== "recording";
}

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = false;
}

setPageState("ready", "Ready to record");

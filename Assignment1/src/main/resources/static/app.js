"use strict";

const startButton = document.querySelector("#start-button");
const stopButton = document.querySelector("#stop-button");
const statusText = document.querySelector("#status-text");

let mediaRecorder;
let microphoneStream;
let audioChunks = [];

startButton.addEventListener("click", startRecording);
stopButton.addEventListener("click", stopRecording);

async function startRecording() {
    microphoneStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    mediaRecorder = new MediaRecorder(microphoneStream);
    audioChunks = [];

    mediaRecorder.addEventListener("dataavailable", (event) => {
        if (event.data.size > 0) {
            audioChunks.push(event.data);
        }
    });

    mediaRecorder.start();
    document.body.dataset.state = "recording";
    statusText.textContent = "Recording in progress";
    startButton.disabled = true;
    stopButton.disabled = false;
}

function stopRecording() {
    mediaRecorder.stop();
    microphoneStream.getTracks().forEach((track) => track.stop());
    statusText.textContent = "Recording stopped";
    startButton.disabled = false;
    stopButton.disabled = true;
}

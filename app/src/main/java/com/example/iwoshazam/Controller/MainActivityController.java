package com.example.iwoshazam.Controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.iwoshazam.R;
import com.skyfishjy.library.RippleBackground;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.example.iwoshazam.Model.RecognizedSongModel;
import com.example.iwoshazam.Model.ShazamModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MainActivityController extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST = 1;
    private static final String LOG_TAG = "AudioRecordTest";
    private RippleBackground rippleBackground;
    private ShazamModel shazamModel;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;

    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private String filename;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private int bufferSize = 0;
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write((byte) (value >> 0));
        output.write((byte) (value >> 8));
        output.write((byte) (value >> 16));
        output.write((byte) (value >> 24));
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write((byte) (value >> 0));
        output.write((byte) (value >> 8));
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }
    private class RecognizeSongTask extends AsyncTask<String, Void, String> {



        @Override
        protected String doInBackground(String... params) {
            try {
                String rawFilePath = params[0];
                return shazamModel.recognizeSong(rawFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String songInfoJson) {
            if (songInfoJson != null) {
                // Parse the JSON response
                JsonParser parser = new JsonParser();
                JsonObject json = (JsonObject) parser.parse(songInfoJson);

                // Extract song title and artist from the JSON response
                JsonObject track = json.getAsJsonObject("track");
                String songTitle = track.get("title").getAsString();
                String songArtist = track.get("subtitle").getAsString();
                String imageURL = track.get("images").getAsJsonObject().get("coverart").getAsString();
                String youtubeURL = "";
                try {
                    youtubeURL = track.get("url").getAsString();
                } catch (Exception ex) {
                    System.out.println("No Youtube URL found");
                }

                // Create the RecognizedSong object
                RecognizedSongModel recognizedSong = new RecognizedSongModel();
                recognizedSong.setTitle(songTitle);
                recognizedSong.setArtist(songArtist);
                recognizedSong.setCoverArt(imageURL);
                recognizedSong.setYoutubeUrl(youtubeURL);

                // Navigate to the RecognizedSongActivity
                Intent intent = new Intent(MainActivityController.this, RecognizedSongActivityController.class);
                intent.putExtra("recognizedSong", recognizedSong);
                startActivity(intent);
            }
        }
    }
    private String convertToRaw(String audioFilePath) throws IOException {
        String rawFilePath = audioFilePath.replace(".wav", ".raw");

        File inputFile = new File(audioFilePath);
        File outputFile = new File(rawFilePath);

        // Set up the input and output streams
        FileInputStream inputStream = new FileInputStream(inputFile);
        FileOutputStream outputStream = new FileOutputStream(outputFile);

        // Set the audio parameters
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        // Set up the AudioTrack for playback
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
        );

        // Create a buffer to read audio data from the input stream
        byte[] buffer = new byte[bufferSize];

        // Start the audio playback
        audioTrack.play();

        // Read audio data from the input stream and write it to the output stream
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            audioTrack.write(buffer, 0, bytesRead);
            outputStream.write(buffer, 0, bytesRead);
        }

        // Stop the audio playback and release resources
        audioTrack.stop();
        audioTrack.release();

        // Close the input and output streams
        inputStream.close();
        outputStream.close();

        return rawFilePath;
    }

    private void recognizeSong() throws IOException {
        // No need to convert to raw, since the initial audio file is already in raw format
        String rawFilePath = filename;

        File file = new File(rawFilePath);
        if (!file.exists()) {
            Log.e(LOG_TAG, "File does not exist: " + rawFilePath);
            return;
        }
        if (file.length() == 0) {
            Log.e(LOG_TAG, "File is empty: " + rawFilePath);
            return;
        }
        new RecognizeSongTask().execute(rawFilePath);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_view);

        shazamModel = new ShazamModel();

        rippleBackground = findViewById(R.id.content);

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, PERMISSIONS_REQUEST);
        }

        // Initialize button and set click listener
        Button findSongButton = findViewById(R.id.btnFindSong);

        findSongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "Button clicked, starting process...");
                // Start ripple animation
                rippleBackground.startRippleAnimation();

                // Start recording
                startRecording();

                // Stop recording after 6 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        stopRecording();

                        // Stop ripple animation
                        rippleBackground.stopRippleAnimation();
                    }
                }, 6000);

                // Start song recognition after a delay
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOG_TAG, "Starting song recognition...");
                        File file = new File(filename);
                        long fileSize = file.length();
                        Log.d(LOG_TAG, "File size: " + fileSize + " bytes");
                        Log.d(LOG_TAG, "File path: " + file.getAbsolutePath());
                        Log.d(LOG_TAG, "File name: " + file.getName());
                        String fileExtension = filename.substring(filename.lastIndexOf(".") + 1);
                        Log.d(LOG_TAG, "File extension: " + fileExtension);
                        Log.d(LOG_TAG, "File exists: " + file.exists());
                        try {
                            recognizeSong();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Failed to recognize song: ", e);
                        }
                    }
                }, 6500);
            }
        });
    }

    private void startRecording() {
        bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(LOG_TAG, "Invalid buffer size");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        recorder.startRecording();
        isRecording = true;

        // Start writing data to file on a new thread
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        filename = getExternalFilesDir(null).getAbsolutePath() + "/" + UUID.randomUUID().toString() + "_audio.raw";
        String tempFilename = getExternalFilesDir(null).getAbsolutePath() + "/" + UUID.randomUUID().toString() + "_temp.raw";


        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found for recording ", e);
        }

        if (null != os) {
            while (isRecording) {
                int read = recorder.read(data, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(data);
                        os.flush(); // flush to make sure all data is immediately written to file
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error saving recording ", e);
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing file ", e);
            }
            // At this point, tempFilename contains raw PCM data.
            // Now we'll convert it to WAV format and save as filename
            try {
                rawToWave(new File(tempFilename), new File(filename));
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error converting to WAV ", e);
            }
        }
    }

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {
        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            output.write(rawData); // audio data
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
    }
}

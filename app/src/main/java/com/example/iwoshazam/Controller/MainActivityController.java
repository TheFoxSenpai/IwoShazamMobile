package com.example.iwoshazam.Controller;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

                        // Navigate to the SavedSongListActivity when the button is clicked
                        Intent intent = new Intent(MainActivityController.this, SavedSongListActivityController.class);
                        startActivity(intent);
                    }
                }, 6000);
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "AudioRecord initialization failed");
            return;
        }

        recorder.startRecording();

        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();

                // Recognize the song
                try {
                    String songInfoJson = shazamModel.recognizeSong(filename);

                    // Parse the JSON response
                    JsonParser parser = new JsonParser();
                    JsonObject json = (JsonObject) parser.parse(songInfoJson);

                    // Extract song title and artist from the JSON response
                    JsonObject track = json.getAsJsonObject("track");
                    String songTitle = track.get("title").getAsString();
                    String songArtist = track.get("subtitle").getAsString();
                    String imageURL = track.get("images").getAsJsonObject().get("coverart").getAsString();

                    // Create the RecognizedSong object
                    RecognizedSongModel recognizedSong = new RecognizedSongModel();
                    recognizedSong.setTitle(songTitle);
                    recognizedSong.setArtist(songArtist);

                    // Navigate to the RecognizedSongActivity
                    Intent intent = new Intent(MainActivityController.this, RecognizedSongActivityController.class);
                    intent.putExtra("recognizedSong", recognizedSong);
                    startActivity(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        filename = getExternalCacheDir().getAbsolutePath() + "/" + UUID.randomUUID().toString() + "audio.raw";
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

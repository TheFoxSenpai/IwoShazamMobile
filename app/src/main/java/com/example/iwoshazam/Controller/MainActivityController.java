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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
    public static void debugLongLog(String response){
        if(response.length() > 4000) {
            Log.d(LOG_TAG, "sb.length = " + response.length());
            int chunkCount = response.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if(max >= response.length()) {
                    Log.d(LOG_TAG, "chunk " + i + " of " + chunkCount + ":" + response.substring(4000 * i));
                } else {
                    Log.d(LOG_TAG, "chunk " + i + " of " + chunkCount + ":" + response.substring(4000 * i, max));
                }
            }
        } else {
            Log.d(LOG_TAG, response.toString());
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
                // Needed add this cuz JSON is to long for debbug
                debugLongLog(songInfoJson);

                // Parse the JSON response
                JsonParser parser = new JsonParser();
                JsonObject json = (JsonObject) parser.parse(songInfoJson);

                // Extract info from the JSON response
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

                String songLyrics = "";
                JsonArray sectionsArray = track.getAsJsonArray("sections");
                if (sectionsArray != null) {
                    Log.d(LOG_TAG, "Section array size: " + sectionsArray.size());
                    for (JsonElement sectionElement : sectionsArray) {
                        JsonObject sectionObject = sectionElement.getAsJsonObject();
                        if (sectionObject.get("type").getAsString().equals("LYRICS")) {
                            JsonArray lyricsArray = sectionObject.getAsJsonArray("text");
                            Log.d(LOG_TAG, "Lyrics array size: " + lyricsArray.size());
                            for (JsonElement lyricElement : lyricsArray) {
                                String lyric = lyricElement.getAsString();
                                Log.d(LOG_TAG, "Lyric: " + lyric);
                                songLyrics += lyric + "\n";
                            }
                        }
                    }
                }
                Log.d(LOG_TAG, "Song lyrics: " + songLyrics);
                // Create the RecognizedSong object
                RecognizedSongModel recognizedSong = new RecognizedSongModel();
                recognizedSong.setTitle(songTitle);
                recognizedSong.setArtist(songArtist);
                recognizedSong.setCoverArt(imageURL);
                recognizedSong.setYoutubeUrl(youtubeURL);
                recognizedSong.setLyrics(songLyrics);

                // Navigate to the RecognizedSongActivity
                Intent intent = new Intent(MainActivityController.this, RecognizedSongActivityController.class);
                intent.putExtra("recognizedSong", recognizedSong);
                startActivity(intent);
            }
        }
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
package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iwoshazam.Model.FirebaseModel;
import com.example.iwoshazam.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class LyricsActivityController extends AppCompatActivity {

    private FirebaseModel firebaseModel;
    private static final String TAG = "LyricsActivityController";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics_view);

        firebaseModel = new FirebaseModel();

        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.d(TAG, "FirebaseApp not initialized");
        } else {
            Log.d(TAG, "FirebaseApp is initialized");
        }

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d(TAG, "Connected to Firebase Database");
                } else {
                    Log.d(TAG, "Not connected to Firebase Database");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "Listener was cancelled");
            }
        });

        TextView lyricsTextView = findViewById(R.id.lyricsTextView);
        String lyrics = getIntent().getStringExtra("lyrics");
        lyricsTextView.setText(lyrics);

        Button skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LyricsActivityController.this, MainActivityController.class);
                startActivity(intent);
                finish();
            }
        });

        Button saveLyricsButton = findViewById(R.id.saveLyricsButton);
        saveLyricsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Save the lyrics to Firebase
                String songName = getIntent().getStringExtra("songName");
                songName = FirebaseModel.sanitizePath(songName); // Sanitize the song name before saving
                firebaseModel.saveSongLyrics(songName, lyrics);

                // Start the SavedSongListActivityController after saving the lyrics
                Intent intent = new Intent(LyricsActivityController.this, SavedSongListActivityController.class);
                startActivity(intent);
                finish();
            }
        });

        // Retrieve the songName from the intent
        String songName = getIntent().getStringExtra("songName");

        // Retrieve the lyrics for the selected song from Firebase
        firebaseModel.getSongLyrics(songName, new FirebaseModel.FirebaseCallback() {
            @Override
            public void onLyricsCallback(String lyrics) {
                if (lyrics != null) {
                    lyricsTextView.setText(lyrics);
                    Log.d(TAG, "Lyrics retrieved successfully: " + lyrics);
                } else {
                    // Handle the case when the lyrics retrieval fails
                    Log.d(TAG, "Failed to retrieve lyrics");
                }
            }

            @Override
            public void onSongNamesCallback(ArrayList<String> songNames) {
                // Not used in this context, so no implementation is required
            }
        });
    }
}

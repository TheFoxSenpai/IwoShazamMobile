package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iwoshazam.Model.RecognizedSongModel;
import com.example.iwoshazam.R;

public class RecognizedSongActivityController extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognized_song_view);

        // Get the recognized song
        Intent intent = getIntent();
        RecognizedSongModel recognizedSong = (RecognizedSongModel) intent.getSerializableExtra("recognizedSong");

        // Set the song information to the TextViews
        TextView titleTextView = findViewById(R.id.titleTextView);
        titleTextView.setText(recognizedSong.getTitle());

        TextView artistTextView = findViewById(R.id.artistTextView);
        artistTextView.setText(recognizedSong.getArtist());
    }
}

package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
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
        titleTextView.setText("Song Title: " + recognizedSong.getTitle());

        TextView artistTextView = findViewById(R.id.artistTextView);
        artistTextView.setText("Artist Name: " + recognizedSong.getArtist());

        // Set the song image to the ImageView
        ImageView imageView = findViewById(R.id.imageView);
        Glide.with(this)
                .load(recognizedSong.getCoverArt())
                .into(imageView);


        // Set the icon to the ImageView
        ImageView iconImageView = findViewById(R.id.iconImageView);
        Glide.with(this)
                .load(R.drawable.logo1)
                .into(iconImageView);
        iconImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(recognizedSong.getYoutubeUrl()));
                startActivity(browserIntent);
            }
        });

        Button saveButton = findViewById(R.id.showButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecognizedSongActivityController.this, LyricsActivityController.class);
                intent.putExtra("songName", recognizedSong.getTitle()); // Pass the song name as an extra
                intent.putExtra("lyrics", recognizedSong.getLyrics());
                startActivity(intent);
            }
        });


    }
}
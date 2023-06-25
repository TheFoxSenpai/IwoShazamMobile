package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iwoshazam.R;

public class LyricsActivityController extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics_view);

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
                // TODO: Implement save lyrics logic
            }
        });
    }
}

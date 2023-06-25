package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.iwoshazam.R;

public class Lyrics2ActivityController extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyrics2_view);

        TextView lyricsTextView = findViewById(R.id.lyricsTextView);
        String lyrics = getIntent().getStringExtra("lyrics");
        lyricsTextView.setText(lyrics);

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Lyrics2ActivityController.this, LyricsActivityController.class);
                startActivity(intent);
                finish();
            }
        });
    }
}

package com.example.iwoshazam.Controller;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.iwoshazam.R;

import java.util.ArrayList;

public class SavedSongListActivityController extends AppCompatActivity {
    private ListView songListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_song_list_view);

        songListView = findViewById(R.id.songListView);
        ArrayList<String> savedSongList = new ArrayList<>(); // Temporary ArrayList, replace it with your actual data from Firebase

        ArrayAdapter<String> itemsAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, savedSongList);

        songListView.setAdapter(itemsAdapter);

        //TODO: Get data from Firebase and populate the list view
    }
}

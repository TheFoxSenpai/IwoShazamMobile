package com.example.iwoshazam.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iwoshazam.Model.FirebaseModel;
import com.example.iwoshazam.R;

import java.util.ArrayList;

public class SavedSongListActivityController extends AppCompatActivity {

    private RecyclerView songRecyclerView;
    private FirebaseModel firebaseModel;
    private static final String TAG = "SavedSongListController";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_song_list_view);

        // Add the arrow button to the top left corner
        Button arrowButton = findViewById(R.id.arrowButton);
        arrowButton.setRotationY(180); // Rotate the button to create a mirrored reflection
        arrowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to MainActivityController
                Intent intent = new Intent(SavedSongListActivityController.this, MainActivityController.class);
                startActivity(intent);
            }
        });

        firebaseModel = new FirebaseModel();

        songRecyclerView = findViewById(R.id.songRecyclerView);
        ArrayList<String> savedSongList = new ArrayList<>();

        SavedSongListAdapter songListAdapter = new SavedSongListAdapter(savedSongList, this, firebaseModel);
        songRecyclerView.setLayoutManager(new LinearLayoutManager(this)); // Add this line
        songRecyclerView.setAdapter(songListAdapter);

        // Swipe to delete functionality
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String songName = savedSongList.get(position);
                savedSongList.remove(position);
                songListAdapter.notifyItemRemoved(position);

                // Delete the song from Firebase
                firebaseModel.deleteSong(songName);
            }
        });

        itemTouchHelper.attachToRecyclerView(songRecyclerView);

        // Retrieve the saved song names from Firebase and populate the list view
        firebaseModel.getSavedSongNames(new FirebaseModel.FirebaseCallback() {

            @Override
            public void onLyricsCallback(String lyrics) {
                // Not used in this context, so no implementation is required
            }

            @Override
            public void onSongNamesCallback(ArrayList<String> songNames) {
                if (songNames != null) {
                    savedSongList.clear();
                    savedSongList.addAll(songNames);
                    songListAdapter.notifyDataSetChanged();
                    Log.d(TAG, "Song names retrieved successfully: " + songNames);
                } else {
                    // Handle the case when the song names retrieval fails
                    Log.d(TAG, "Failed to retrieve song names");
                }
            }
        });
    }
}
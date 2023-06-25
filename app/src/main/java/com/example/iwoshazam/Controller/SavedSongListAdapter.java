package com.example.iwoshazam.Controller;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iwoshazam.Model.FirebaseModel;

import java.util.ArrayList;

public class SavedSongListAdapter extends RecyclerView.Adapter<SavedSongListAdapter.ViewHolder> {

    private ArrayList<String> songList;
    private Context context;
    private FirebaseModel firebaseModel;
    public SavedSongListAdapter(ArrayList<String> songList, Context context, FirebaseModel firebaseModel) {
        this.songList = songList;
        this.context = context;
        this.firebaseModel = firebaseModel;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView songTextView;

        public ViewHolder(View view) {
            super(view);
            songTextView = view.findViewById(android.R.id.text1);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String song = songList.get(position);
        holder.songTextView.setText(song);

        // Here you can add a click listener to handle song selection and go to the Lyrics2ActivityController
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the lyrics of the clicked song from Firebase
                firebaseModel.getSongLyrics(song, new FirebaseModel.FirebaseCallback() {
                    @Override
                    public void onLyricsCallback(String lyrics) {
                        if (lyrics != null) {
                            // Start Lyrics2ActivityController and pass the song's lyrics to it
                            Intent intent = new Intent(context, Lyrics2ActivityController.class);
                            intent.putExtra("lyrics", lyrics);
                            context.startActivity(intent);
                        } else {
                            // Handle the case when the lyrics retrieval fails
                            Log.d("SavedSongListAdapter", "Failed to retrieve lyrics for song: " + song);
                        }
                    }

                    @Override
                    public void onSongNamesCallback(ArrayList<String> songNames) {
                        // Not used in this context, so no implementation is required
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }
}


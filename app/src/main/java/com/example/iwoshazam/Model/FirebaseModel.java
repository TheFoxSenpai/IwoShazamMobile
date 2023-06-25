package com.example.iwoshazam.Model;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FirebaseModel {
    private static final String LYRICS_NODE = "lyrics";

    private DatabaseReference database;

    public FirebaseModel() {
        // Get a reference to the Firebase Realtime Database
        database = FirebaseDatabase.getInstance("https://iwoshazam-default-rtdb.europe-west1.firebasedatabase.app").getReference();
    }

    public static String sanitizePath(String path) {
        return path.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_");
    }
    public void getSongLyrics(String songName, FirebaseCallback firebaseCallback) {
        songName = sanitizePath(songName);

        database.child(LYRICS_NODE).child(songName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String lyrics = dataSnapshot.getValue(String.class);
                        firebaseCallback.onLyricsCallback(lyrics);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle error
                    }
                });
    }
    public void deleteSong(String songName) {
        database.child(LYRICS_NODE).child(songName).removeValue()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Successfully deleted song from database");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error deleting song from database", e);
                    }
                });
    }

    public void getSavedSongNames(final FirebaseCallback callback) {
        database.child(LYRICS_NODE).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> songNames = new ArrayList<>();
                for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                    String songName = childSnapshot.getKey();
                    songNames.add(songName);
                }
                callback.onSongNamesCallback(songNames);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle the error case if retrieval is unsuccessful
                callback.onSongNamesCallback(null);
            }
        });
    }

    public void saveSongLyrics(String songName, String lyrics) {
        database.child(LYRICS_NODE).child(songName).setValue(lyrics)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Write was successful!
                        Log.d(TAG, "Successfully wrote lyrics to database");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        Log.w(TAG, "Error writing lyrics to database", e);
                    }
                });
    }

    // Define a callback interface to handle the asynchronous retrieval of lyrics and song names
    public interface FirebaseCallback {
        void onLyricsCallback(String lyrics);
        void onSongNamesCallback(ArrayList<String> songNames);
    }
}

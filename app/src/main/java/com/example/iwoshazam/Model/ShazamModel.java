package com.example.iwoshazam.Model;

import android.util.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ShazamModel {
//add here you api key https://rapidapi.com/apidojo/api/shazam
    private final String API_KEY = "";

    public String recognizeSong(String songPath) throws IOException {
        File songFile = new File(songPath);
        byte[] songData;

        try (FileInputStream fis = new FileInputStream(songFile)) {
            songData = new byte[(int) songFile.length()];
            int offset = 0;
            int numRead;

            while (offset < songData.length && (numRead = fis.read(songData, offset, songData.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < songData.length) {
                throw new IOException("Failed to fully read file " + songFile.getName());
            }
        }

        String base64Data = Base64.encodeToString(songData, Base64.NO_WRAP);

        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = RequestBody.create(MediaType.parse("text/plain"), base64Data);
        Request request = new Request.Builder()
                .url("https://shazam.p.rapidapi.com/songs/detect")
                .header("x-rapidapi-key", API_KEY)
                .header("x-rapidapi-host", "shazam.p.rapidapi.com")
                .header("Content-Type", "text/plain")
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body().string();

            System.out.println("Status code: " + statusCode);
            System.out.println("Response body: " + responseBody);

            switch (statusCode) {
                case 200:
                    return responseBody;
                case 204:
                    throw new RuntimeException("The song could not be recognized.");
                case 413:
                    throw new RuntimeException("The provided file is too long.");
                default:
                    throw new RuntimeException("Failed to recognize song. Status code: " + statusCode);
            }
        }
    }
}

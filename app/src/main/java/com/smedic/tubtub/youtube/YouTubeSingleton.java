package com.smedic.tubtub.youtube;

import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.smedic.tubtub.R;
import com.smedic.tubtub.YTApplication;

import java.io.IOException;
import java.util.Arrays;

import static com.smedic.tubtub.utils.Auth.SCOPES;

/**
 * Created by smedic on 5.3.17..
 */
public class YouTubeSingleton {

    private static YouTube youTube;
    private static YouTube youTubeWithCredentials;
    private static GoogleAccountCredential credential;

    private static YouTubeSingleton ourInstance = new YouTubeSingleton();

    public static YouTubeSingleton getInstance() {
        return ourInstance;
    }

    private YouTubeSingleton() {


        credential = GoogleAccountCredential.usingOAuth2(
                YTApplication.getAppContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());


        youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {

            }
        }).setApplicationName(YTApplication.getAppContext().getString(R.string.app_name))
                .build();

        youTubeWithCredentials = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
                .setApplicationName(YTApplication.getAppContext().getString(R.string.app_name))
                .build();


        Log.d("kandabashi", "googleAccount!");
    }

    public static YouTube getYouTube() {
        return youTube;
    }

    public static YouTube getYouTubeWithCredentials() {
        return youTubeWithCredentials;
    }

    public static GoogleAccountCredential getCredential() {
        return credential;
    }
}

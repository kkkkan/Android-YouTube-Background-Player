/*
 * Copyright (C) 2017 kkkkan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kkkkan.youtube.tubtub.youtube;

import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import com.kkkkan.youtube.R;
import com.kkkkan.youtube.tubtub.YTApplication;

import java.io.IOException;
import java.util.Arrays;

import static com.kkkkan.youtube.tubtub.utils.Auth.SCOPES;

/**
 * Created by smedic on 5.3.17..
 */
public class YouTubeSingleton {

    private static final String TAG = "YouTubeSingleton";
    private static YouTube youTube;
    private static YouTube youTubeWithCredentials;
    private static GoogleAccountCredential credential;
    private static YouTubeSingleton ourInstance = new YouTubeSingleton();

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


        Log.d(TAG, "googleAccount!");
    }

    public static YouTubeSingleton getInstance() {
        return ourInstance;
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

package com.kkkkan.youtube.tubtub.youtube;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static com.kkkkan.youtube.tubtub.utils.Config.SUGGESTIONS_URL;

/**
 * Created by smedic on 13.2.17..
 */

public class SuggestionsLoader extends AsyncTaskLoader<List<String>> {

    private static final String TAG = "SMEDIC JsonAsyncTask";

    private static final int JSON_ERROR = 0;
    private static final int JSON_ARRAY = 1;
    private static final int JSON_OBJECT = 2;
    private final String suggestion;

    public SuggestionsLoader(Context context, String suggestion) {
        super(context);
        this.suggestion = suggestion;
    }

    @Override
    public List<String> loadInBackground() {

        //encode param to avoid spaces in URL
        String encodedParam = "";
        try {
            encodedParam = URLEncoder.encode(suggestion, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ArrayList<String> items = new ArrayList<>();
        try {
            URL url = new URL(SUGGESTIONS_URL + encodedParam);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            // gets the server json data
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream()));

            String next;
            while ((next = bufferedReader.readLine()) != null) {

                if (checkJson(next) == JSON_ERROR) {
                    //if not valid, remove invalid parts (this is simple hack for URL above)
                    next = next.substring(19, next.length() - 1);
                }

                JSONArray ja = new JSONArray(next);
                for (int i = 0; i < ja.length(); i++) {
                    if (ja.get(i) instanceof JSONArray) {
                        JSONArray ja2 = ja.getJSONArray(i);
                        for (int j = 0; j < ja2.length(); j++) {
                            if (ja2.get(j) instanceof JSONArray) {
                                String suggestion = ((JSONArray) ja2.get(j)).getString(0);
                                //Log.d(TAG, "Suggestion: " + suggestion);
                                items.add(suggestion);
                            }
                        }
                    } else if (ja.get(i) instanceof JSONObject) {
                        //Log.d(TAG, "json object");
                    } else {
                        //Log.d(TAG, "unknown object");
                    }
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return items;
    }

    @Override
    public void deliverResult(List<String> data) {
        if (isReset()) {
            // The Loader has been reset; ignore the result and invalidate the data.
            return;
        }
        super.deliverResult(data);
    }

    private int checkJson(String string) {
        try {
            Object json = new JSONTokener(string).nextValue();
            if (json instanceof JSONObject) {
                return JSON_OBJECT;
            } else if (json instanceof JSONArray) {
                return JSON_ARRAY;
            } else {
                return JSON_ERROR;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return JSON_ERROR;
        }
    }
}

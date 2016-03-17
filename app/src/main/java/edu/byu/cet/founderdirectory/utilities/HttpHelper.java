package edu.byu.cet.founderdirectory.utilities;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Liddle on 3/17/16.
 */
public class HttpHelper {

    private static final String TAG = HttpHelper.class.getSimpleName();

    private static String formatParameters(Map<String, String> parameters) throws UnsupportedEncodingException {
        // See http://bit.ly/1nROt5A for ideas on this pattern
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (String key : parameters.keySet()) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(parameters.get(key), "UTF-8"));
        }

        return result.toString();
    }

    public static String getContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            br.close();

            String result = sb.toString();
        } catch (MalformedURLException e) {
            // Ignore
            Log.d(TAG, "getContent: " + e);
        } catch (IOException e) {
            // Ignore
            Log.d(TAG, "getContent: " + e);
        }

        return "";
    }

    public static String postContent(String urlString, Map<String, String> parameters) {
        // See http://bit.ly/1nROt5A for ideas on this pattern

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            OutputStream outputStream = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(formatParameters(parameters));
            writer.flush();
            writer.close();
            outputStream.close();

            urlConnection.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            br.close();

            String result = sb.toString();
        } catch (MalformedURLException e) {
            // Ignore
            Log.d(TAG, "postContent: " + e);
        } catch (IOException e) {
            // Ignore
            Log.d(TAG, "postContent: " + e);
        }

        return "";
    }
}
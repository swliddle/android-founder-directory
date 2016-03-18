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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

/**
 * HTTP helper class.  Provides high-level interface to issue GET and POST commands.
 *
 * Created by Liddle on 3/17/16.
 */
public class HttpHelper {

    /**
     * Default string encoding.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Tag for logging.
     */
    private static final String TAG = HttpHelper.class.getSimpleName();

    /**
     * Format the given map of parameters into a string suitable for
     * sending as URL-encoded GET or POST parameters.
     *
     * @param parameters A map of parameters
     * @return A string representation of the GET query string or POST body
     * @throws UnsupportedEncodingException
     */
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

            result.append(URLEncoder.encode(key, DEFAULT_ENCODING));
            result.append("=");
            result.append(URLEncoder.encode(parameters.get(key), DEFAULT_ENCODING));
        }

        return result.toString();
    }

    /**
     * Use the GET method to process a given URL and return the server's response.
     *
     * @param urlString A string representation of a URL to GET
     * @return The server response string for the given URL
     */
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

            return sb.toString();
        } catch (IOException e) {
            // Ignore
            Log.d(TAG, "getContent: " + e);
        }

        return "";
    }

    /**
     * Use the POST method to process a given URL and return the server's response.
     *
     * @param urlString A string representation of a URL to POST
     * @param parameters A map of parameters to include in the POST body
     * @return The server response string for the given URL and POST parameters
     */
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

            return sb.toString();
        } catch (IOException e) {
            // Ignore
            Log.d(TAG, "postContent: " + e);
        }

        return "";
    }
}
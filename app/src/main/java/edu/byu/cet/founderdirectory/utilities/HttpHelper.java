package edu.byu.cet.founderdirectory.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HTTP helper class.  Provides high-level interface to issue GET and POST commands.
 *
 * Created by Liddle on 3/17/16.
 */
public class HttpHelper {

    private static final String BOUNDARY = "*****";

    /**
     * Default string encoding.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final String CRLF = "\r\n";

    private static final String TWO_HYPHENS = "--";

    /**
     * Tag for logging.
     */
    private static final String TAG = HttpHelper.class.getSimpleName();

    /**
     * Special context that allows us to use our self-signed SSL certificate.
     */
    private static SSLContext sSSLContext = null;

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

    public static Bitmap getBitmap(String urlString) {
        HttpsURLConnection urlConnection = null;
        Bitmap bitmap = null;

        try {
            URL url = new URL(urlString);
            urlConnection = getSecureConnection(url);
            int statusCode = urlConnection.getResponseCode();

            if (statusCode != HttpsURLConnection.HTTP_OK) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();

            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
            }
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            bitmap = null;
            Log.d(TAG, "getBitmap: " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return bitmap;
    }

    /**
     * Use the GET method to process a given URL and return the server's response.
     *
     * @param urlString A string representation of a URL to GET
     * @return The server response string for the given URL
     */
    public static String getContent(String urlString) {
        HttpsURLConnection urlConnection = null;
        StringBuilder sb = null;

        try {
            URL url = new URL(urlString);
            urlConnection = getSecureConnection(url);

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;

            sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            br.close();

        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.d(TAG, "getContent: " + e);
            sb = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return (sb != null) ? sb.toString() : "";
    }

    /**
     * Use the GET method to process a given URL and return the server's response.
     *
     * @param urlString A string representation of a URL to GET
     * @param parameters A map of parameters to be encoded on the GET query string
     * @return The server response string for the given URL
     */
    public static String getContent(String urlString, Map<String, String> parameters) {
        try {
            return getContent(urlString + "?" + formatParameters(parameters));
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "getContent encoding exception: " + e);
            return getContent(urlString);
        }
    }

    /**
     * Get a secure connection that uses our overly-trusting TrustManager so we can
     * authenticate with our self-signed certificate.
     *
     * @param url A URL to process
     * @return A secure connection
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private static HttpsURLConnection getSecureConnection(URL url) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        if (sSSLContext == null) {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });
        }

        return (HttpsURLConnection) url.openConnection();
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
        HttpsURLConnection urlConnection = null;
        StringBuilder sb = null;

        try {
            URL url = new URL(urlString);
            urlConnection = getSecureConnection(url);

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
            String line;

            sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            br.close();
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.d(TAG, "postContent: " + e);
            sb = null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return (sb != null) ? sb.toString() : "";
    }

    private static void addFormField(DataOutputStream dos, String fieldName, String value) throws IOException {
        dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
        dos.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"" + CRLF + CRLF);
        dos.writeBytes(value + CRLF);
    }

    public static String postMultipartContent(String urlString, Map<String, String> parameters, Bitmap bitmap) {
        HttpsURLConnection urlConnection = null;
        StringBuilder sb = null;

        try {
            DataOutputStream dos;
            URL url = new URL(urlString);

            urlConnection = getSecureConnection(url);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("ENCTYPE", "multipart/form-data");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);
            urlConnection.setRequestProperty("file", "founderphoto");

            dos = new DataOutputStream(urlConnection.getOutputStream());

            for (String key : parameters.keySet()) {
                addFormField(dos, key, parameters.get(key));
            }

            dos.writeBytes(TWO_HYPHENS + BOUNDARY + CRLF);
            dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"founderphoto\"" + CRLF + CRLF);

            if (bitmap != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                byte[] data = bos.toByteArray();
                dos.write(data, 0, data.length);
            }

            dos.writeBytes(CRLF);
            dos.writeBytes(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + CRLF);
            dos.flush();
            dos.close();

            urlConnection.connect();

            BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;

            sb = new StringBuilder();

            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            br.close();
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            sb = null;
            Log.d(TAG, "postMultipartContent: " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return (sb != null) ? sb.toString() : "";
    }

    public static void sendAsyncHttpMessage(final String message) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getContent(message);
                }
            }).start();
        } catch (Exception e) {
            // Ignore
        }
    }
}
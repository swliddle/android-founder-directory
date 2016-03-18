package edu.byu.cet.founderdirectory.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import edu.byu.cet.founderdirectory.LoginActivity;
import edu.byu.cet.founderdirectory.R;
import edu.byu.cet.founderdirectory.provider.FounderProvider;
import edu.byu.cet.founderdirectory.utilities.HttpHelper;

/**
 * Service to synchronize founder directory with server.
 *
 * Created by Liddle on 3/17/16.
 */
public class SyncService extends IntentService {

    /**
     * Default string encoding.
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * Maximum time this service has to live.  Because we relaunch the
     * service when the user launches the app, we'll proactively shut
     * ourselves down after this number of milliseconds.
     */
    private static final int MAX_LIVE_TIME = 2 * 60 * 60 * 1000;

    /**
     * Interval, in milliseconds, between sync polling requests.
     */
    private static final int POLL_INTERVAL = 5 * 60 * 1000;

    /**
     * Key for passing session token through the intent extras.
     */
    public static final String SESSION_TOKEN = "session";

    /**
     * Status indicating that the sync operation retrieved updates from the server.
     */
    private static final boolean SYNC_FOUND_SERVER_UPDATES = true;

    /**
     * ID of the sync notification, in case we want to update it.
     */
    private static final int SYNC_NOTIFICATION_ID = 42424242;

    /**
     * Base URL for synchronizing with server.
     */
    private static final String SYNC_SERVER_URL = "http://scriptures.byu.edu/founders/";

    /**
     * Tag for logging.
     */
    private static final String TAG = SyncService.class.getSimpleName();

    /**
     * Timestamp of last sync with server.
     */
    private long mLastSyncTime = 0;

    /**
     * When we get to this time, shut down the service.
     */
    private final long mMaxTime;

    /**
     * Session token for authentication with the server.
     */
    private String mSessionToken;

    /**
     * Default constructor
     */
    public SyncService() {
        super(SyncService.class.getSimpleName());

        mMaxTime = System.currentTimeMillis() + MAX_LIVE_TIME;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent: " + intent);

        mSessionToken = intent.getStringExtra(SESSION_TOKEN);

        // Note that because we extend IntentService, we're already on
        // a background thread.  We're not going to block the UI thread.

        while (mSessionToken != null && System.currentTimeMillis() < mMaxTime) {
            // Double-check that the interval has elapsed, in case of interrupted sleep.
            if (mLastSyncTime + POLL_INTERVAL < System.currentTimeMillis()) {
                if (synchronizeFounders() == SYNC_FOUND_SERVER_UPDATES) {
                    notifyUserOfSyncUpdates();
                }
            }

            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    /**
     * Tell the user we've synchronized data with the server.
     */
    private void notifyUserOfSyncUpdates() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        PendingIntent launchIntent = PendingIntent.getActivity(getApplicationContext(),
                0, new Intent(getApplicationContext(), LoginActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentTitle("Founder Directory")
               .setContentText("Updates to the Founder Directory are now available.")
               .setSmallIcon(R.drawable.rollins_logo_e_40)
               .setContentIntent(launchIntent)
               .setAutoCancel(true);

        manager.notify(SYNC_NOTIFICATION_ID, builder.build());
    }

    /**
     * Main driver for the synchronization process.
     */
    private boolean synchronizeFounders() {
        mLastSyncTime = System.currentTimeMillis();

        int maxVersion = maxFounderVersion();
        int serverMaxVersion = 0;

        // Note: In the production version, we won't let users delete
        //       or create founder records, only update.
        serverMaxVersion = syncDeletedFounders(serverMaxVersion);
        serverMaxVersion = syncNewFounders(serverMaxVersion);
        serverMaxVersion = syncDirtyFounders(serverMaxVersion);
        return syncServerFounderUpdates(maxVersion, serverMaxVersion);
    }

    /**
     * Map all the Founder data fields to their intermediate key for our server.
     * @return
     */
    private Map<String, String> allFieldsMap() {
        String[] allFieldNames = FounderProvider.Contract.allFieldsIdVersion();
        HashMap<String, String> allFields = new HashMap<>();
        int index = 0;

        for (String field : allFieldNames) {
            if (index > 0 && index < allFieldNames.length - 1) {
                allFields.put("f" + index, field);
            }

            ++index;
        }

        return allFields;
    }

    /**
     * Figure out what the highest version is that we know about.
     */
    private int maxFounderVersion() {
        Cursor maxVersionCursor = getContentResolver().query(FounderProvider.Contract.CONTENT_URI,
                new String[]{"MAX(" + FounderProvider.Contract.VERSION + ") as max_version"},
                null, null, null);
        int maxVersion = 0;

        if (maxVersionCursor.moveToFirst()) {
            maxVersion = maxVersionCursor.getInt(0);
        }

        maxVersionCursor.close();

        return maxVersion;
    }

    private int syncDeletedFounders(int serverMaxVersion) {
        Cursor deleted = getContentResolver().query(
                FounderProvider.Contract.CONTENT_URI,
                new String[]{FounderProvider.Contract._ID},
                FounderProvider.Contract.DELETED + " <> 0", null,
                FounderProvider.Contract.VERSION);

        if (deleted != null) {
            boolean success = deleted.moveToFirst();

            while (success) {
                int deletedId = deleted.getInt(0);

                try {
                    StringBuilder uriBuilder = new StringBuilder(SYNC_SERVER_URL);

                    uriBuilder.append("deletefounder.php");
                    uriBuilder.append("?k=" + mSessionToken);
                    uriBuilder.append("&i=" + deletedId);

                    String result = HttpHelper.getContent(uriBuilder.toString()).trim();
                    serverMaxVersion = Integer.parseInt(result);

                    if (!result.equals("0")) {
                        // Sync to delete on server worked, so remove from local database
                        getContentResolver().delete(FounderProvider.Contract.CONTENT_URI,
                                FounderProvider.Contract._ID + " = ?", new String[]{deletedId + ""});
                    }
                } catch (Exception e) {
                    Log.d(TAG, "syncDeletedFounders: unable to delete " + deletedId);
                }

                success = deleted.moveToNext();
            }

            deleted.close();
        }

        return serverMaxVersion;
    }

    private int syncDirtyFounders(int serverMaxVersion) {
        String[] founderFields = FounderProvider.Contract.allFieldsIdVersion();

        // Get dirty founders that are not deleted or new
        Cursor dirtyFounders = getContentResolver().query(
                FounderProvider.Contract.CONTENT_URI,
                founderFields,
                FounderProvider.Contract.DIRTY + " <> 0 and (" + FounderProvider.Contract.DELETED +
                        " is null or " + FounderProvider.Contract.DELETED + " <> 0) and (" +
                        FounderProvider.Contract.NEW + " is null or " + FounderProvider.Contract.NEW + " = 0)",
                null,
                FounderProvider.Contract.VERSION);

        if (dirtyFounders != null) {
            boolean success = dirtyFounders.moveToFirst();

            while (success) {
                try {
                    int dirtyId = dirtyFounders.getInt(dirtyFounders.getColumnIndexOrThrow(FounderProvider.Contract._ID));
                    String url = SYNC_SERVER_URL + "updatefounder.php";
                    Map<String, String> fieldKeyMap = allFieldsMap();
                    HashMap<String, String> parameters = new HashMap<>();

                    for (String field : fieldKeyMap.keySet()) {
                        parameters.put(field,
                                dirtyFounders.getString(dirtyFounders.getColumnIndexOrThrow(fieldKeyMap.get(field))));
                    }

                    parameters.put("k", mSessionToken);
                    parameters.put("i", dirtyId + "");
                    parameters.put("v", dirtyFounders.getInt(dirtyFounders.getColumnIndexOrThrow(FounderProvider.Contract.VERSION)) + "");

                    String result = HttpHelper.postContent(url, parameters).trim();
                    JSONObject serverUpdate = new JSONObject(result);

                    if (!result.equals("0")) {
                        // Sync to server worked, so replace in local database with updated values
                        ContentValues values = new ContentValues();

                        values.put(FounderProvider.Contract.NEW, FounderProvider.Contract.FLAG_EXISTING);
                        values.put(FounderProvider.Contract.DIRTY, FounderProvider.Contract.FLAG_CLEAN);

                        for (String field : founderFields) {
                            if ( !field.equalsIgnoreCase(FounderProvider.Contract.SERVER_ID) &&
                                 !field.equalsIgnoreCase(FounderProvider.Contract.DELETED) ) {
                                values.put(field, serverUpdate.getString(field));
                            }
                        }

                        serverMaxVersion = Integer.parseInt(serverUpdate.getString(FounderProvider.Contract.VERSION));

                        getContentResolver().update(FounderProvider.Contract.CONTENT_URI, values,
                                FounderProvider.Contract._ID + " = ?", new String[]{ dirtyId + "" });
                    }
                } catch (Exception e) {
                    Log.d(TAG, "syncDirtyFounders: unable to update dirty founder: " + e);
                }

                success = dirtyFounders.moveToNext();
            }

            dirtyFounders.close();
        }

        return serverMaxVersion;
    }

    private int syncNewFounders(int serverMaxVersion) {
        String[] founderFields = FounderProvider.Contract.allFieldsIdVersion();
        Cursor newFounders = getContentResolver().query(
                FounderProvider.Contract.CONTENT_URI,
                founderFields,
                FounderProvider.Contract.NEW + " = " + FounderProvider.Contract.FLAG_NEW, null,
                FounderProvider.Contract.VERSION);

        if (newFounders != null) {
            boolean success = newFounders.moveToFirst();

            while (success) {
                try {
                    int newId = newFounders.getInt(newFounders.getColumnIndexOrThrow(FounderProvider.Contract._ID));
                    String url = SYNC_SERVER_URL + "addfounder.php";
                    Map<String, String> fieldKeyMap = allFieldsMap();
                    HashMap<String, String> parameters = new HashMap<>();

                    for (String field : fieldKeyMap.keySet()) {
                        String value = newFounders.getString(newFounders.getColumnIndexOrThrow(fieldKeyMap.get(field)));

                        if (value == null || value.equalsIgnoreCase("null")) {
                            value = "";
                        }

                        parameters.put(field, value);
                    }

                    parameters.put("k", mSessionToken);

                    String result = HttpHelper.postContent(url, parameters).trim();
                    JSONObject serverNew = new JSONObject(result);

                    if (!result.equals("0")) {
                        // Sync to add on server worked, so replace in local database
                        ContentValues values = new ContentValues();

                        // TODO: There could be an issue here.  Make sure this ID doesn't already exist.
                        values.put(FounderProvider.Contract._ID, serverNew.getString(FounderProvider.Contract.SERVER_ID));
                        values.put(FounderProvider.Contract.NEW, FounderProvider.Contract.FLAG_EXISTING);
                        values.put(FounderProvider.Contract.DIRTY, FounderProvider.Contract.FLAG_CLEAN);
                        values.put(FounderProvider.Contract.VERSION, serverNew.getString(FounderProvider.Contract.VERSION));
                        serverMaxVersion = Integer.parseInt(serverNew.getString(FounderProvider.Contract.VERSION));

                        getContentResolver().update(
                                FounderProvider.Contract.CONTENT_URI,
                                values,
                                FounderProvider.Contract._ID + " = ?",
                                new String[] { newId + "" });
                    }
                } catch (Exception e) {
                    Log.d(TAG, "syncNewFounders: unable to update founder: " + e);
                    // Ignore
                }

                success = newFounders.moveToNext();
            }

            newFounders.close();
        }

        return serverMaxVersion;
    }

    private boolean syncServerFounderUpdates(int maxVersion, int serverMaxVersion) {
        boolean changesMade = false;

        try {
            // Ask the server for updates between our max at the beginning of the sync and
            // the new max on the server
            String query = SYNC_SERVER_URL + "getupdatessince.php?k=" + mSessionToken + "&v=" +
                    maxVersion + "&x=" + serverMaxVersion;
            Log.d(TAG, "syncServerFounderUpdates: url " + query);
            String result = HttpHelper.getContent(query);
            Log.d(TAG, "syncServerFounderUpdates: result " + result);
            JSONArray founders = new JSONArray(result);
            int len = founders.length();

            for (int i = 0; i < len; i++) {
                JSONObject founder = (JSONObject) founders.get(i);
                changesMade = true;

                if (founder.getString(FounderProvider.Contract.DELETED).equalsIgnoreCase(FounderProvider.Contract.FLAG_DELETED)) {
                    // We need to delete this founder
                    getContentResolver().delete(FounderProvider.Contract.CONTENT_URI,
                            FounderProvider.Contract._ID + " = ?",
                            new String[] { founder.getString(FounderProvider.Contract.SERVER_ID) });
                } else {
                    // We need to insert or update this founder
                    ContentValues values = new ContentValues();

                    for (String field : FounderProvider.Contract.allFieldsIdVersion()) {
                        if (!field.equalsIgnoreCase(FounderProvider.Contract._ID)) {
                            values.put(field, founder.getString(field));
                        }
                    }

                    values.put(FounderProvider.Contract._ID, founder.getString(FounderProvider.Contract.SERVER_ID));

                    // Attempt to update
                    int count = getContentResolver().update(FounderProvider.Contract.CONTENT_URI, values,
                            FounderProvider.Contract._ID + " = ?",
                            new String[] { founder.getString(FounderProvider.Contract.SERVER_ID) });

                    if (count <= 0) {
                        // If update failed, attempt to insert
                        getContentResolver().insert(FounderProvider.Contract.CONTENT_URI, values);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "syncServerFounderUpdates: " + e);
        }

        return changesMade;
    }
}
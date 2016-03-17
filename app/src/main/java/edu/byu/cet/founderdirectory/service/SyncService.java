package edu.byu.cet.founderdirectory.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

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
                synchronizeFounders();
                notifyUserOfSyncAttempt();
            }

            try {
                Thread.sleep(POLL_INTERVAL);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void notifyUserOfSyncAttempt() {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Founder Directory Sync")
                .setContentText("We've just synced with the server again.")
                .setSmallIcon(R.drawable.rollins_logo_e_40);

        manager.notify(0, builder.build());

    }

    /**
     * List of fields in the Founder record.
     * @return
     */
    private String[] allFieldsIdVersion() {
        return new String[] {
                FounderProvider.Contract._ID,
                FounderProvider.Contract.GIVEN_NAMES,
                FounderProvider.Contract.SURNAMES,
                FounderProvider.Contract.PREFERRED_FIRST_NAME,
                FounderProvider.Contract.PREFERRED_FULL_NAME,
                FounderProvider.Contract.CELL,
                FounderProvider.Contract.EMAIL,
                FounderProvider.Contract.WEB_SITE,
                FounderProvider.Contract.LINKED_IN,
                FounderProvider.Contract.BIOGRAPHY,
                FounderProvider.Contract.EXPERTISE,
                FounderProvider.Contract.SPOUSE_GIVEN_NAMES,
                FounderProvider.Contract.SPOUSE_SURNAMES,
                FounderProvider.Contract.SPOUSE_PREFERRED_FIRST_NAME,
                FounderProvider.Contract.SPOUSE_PREFERRED_FULL_NAME,
                FounderProvider.Contract.SPOUSE_CELL,
                FounderProvider.Contract.SPOUSE_EMAIL,
                FounderProvider.Contract.STATUS,
                FounderProvider.Contract.YEAR_JOINED,
                FounderProvider.Contract.HOME_ADDRESS1,
                FounderProvider.Contract.HOME_ADDRESS2,
                FounderProvider.Contract.HOME_CITY,
                FounderProvider.Contract.HOME_STATE,
                FounderProvider.Contract.HOME_POSTAL_CODE,
                FounderProvider.Contract.HOME_COUNTRY,
                FounderProvider.Contract.ORGANIZATION_NAME,
                FounderProvider.Contract.JOB_TITLE,
                FounderProvider.Contract.WORK_ADDRESS1,
                FounderProvider.Contract.WORK_ADDRESS2,
                FounderProvider.Contract.WORK_CITY,
                FounderProvider.Contract.WORK_STATE,
                FounderProvider.Contract.WORK_POSTAL_CODE,
                FounderProvider.Contract.WORK_COUNTRY,
                FounderProvider.Contract.MAILING_ADDRESS1,
                FounderProvider.Contract.MAILING_ADDRESS2,
                FounderProvider.Contract.MAILING_CITY,
                FounderProvider.Contract.MAILING_STATE,
                FounderProvider.Contract.MAILING_POSTAL_CODE,
                FounderProvider.Contract.MAILING_COUNTRY,
                FounderProvider.Contract.MAILING_SAME_AS,
                FounderProvider.Contract.IMAGE_URL,
                FounderProvider.Contract.SPOUSE_IMAGE_URL,
                FounderProvider.Contract.VERSION
        };
    }

    /**
     * Map all the Founder data fields to their intermediate key for our server.
     * @return
     */
    private Map<String, String> allFieldsMap() {
        String[] allFieldNames = allFieldsIdVersion();
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
                                FounderProvider.Contract._ID + " = ?", new String[] { deletedId + "" });
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
        String[] founderFields = allFieldsIdVersion();

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
                        parameters.put(fieldKeyMap.get(field),
                                dirtyFounders.getString(dirtyFounders.getColumnIndexOrThrow(field)));
                    }

                    parameters.put("k", mSessionToken);
                    parameters.put("i", dirtyId + "");
                    parameters.put("v", dirtyFounders.getInt(dirtyFounders.getColumnIndexOrThrow(FounderProvider.Contract.VERSION)) + "");

                    String result = HttpHelper.postContent(url, parameters).trim();
                    JSONObject serverUpdate = new JSONObject(result);

                    if (!result.equals("0")) {
                        // Sync to server worked, so replace in local database with updated values
                        ContentValues values = new ContentValues();

                        values.put(FounderProvider.Contract.NEW, 0);
                        values.put(FounderProvider.Contract.DIRTY, 0);

                        for (String field : founderFields) {
                            if (!field.equalsIgnoreCase("id") && !field.equalsIgnoreCase("deleted")) {
                                values.put(field, serverUpdate.getString(field));
                            }
                        }

                        serverMaxVersion = Integer.parseInt(serverUpdate.getString(FounderProvider.Contract.VERSION));

                        getContentResolver().update(FounderProvider.Contract.CONTENT_URI, values,
                                FounderProvider.Contract._ID + " = ?", new String[]{dirtyId + ""});
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

    private void synchronizeFounders() {
        mLastSyncTime = System.currentTimeMillis();

        int maxVersion = maxFounderVersion();
        int serverMaxVersion = 0;

        // Note: In the production version, we won't let users delete
        //       or create founder records, only update.
        serverMaxVersion = syncDeletedFounders(serverMaxVersion);
        serverMaxVersion = syncNewFounders(serverMaxVersion);
        serverMaxVersion = syncDirtyFounders(serverMaxVersion);
        syncServerFounderUpdates(maxVersion, serverMaxVersion);
    }

    private int syncNewFounders(int serverMaxVersion) {
        // NEEDSWORK: implement this one

        return serverMaxVersion;
    }

    private void syncServerFounderUpdates(int maxVersion, int serverMaxVersion) {
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

                if (founder.getString(FounderProvider.Contract.DELETED).equalsIgnoreCase("1")) {
                    // We need to delete this founder
                    getContentResolver().delete(FounderProvider.Contract.CONTENT_URI,
                            FounderProvider.Contract._ID + " = ?",
                            new String[] { founder.getString("id") });
                } else {
                    // We need to insert or update this founder
                    ContentValues cv = new ContentValues();

                    for (String field : allFieldsIdVersion()) {
                        if (!field.equalsIgnoreCase(FounderProvider.Contract._ID)) {
                            cv.put(field, founder.getString(field));
                        }
                    }

                    cv.put(FounderProvider.Contract._ID, founder.getString("id"));

                    // Attempt to update
                    int count = getContentResolver().update(FounderProvider.Contract.CONTENT_URI, cv,
                            FounderProvider.Contract._ID + " = ?", new String[] { founder.getString("id") });

                    if (count <= 0) {
                        // If update failed, attempt to insert
                        getContentResolver().insert(FounderProvider.Contract.CONTENT_URI, cv);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "syncServerFounderUpdates: " + e);
        }
    }
}
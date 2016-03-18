package edu.byu.cet.founderdirectory.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * ContentProvider for the CET Founders Directory app.
 *
 * Created by Liddle on 2/10/16.
 */
public class FounderProvider extends ContentProvider {
    // URI matcher codes
    private static final int URI_MATCHER_FOUNDERS = 1;
    private static final int URI_MATCHER_FOUNDER_ID = 2;

    // MIME type codes
    private static final String MIME_COLLECTION = "vnd.android.cursor.dir/";
    private static final String MIME_ITEM = "vnd.android.cursor.item/";
    private static final String MIME_BASE = "vnd.helloandroid.";

    /**
     * Handle to our underlying database.
     */
    private FounderDatabaseHelper mDatabase = null;

    /**
     * URI matcher to identify what kind of request we're receiving.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // A static initializer executes when the Java VM first loads this class.
    // We use it to perform one-time initialization of static members like sUriMatcher.
    static {
        sUriMatcher.addURI(Contract.AUTHORITY, Contract.FOUNDER, URI_MATCHER_FOUNDERS);
        sUriMatcher.addURI(Contract.AUTHORITY, Contract.FOUNDER + "/#", URI_MATCHER_FOUNDER_ID);
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] whereArgs) {
        return modify(uri, null, where, whereArgs);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            // This could be an if/else, but with more tables we'll want a switch instead.
            case URI_MATCHER_FOUNDER_ID:
                return MIME_ITEM + MIME_BASE + Contract.FOUNDER;
            default:
                return MIME_COLLECTION + MIME_BASE + Contract.FOUNDER;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        long rowId = mDatabase.getWritableDatabase().insert(tableForUri(uri), Contract.IMAGE_URL, initialValues);

        if (rowId <= 0) {
            throw new SQLException("Failed insert: " + uri);
        }

        // Now notify the resolver of the change so it can inform any listeners.
        Uri insertUri = ContentUris.withAppendedId(Contract.CONTENT_URI, rowId);
        Context context = getContext();

        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }

        return insertUri;
    }

    private int modify(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int count;
        SQLiteDatabase database = mDatabase.getWritableDatabase();
        String table = tableForUri(uri);

        // First attempt the delete or update operation.
        if (values == null) {
            // With no values this is a delete operation.
            count = database.delete(table, where, whereArgs);
        } else {
            count = database.update(table, values, where, whereArgs);
        }

        // Then notify the resolver of the change so it can inform any listeners.
        Context context = getContext();

        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }

        return count;
    }

    @Override
    public boolean onCreate() {
        // We delegate creation to the helper.
        mDatabase = new FounderDatabaseHelper(getContext());

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
        // A best practice is to use a query builder to construct an actual query from the URI.
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String orderBy = null;

        qb.setTables(tableForUri(uri));

        if (!TextUtils.isEmpty(sort)) {
            orderBy = sort;
        }

        if (sUriMatcher.match(uri) == URI_MATCHER_FOUNDER_ID) {
            qb.appendWhere(Contract._ID + "=" + uri.getLastPathSegment());
        }

        // Note that we're not really performing the query per se, but rather building a Cursor that will
        // iterate over all the query results.  We can use a read-only database here.
        Cursor cursor = qb.query(mDatabase.getReadableDatabase(), projection, selection, selectionArgs, null, null, orderBy);

        // The cursor needs to know about the resolver so it can be informed of any changes while
        // the cursor is active because changes could impact cursor results.
        Context context = getContext();

        if (context != null) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }

        return cursor;
    }

    /**
     * Determine which table we should use for a given URI.
     *
     * @param uri A URI corresponding to this provider
     * @return The table name needed for a CRUD operation based on this URI
     */
    private String tableForUri(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case URI_MATCHER_FOUNDERS:
            case URI_MATCHER_FOUNDER_ID:
            default:
                return Contract.FOUNDER;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] whereArgs) {
        return modify(uri, values, where, whereArgs);
    }

    /**
     * Database helper to create a CET Founders SQLite3 database.
     */
    public class FounderDatabaseHelper extends SQLiteOpenHelper {
        /**
         * Database filename.
         */
        private static final String DATABASE_NAME = "founders.db";

        /**
         * Database version.
         */
        private static final int DATABASE_VERSION = 5;

        /**
         * Normal constructor.
         *
         * @param context The context in which this content provider operates
         */
        public FounderDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        private void initDatabase(SQLiteDatabase db) {
            String create = "CREATE TABLE ";
            String idField = BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ";

            // Create the match table
            db.execSQL(create + Contract.FOUNDER + " (" + idField + //
                    Contract.GIVEN_NAMES + " TEXT, " + //
                    Contract.SURNAMES + " TEXT, " + //
                    Contract.PREFERRED_FIRST_NAME + " TEXT, " + //
                    Contract.PREFERRED_FULL_NAME + " TEXT, " + //
                    Contract.CELL + " TEXT, " + //
                    Contract.EMAIL + " TEXT, " + //
                    Contract.WEB_SITE + " TEXT, " + //
                    Contract.LINKED_IN + " TEXT, " + //
                    Contract.BIOGRAPHY + " TEXT, " + //
                    Contract.EXPERTISE + " TEXT, " + //
                    Contract.SPOUSE_GIVEN_NAMES + " TEXT, " + //
                    Contract.SPOUSE_SURNAMES + " TEXT, " + //
                    Contract.SPOUSE_PREFERRED_FIRST_NAME + " TEXT, " + //
                    Contract.SPOUSE_PREFERRED_FULL_NAME + " TEXT, " + //
                    Contract.SPOUSE_CELL + " TEXT, " + //
                    Contract.SPOUSE_EMAIL + " TEXT, " + //
                    Contract.STATUS + " TEXT, " + //
                    Contract.YEAR_JOINED + " TEXT, " + //
                    Contract.HOME_ADDRESS1 + " TEXT, " + //
                    Contract.HOME_ADDRESS2 + " TEXT, " + //
                    Contract.HOME_CITY + " TEXT, " + //
                    Contract.HOME_STATE + " TEXT, " + //
                    Contract.HOME_POSTAL_CODE + " TEXT, " + //
                    Contract.HOME_COUNTRY + " TEXT, " + //
                    Contract.ORGANIZATION_NAME + " TEXT, " + //
                    Contract.JOB_TITLE + " TEXT, " + //
                    Contract.WORK_ADDRESS1 + " TEXT, " + //
                    Contract.WORK_ADDRESS2 + " TEXT, " + //
                    Contract.WORK_CITY + " TEXT, " + //
                    Contract.WORK_STATE + " TEXT, " + //
                    Contract.WORK_POSTAL_CODE + " TEXT, " + //
                    Contract.WORK_COUNTRY + " TEXT, " + //
                    Contract.MAILING_ADDRESS1 + " TEXT, " + //
                    Contract.MAILING_ADDRESS2 + " TEXT, " + //
                    Contract.MAILING_CITY + " TEXT, " + //
                    Contract.MAILING_STATE + " TEXT, " + //
                    Contract.MAILING_POSTAL_CODE + " TEXT, " + //
                    Contract.MAILING_COUNTRY + " TEXT, " + //
                    Contract.MAILING_SAME_AS + " TEXT, " + //
                    Contract.IMAGE_URL + " TEXT, " + //
                    Contract.SPOUSE_IMAGE_URL + " TEXT, " + //
                    Contract.VERSION + " INTEGER, " + //
                    Contract.DELETED + " INTEGER, " + //
                    Contract.DIRTY + " INTEGER, " + //
                    Contract.NEW + " INTEGER " + //
                    ");");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            initDatabase(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < DATABASE_VERSION) {
                // NEEDSWORK: this is a very naive upgrade technique

                db.execSQL("DROP TABLE " + Contract.FOUNDER);
                initDatabase(db);
            }
        }
    }

    /**
     * This class is the contract that describes tables and fields in the
     * CET Founders database.
     */
    public static final class Contract implements BaseColumns {
        // Table names
        public static final String FOUNDER = "founder";

        // Founder fields
        // Also BaseColumns._ID here
        public static final String GIVEN_NAMES = "given_names";
        public static final String SURNAMES = "surnames";
        public static final String PREFERRED_FIRST_NAME = "preferred_first_name";
        public static final String PREFERRED_FULL_NAME = "preferred_full_name";
        public static final String CELL = "cell";
        public static final String EMAIL = "email";
        public static final String WEB_SITE = "web_site";
        public static final String LINKED_IN = "linked_in";
        public static final String BIOGRAPHY = "biography";
        public static final String EXPERTISE = "expertise";
        public static final String SPOUSE_GIVEN_NAMES = "spouse_given_names";
        public static final String SPOUSE_SURNAMES = "spouse_surnames";
        public static final String SPOUSE_PREFERRED_FIRST_NAME = "spouse_preferred_first_name";
        public static final String SPOUSE_PREFERRED_FULL_NAME = "spouse_preferred_full_name";
        public static final String SPOUSE_CELL = "spouse_cell";
        public static final String SPOUSE_EMAIL = "spouse_email";
        public static final String STATUS = "status";
        public static final String YEAR_JOINED = "year_joined";
        public static final String HOME_ADDRESS1 = "home_address1";
        public static final String HOME_ADDRESS2 = "home_address2";
        public static final String HOME_CITY = "home_city";
        public static final String HOME_STATE = "home_state";
        public static final String HOME_POSTAL_CODE = "home_postal_code";
        public static final String HOME_COUNTRY = "home_country";
        public static final String ORGANIZATION_NAME = "organization_name";
        public static final String JOB_TITLE = "job_title";
        public static final String WORK_ADDRESS1 = "work_address1";
        public static final String WORK_ADDRESS2 = "work_address2";
        public static final String WORK_CITY = "work_city";
        public static final String WORK_STATE = "work_state";
        public static final String WORK_POSTAL_CODE = "work_postal_code";
        public static final String WORK_COUNTRY = "work_country";
        public static final String MAILING_ADDRESS1 = "mailing_address1";
        public static final String MAILING_ADDRESS2 = "mailing_address2";
        public static final String MAILING_CITY = "mailing_city";
        public static final String MAILING_STATE = "mailing_state";
        public static final String MAILING_POSTAL_CODE = "mailing_postal_code";
        public static final String MAILING_COUNTRY = "mailing_country";
        public static final String MAILING_SAME_AS = "mailing_same_as";
        public static final String IMAGE_URL = "image_url";
        public static final String SPOUSE_IMAGE_URL = "spouse_image_url";
        public static final String VERSION = "version";
        public static final String DELETED = "deleted";
        public static final String DIRTY = "dirty";
        public static final String NEW = "new";

        /**
         * The authority name for this ContentProvider.
         */
        public static final String AUTHORITY = "edu.byu.cet.founderdirectory.provider";

        /**
         * Main URI pattern for CET Founders content.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + FOUNDER);

        /**
         * Flag indicating this Founder record is not deleted.
         */
        public static final String FLAG_AVAILABLE = "0";

        /**
         * Flag indicating this Founder record is clean.
         */
        public static final String FLAG_CLEAN = "0";

        /**
         * Flag indicating this Founder record is marked as deleted.
         */
        public static final String FLAG_DELETED = "1";

        /**
         * Flag indicating this Founder record is dirty.
         */
        public static final String FLAG_DIRTY = "1";

        /**
         * Flag indicating this Founder record has been stored on the server.
         */
        public static final String FLAG_EXISTING = "0";

        /**
         * Flag indicating this Founder record is new (has not been stored on the server).
         */
        public static final String FLAG_NEW = "1";

        /**
         * Field name of the ID field on the server (needed for translation).
         */
        public static final String SERVER_ID = "id";

        /**
         * Gives an array of fields in the Founder record, including ID and version fields,
         * together with all content fields.
         *
         * @return List of fields in the Founder record.
         */
        public static String[] allFieldsIdVersion() {
            return new String[] {
                    _ID, GIVEN_NAMES, SURNAMES, PREFERRED_FIRST_NAME, PREFERRED_FULL_NAME,
                    CELL, EMAIL, WEB_SITE, LINKED_IN, BIOGRAPHY, EXPERTISE, SPOUSE_GIVEN_NAMES,
                    SPOUSE_SURNAMES, SPOUSE_PREFERRED_FIRST_NAME, SPOUSE_PREFERRED_FULL_NAME,
                    SPOUSE_CELL, SPOUSE_EMAIL, STATUS, YEAR_JOINED, HOME_ADDRESS1, HOME_ADDRESS2,
                    HOME_CITY, HOME_STATE, HOME_POSTAL_CODE, HOME_COUNTRY, ORGANIZATION_NAME,
                    JOB_TITLE, WORK_ADDRESS1, WORK_ADDRESS2, WORK_CITY, WORK_STATE,
                    WORK_POSTAL_CODE, WORK_COUNTRY, MAILING_ADDRESS1, MAILING_ADDRESS2,
                    MAILING_CITY, MAILING_STATE, MAILING_POSTAL_CODE, MAILING_COUNTRY,
                    MAILING_SAME_AS, IMAGE_URL, SPOUSE_IMAGE_URL, VERSION
            };
        }
    }
}
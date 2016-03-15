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
    public int delete(Uri uri, String where, String[] whereArgs) {
        return modify(uri, null, where, whereArgs);
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            // This could be an if/else, but with more tables we'll want a switch instead.
            case URI_MATCHER_FOUNDER_ID:
                return MIME_ITEM + MIME_BASE + Contract.FOUNDER;
            default:
                return MIME_COLLECTION + MIME_BASE + Contract.FOUNDER;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        long rowId = mDatabase.getWritableDatabase().insert(tableForUri(uri), Contract.IMAGE_URL, initialValues);

        if (rowId <= 0) {
            throw new SQLException("Failed insert: " + uri);
        }

        // Now notify the resolver of the change so it can inform any listeners.
        Uri insertUri = ContentUris.withAppendedId(Contract.CONTENT_URI, rowId);

        getContext().getContentResolver().notifyChange(uri, null);

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
        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public boolean onCreate() {
        // We delegate creation to the helper.
        mDatabase = new FounderDatabaseHelper(getContext());

        return mDatabase != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort) {
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
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

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
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
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
         * Normal constructor.
         *
         * @param context
         */
        public FounderDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String create = "CREATE TABLE ";
            String idField = BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ";

            // Create the match table
            db.execSQL(create + Contract.FOUNDER + " (" + idField + //
                    Contract.GIVEN_NAMES + " TEXT, " + //
                    Contract.SURNAMES + " TEXT, " + //
                    Contract.PREFERRED_NAME + " TEXT, " + //
                    Contract.CELL + " TEXT, " + //
                    Contract.EMAIL + " TEXT, " + //
                    Contract.SPOUSE_NAME + " TEXT, " + //
                    Contract.SPOUSE_CELL + " TEXT, " + //
                    Contract.SPOUSE_EMAIL + " TEXT, " + //
                    Contract.STATUS + " TEXT, " + //
                    Contract.YEAR_JOINED + " TEXT, " + //
                    Contract.HOME_ADDRESS1 + " TEXT, " + //
                    Contract.HOME_ADDRESS2 + " TEXT, " + //
                    Contract.HOME_CITY + " TEXT, " + //
                    Contract.HOME_STATE + " TEXT, " + //
                    Contract.HOME_ZIP + " TEXT, " + //
                    Contract.COMPANY_NAME + " TEXT, " + //
                    Contract.WORK_ADDRESS1 + " TEXT, " + //
                    Contract.WORK_ADDRESS2 + " TEXT, " + //
                    Contract.WORK_CITY + " TEXT, " + //
                    Contract.WORK_STATE + " TEXT, " + //
                    Contract.WORK_ZIP + " TEXT, " + //
                    Contract.PROFILE + " TEXT " + //
                    Contract.NETWORKING_INTERESTS + " TEXT " + //
                    Contract.FAMILY_PROFILE + " TEXT " + //
                    Contract.HOBBIES + " TEXT " + //
                    Contract.IMAGE_URL + " TEXT " + //
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Currently there is nothing to do.  But as our application evolves,
            // we may need to write modification statements to transform an existing
            // schema into a later version.  Note that you might also want to
            // implement onDowngrade, but only for special cases.
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
        public static final String PREFERRED_NAME = "preferred_name";
        public static final String CELL = "cell";
        public static final String EMAIL = "email";
        public static final String SPOUSE_NAME = "spouse_name";
        public static final String SPOUSE_CELL = "spouse_cell";
        public static final String SPOUSE_EMAIL = "spouse_email";
        public static final String STATUS = "status";
        public static final String YEAR_JOINED = "year_joined";
        public static final String HOME_ADDRESS1 = "home_address1";
        public static final String HOME_ADDRESS2 = "home_address2";
        public static final String HOME_CITY = "home_city";
        public static final String HOME_STATE = "home_state";
        public static final String HOME_ZIP = "home_zip";
        public static final String COMPANY_NAME = "company_name";
        public static final String WORK_ADDRESS1 = "work_address1";
        public static final String WORK_ADDRESS2 = "work_address2";
        public static final String WORK_CITY = "work_city";
        public static final String WORK_STATE = "work_state";
        public static final String WORK_ZIP = "work_zip";
        public static final String PROFILE = "profile";
        public static final String NETWORKING_INTERESTS = "net_interests";
        public static final String FAMILY_PROFILE = "family_profile";
        public static final String HOBBIES = "hobbies";
        public static final String IMAGE_URL = "image_url";

        /**
         * The authority name for this ContentProvider.
         */
        public static final String AUTHORITY = "edu.byu.cet.founderdirectory.provider";

        /**
         * Main URI pattern for CET Founders content.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + FOUNDER);
    }
}
/*
 * 	BaseTableProvider.java
 * 	internal-common
 *
 * 	Copyright (C) 2016, Webzilla Apps Inc. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of Webzilla Apps Incorporated and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to Webzilla Apps Incorporated and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from Webzilla Apps Incorporated.
 */

package data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.CrossProcessCursorWrapper;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static android.provider.BaseColumns._ID;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 04/10/2016
 */
@SuppressWarnings("WeakerAccess, unused")
final class DatabaseTable {

    /** Data column */
    public static final String DATA_COLUMN = "data";

    /** ID Selection */
    public static final String ID_SELECTION = _ID + "=?";
    /** WHERE ID Selection */
    public static final String ID_SELECTION_WHERE = "WHERE " + ID_SELECTION;

    /** Standard content-type prefix. */
    private static final String CONTENT_TYPE_PREFIX = "vnd.android.cursor.dir/vnd.";
    /** Standard content item-type prefix. */
    private static final String CONTENT_ITEM_TYPE_PREFIX = "vnd.android.cursor.item/vnd.";

    /** The directory content type. */
    public static final int TYPE_DIR = 0;
    /** The item content type. */
    public static final int TYPE_ITEM = 1;

    /** The desc sort order suffix. */
    @SuppressWarnings("unused")
    public static final String DESC_SORT_ORDER_SUFFIX = " DESC";
    /** The asc sort order suffix. */
    public static final String ASC_SORT_ORDER_SUFFIX = " ASC";

    /** Id selection script. */
    protected static final String WHERE_ID_SCRIPT = "WHERE " + ID_SELECTION;

    /** Common column prefix for unique values. */
    protected static final String UNIQUE = "UNIQUE";
    /** Common column prefix for non-null values. */
    protected static final String NOT_NULL = "NOT NULL";
    /** Common column prefix for unique values. */
    protected static final String NOT_NULL_UNIQUE = NOT_NULL + " " + UNIQUE;
    /** Common column prefix for key column. */
    private static final String PRIMARY_KEY_AUTOINCREMENT = "INTEGER PRIMARY KEY AUTOINCREMENT";

    /** Readable database. */
    private SQLiteDatabase mReadableDatabase = null;
    /** Writable database. */
    private SQLiteDatabase mWritableDatabase = null;

    /** Content type */
    private String mContentType = null;
    /** Content item type */
    private String mContentItemType = null;

    /** Internal uri matcher. */
    private final UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /** Allowed query parameters. */
    private final HashSet<String> mAllowedQueryParameters = new HashSet<String>()
    {{addAll(Arrays.asList(getSupportedQueryParams()));}};

    /** Provider writable columns. */
    public final Set<String> providerColumns;
    /** Sync writable columns. */
    public final Set<String> syncColumns;
    /** Write allowed only for sync adapter. */
    public final boolean onlyForSync;

    /** DB names. */
    public final String tableName, rowName;

    /** Insert statement */
    private SQLiteStatement mInsertStatement = null;
    /** Delete statement */
    private SQLiteStatement mDeleteStatement = null;
    /** Get statement */
    private SQLiteStatement mGetDataStatement = null;

    /** The script for show database. */
    private static final String TABLE_CREATE_SCRIPT =
        "CREATE TABLE '%s' (" + _ID + " INTEGER NOT NULL PRIMARY KEY%sUNIQUE, " + DATA_COLUMN + " BLOB NOT NULL);";

    /** The script for apply database. */
    private static final String TABLE_REMOVE_SCRIPT = "DROP TABLE IF EXISTS '%s';";

    /** The script for insert operation. */
    private static final String INSERT_SCRIPT = "INSERT INTO '%s' VALUES (?,?);";

    /** Preferences table */
    public final boolean preferences;

    /** Content uri for this table. */
    final Uri contentUri;

    /** Default constructor */
    @SuppressWarnings("unchecked")
    public DatabaseTable(@NonNull String name, boolean preferences, @NonNull Uri contentUri) {
        tableName = name; rowName = tableName.substring(0, tableName.length() - 1);
        onlyForSync = false; syncColumns = providerColumns = Collections.EMPTY_SET;
        this.contentUri = contentUri.buildUpon().appendPath(name).build();
        this.preferences = preferences;
    }

    /**
     * Add internal uri
     *
     * @param authority content provider authority
     */
    public final String addUris(@NonNull String authority) {
        final String[] parts = authority.split("\\.");
        final String companyName = parts.length > 1 ? parts[1] : parts[0];
        mUriMatcher.addURI(authority, tableName, TYPE_DIR);
        mUriMatcher.addURI(authority, tableName + "/#", TYPE_ITEM);
        mContentType = CONTENT_TYPE_PREFIX + companyName + "." + rowName;
        mContentItemType = CONTENT_ITEM_TYPE_PREFIX + companyName + "." + rowName;
        return tableName;
    }

    /**
     * @param uri source uri
     * @return is item-resource
     */
    public final boolean isItem(@NonNull Uri uri)
    {return getCode(uri) == DatabaseTable.TYPE_ITEM;}

    /**
     * @param uri source uri
     * @return code by uri
     */
    public final int getCode(@NonNull Uri uri)
    {return mUriMatcher.match(uri);}

    /** @return Allowed query parameters */
    public final @NonNull
    HashSet<String> getAllowedQueryParams()
    {return mAllowedQueryParameters;}

    /**
     * @param match match code
     * @return the mime type of resource
     */
    @Nullable
    public final String getType(int match) {
        switch (match) {
            case TYPE_DIR: return mContentType;
            case TYPE_ITEM: return mContentItemType;
            default: return null;
        }
    }

    /**
     * Query specific item by his id.
     *
     * @param id the row id
     * @param columns the query projection
     * @param signal the cancellation signal
     *
     * @return database cursor
     */
    @NonNull
    public Cursor query(@NonNull String id, @Nullable String[] columns, @Nullable
        CancellationSignal signal) {
        if (signal != null) signal.throwIfCanceled();  String sql = "SELECT ";
        sql = columns != null && columns.length != 0 ? appendColumns(sql, columns) : sql + "* ";
        sql = sql + "FROM " + tableName + " " + ID_SELECTION_WHERE;
        return rawQueryCompat(sql, new String[] {id}, signal);
    }

    /**
     * Back Compatibility.
     *
     * @param sql the sql script
     * @param args the sql arguments
     * @param signal the cancellation signal
     *
     * @return Database Cursor
     */
    private Cursor rawQueryCompat(String sql, String[] args, CancellationSignal signal)
    {return mReadableDatabase.rawQuery(sql, args, signal);}

    /**
     * Query any items by selection with sorting.
     *
     * @param select select sql-expression
     * @param selArg selection args for binding
     * @param sorting sort order of items
     * @param columns projection of query
     * @param signal cancellation signal
     *
     * @return database cursor
     */
    @NonNull
    public Cursor query
    (@Nullable String select, @Nullable String[] selArg, @Nullable
        String sorting,
                        @Nullable String[] columns, @Nullable
        CancellationSignal signal) {
        if (signal != null) signal.throwIfCanceled();
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(tableName); final String groupBy = null, having = null, limit = null;
        return queryCompat(queryBuilder, mReadableDatabase, columns, select, selArg, groupBy, having, sorting, limit, signal);
    }

    /** Common compatibility. */
    private Cursor queryCompat(SQLiteQueryBuilder queryBuilder, SQLiteDatabase db,
                               String[] columns, String select, String[] selArg,
                               String groupBy, String having, String sorting, String limit,
                               CancellationSignal signal)
    {return queryBuilder.query(getReadableDatabase(), columns, select, selArg, groupBy, having, sorting, limit, signal);}


    /**
     * Add the names that are non-null in columns to s, separating them with commas.
     *
     * @param sqlQuery source sql-query string
     * @param columns projection of query
     *
     * @return new query string with appended projection
     **/
    @NonNull
    private static String appendColumns(@NonNull String sqlQuery, @NonNull String[] columns) {
        final StringBuilder builder = new StringBuilder(sqlQuery);
        for (int i = 0; i < columns.length; i++) {
            final String column = columns[i];
            if (column != null) {
                if (i > 0) builder.append(", ");
                builder.append(column);
            }
        }
        sqlQuery = builder.toString();
        builder.setLength(0);
        return sqlQuery + " ";
    }

    /**
     * Insert new item to table
     *
     * @param uri uri resource
     * @param values insert values
     *
     * @return id of inserted row
     */
    @NonNull
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (values == null) return uri; mInsertStatement.clearBindings();
        if (preferences) mInsertStatement.bindLong(1, values.getAsLong(_ID));
        mInsertStatement.bindBlob(2, values.getAsByteArray(DATA_COLUMN));
        final long result = mInsertStatement.executeInsert();
        if (result == -1) throw new RuntimeException("Error during insertion " + uri);
        return preferences ? uri : ContentUris.withAppendedId(uri, result);
    }

    /**
     * Delete item from table.
     *
     * @param id item id
     *
     * @return count of deleted
     */
    public int delete(@NonNull String id) {
        mDeleteStatement.clearBindings();
        mDeleteStatement.bindLong(1, Long.parseLong(id));
        return mDeleteStatement.executeUpdateDelete();
    }

    /**
     * Delete any items from table.
     *
     * @param select select sql-expression
     * @param selArg selection args for binding
     *
     * @return count of deleted
     */
    public int delete(@Nullable String select, @Nullable String[] selArg)
    {return getWritableDatabase().delete(tableName, select, selArg);}

    /**
     * Update item from table.
     *
     * @param id item id
     *
     * @return count of deleted
     */
    public int update(@NonNull String id, @NonNull ContentValues values)
    {return getWritableDatabase().update(tableName, values, ID_SELECTION, new String[]{id});}

    /**
     * Delete any items from table.
     *
     * @param select select sql-expression
     * @param selArg selection args for binding
     *
     * @return count of deleted
     */
    public int update(@Nullable String select, @Nullable String[] selArg, @NonNull
        ContentValues values)
    {return getWritableDatabase().update(tableName, values, select, selArg);}

    /**
     * @param id the row id
     * @return the parcel file descriptor
     */
    public ParcelFileDescriptor blobFileDescriptorForQuery(@NonNull String id)
    {return mGetDataStatement != null ? DatabaseUtils.blobFileDescriptorForQuery(mGetDataStatement, new String[]{id}) : null;}

    /** @return stream types */
    @NonNull public String[] getStreamTypes
    (int match, String mimeTypeFilter)
    {return new String[0];}

    /** @return Supported query parameters */
    @NonNull protected String[] getSupportedQueryParams()
    {return new String[0];}

    /**
     * @param readableSQLiteDatabase readable database instance
     * @param writableSQLiteDatabase writable database instance
     */
    public void onCreate
    (@NonNull SQLiteDatabase readableSQLiteDatabase, @NonNull
        SQLiteDatabase writableSQLiteDatabase) {
        mReadableDatabase = readableSQLiteDatabase; mWritableDatabase = writableSQLiteDatabase;
        mDeleteStatement = writableSQLiteDatabase.compileStatement(
                "DELETE FROM " + tableName + " " + ID_SELECTION_WHERE + ";");
        try {mGetDataStatement = writableSQLiteDatabase.compileStatement
            ("SELECT " + DATA_COLUMN + " FROM " + tableName + " " + ID_SELECTION_WHERE + " LIMIT 1;");}
        catch (SQLException ignored){}
        mInsertStatement = writableSQLiteDatabase.compileStatement(script(INSERT_SCRIPT));
    }

    /**
     * @param db database instance
     * @param old old version
     */
    public final void onPrepare(@NonNull SQLiteDatabase db, int old)
    {switch (old) {case  0: create(db); break;}}

    /** @param db database instance */
    private void create(@NonNull SQLiteDatabase db)
    {db.execSQL(String.format(Locale.US, TABLE_CREATE_SCRIPT, tableName,
        preferences ? " " : " AUTOINCREMENT "));}

    @NonNull private String script(@NonNull String pattern)
    {return String.format(Locale.US, pattern, tableName);}

    /** @return readable database */
    public final SQLiteDatabase getReadableDatabase()
    {return mReadableDatabase;}

    /** @return writable database */
    public final SQLiteDatabase getWritableDatabase()
    {return mWritableDatabase;}

    /** Destroy the helper */
    protected void onDestroy() {
        mDeleteStatement.close();
        mDeleteStatement = null;
        if (mGetDataStatement != null) {
            mGetDataStatement.close();
            mGetDataStatement = null;
        }
    }

    /**
     * @param arg     arguments
     * @param extras  extras
     *
     * @return result
     */
    @NonNull
    Bundle call(@Nullable String arg, @Nullable Bundle extras) {
      /*final String query = "SELECT EXISTS(SELECT 1 FROM " + tableName + " WHERE _id = ? LIMIT 1);";
      final Bundle result = new Bundle();
      result.putBoolean("result", DatabaseUtils.longForQuery(mReadableDatabase, query, new String[]{arg}) == 1);*/
      return Bundle.EMPTY;
    }

    /** The openable cursor factory. */
    private static final class OpenableCursorFactory implements SQLiteDatabase.CursorFactory {

        /** {@inheritDoc} */
        @Override
        public final Cursor newCursor(SQLiteDatabase db, SQLiteCursorDriver masterQuery, String editTable, SQLiteQuery query)
        {return new OpenableSQLiteCursor(masterQuery, editTable, query);}

        /** The openable cursor. */
        private static final class OpenableSQLiteCursor extends
            CrossProcessCursorWrapper {

            /**
             * Creates an {@link #OpenableSQLiteCursor(SQLiteCursorDriver, String, SQLiteQuery)}.
             *
             * @param editTable the name of the table used for this query
             * @param query the {@link SQLiteQuery} object associated with this cursor object.
             */
            @SuppressWarnings("JavaDoc")
            OpenableSQLiteCursor(SQLiteCursorDriver driver, String editTable, SQLiteQuery query)
            {super(new SQLiteCursor(driver, editTable, query));}

            /** {@inheritDoc} */
            @Override
            public final String getString(int columnIndex)
            {return super.getString(columnIndex);}
        }
    }

}

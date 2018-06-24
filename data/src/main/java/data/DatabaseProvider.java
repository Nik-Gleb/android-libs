package data;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static android.database.DatabaseUtils.dumpCursor;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2018
 */
final class DatabaseProvider extends SQLiteProvider {

  /** The log-cat tag */
  private static final String TAG = "DatabaseProvider";

  /** Keep database flag */
  private static final boolean KEEP_DATABASE_BY_CLOSE = true;

  /**
   * Arbitrary integer that we assign to the messages that we send to this
   * thread's handler, indicating that these are requests to send an apply
   * notification intent.
   */
  private static final int UPDATE_BROADCAST_MSG = 1;

  /**
   * Any requests to send a PROVIDER_CHANGED intent will be collapsed over
   * this window, to prevent spamming too many intents at once.
   */
  @SuppressWarnings("PointlessArithmeticExpression")
  private static final long UPDATE_BROADCAST_TIMEOUT_MILLIS = 1 * DateUtils.SECOND_IN_MILLIS;
  /** Sync apply broad cast timeout mills */
  private static final long SYNC_UPDATE_BROADCAST_TIMEOUT_MILLIS = 30 * DateUtils.SECOND_IN_MILLIS;

  /** The type of query transaction. */
  private static final int TRANSACTION_QUERY = 0;
  /** The type of insert transaction. */
  private static final int TRANSACTION_INSERT = 1;
  /** The type of apply transaction. */
  private static final int TRANSACTION_UPDATE = 2;
  /** The type of delete transaction. */
  private static final int TRANSACTION_DELETE = 3;

  /** The Parameters for SyncAdapter. */
  private static final HashSet<String> ALLOWED_QUERY_PARAMETERS = new HashSet<String>()
  {{add(CALLER_IS_SYNCADAPTER); add("reset");}};

  /** Common callback. */
  private final Callback mCallback = new Callback(this);


  /** Main handler */
  private Handler mHandler = new Handler(Looper.getMainLooper(), mCallback);

  /** The Content Resolver. */
  @Nullable private ContentResolver mContentResolver = null;

  /** Content uri */
  @NonNull private final Uri mContentUri;

  /** Database name. */
  @NonNull private final String mName;

  /** Database version. */
  private final int mVersion;

  /** Database tables. */
  @NonNull private final DatabaseTable[] mTables;

  /** The Uri Matcher. */
  private final UriMatcher mUriMatcher;


  /**
   * Constructs a new database provider
   *
   * @param context application context
   */
  DatabaseProvider(@NonNull Context context, @NonNull String authority,
      @NonNull String name, int version, @NonNull String[] tables) {
    super(context);
    mContentUri = new Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(authority).build();
    mName = name; mVersion = version;
    mTables = new DatabaseTable[tables.length];
    for (int i = 0; i < mTables.length; i++)
      mTables[i] = new DatabaseTable(tables[i], i == 0);

     mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH) {{
      for (int i = 0; i < mTables.length; i++) {
        final int index = i + 1;
        final DatabaseTable table = mTables[i];
        final String tableName = table.addUris(authority);
        addURI(authority, tableName, index);
        addURI(authority, tableName + "/#", index);
        addURI(authority, tableName + "/-#", index);
      }
    }};
  }

  @NonNull
  @Override
  public String[] getSupportedPaths() {
    final LinkedHashSet<String> paths = new LinkedHashSet<>();
    for (final DatabaseTable mTable : mTables) {
      paths.add(mTable.tableName);
      paths.add(mTable.tableName + "/#");
      paths.add(mTable.tableName + "/-#");
    }
    return paths.toArray(new String[paths.size()]);
  }

  /** Returns a {@link SQLiteOpenHelper} that can open the database. */
  @Override
  protected final SQLiteOpenHelper getDatabaseHelper(Context context)
  {return DatabaseHelper.getInstance(context, mName, mVersion, mTables);}

  /** {@inheritDoc} */
  @Override
  public final boolean onCreate() {
    final boolean result = super.onCreate();
    final SQLiteOpenHelper sqLiteOpenHelper = getDatabaseHelper(context);
    final SQLiteDatabase writableDatabase = sqLiteOpenHelper.getWritableDatabase();
    final SQLiteDatabase readableDatabase = sqLiteOpenHelper.getReadableDatabase();

    for (final DatabaseTable table : mTables)
      table.onCreate(writableDatabase, readableDatabase);

    mContentResolver = context.getContentResolver();
    return result;
  }

  /** {@inheritDoc} */
  @Override
  public final Bundle call
  (@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    final DatabaseTable table = getTableByUri(mContentUri.buildUpon()
        .appendEncodedPath(method).build()); return table.call(arg, extras);
  }

  /** {@inheritDoc} */
  @Override
  @NonNull
  public final Cursor query(@NonNull
      Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder)
  {return query(uri, projection, selection, selectionArgs, sortOrder, null);}

  /** {@inheritDoc} */
  @Override
  @NonNull
  public final Cursor query(@NonNull Uri uri, String[] projection,
      String selection, String[] selectionArgs,
      String sortOrder, CancellationSignal signal) {

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(Log.DEBUG, TAG, " >>> QUERY: " + uri + ", " + Arrays.toString(projection) + "; " +
          selection + " " + Arrays.toString(selectionArgs) + "; " + sortOrder);

    final DatabaseTable table = getTableByUri(uri); final boolean isItem = table.isItem(uri);
    validateQueryParameters(table.getAllowedQueryParams(), uri.getQueryParameterNames());
    verifyTransactionAllowed(TRANSACTION_QUERY, isItem, table, uri, null, selection, selectionArgs, false);

    final Cursor result = isItem ?
        table.query(uri.getLastPathSegment(), projection, signal):
        table.query(selection, selectionArgs, sortOrder, projection, signal);

    if (mContentResolver != null) result.setNotificationUri(mContentResolver, uri);

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(Log.DEBUG, TAG, " <<< QUERY: " + dumpCursorToString(result));

    return result;
  }

  /** The equivalent of the {@link #insert} method, but invoked within a transaction. */
  @Override
  protected final Uri insertInTransaction(@NonNull Uri uri, @Nullable
      ContentValues values,
      boolean callerIsSyncAdapter) {

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(
          Log.DEBUG, TAG, " >>> INSERT(" + (callerIsSyncAdapter ? "SYNC" : "APP") +
          "): " + uri + ", " + values);

    final DatabaseTable table = getTableByUri(uri); final boolean isItem = table.isItem(uri);
    validateQueryParameters(table.getAllowedQueryParams(), uri.getQueryParameterNames());
    verifyTransactionAllowed(TRANSACTION_INSERT, isItem, table, uri, values, null, null,
        callerIsSyncAdapter);

    //noinspection UnnecessaryLocalVariable
    final Uri result = table.insert(uri, values);
    sendUpdateNotification(result, callerIsSyncAdapter);

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(Log.DEBUG, TAG, " <<< INSERT(RESULT): " + result);

    return result;
  }

  /** The equivalent of the {@link #delete} method, but invoked within a transaction. */
  @Override
  protected final int deleteInTransaction(Uri uri, String selection, String[] selectionArgs,
      boolean callerIsSyncAdapter) {

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(
          Log.DEBUG, TAG, " >>> DELETE(" + (callerIsSyncAdapter ? "SYNC" : "APP") + "): " + uri + "; " +
          selection + " " + Arrays.toString(selectionArgs));

    final DatabaseTable table = getTableByUri(uri); final boolean isItem = table.isItem(uri);
    validateQueryParameters(table.getAllowedQueryParams(), uri.getQueryParameterNames());
    verifyTransactionAllowed(TRANSACTION_DELETE, isItem, table, uri, null,
        selection, selectionArgs, callerIsSyncAdapter);

    final int result = isItem ?
        table.delete(uri.getLastPathSegment()):
        table.delete(selection, selectionArgs);

    if (uri.getBooleanQueryParameter("reset", false))
      table.getWritableDatabase().execSQL
          ("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + table.tableName + "'");

    if (result != 0) sendUpdateNotification(uri, callerIsSyncAdapter);

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(Log.DEBUG, TAG, " <<< DELETE(RESULT): " + result);

    return result;
  }

  /** The equivalent of the {@link #update} method, but invoked within a transaction. */
  @Override
  protected final int updateInTransaction(Uri uri, ContentValues values,
      String selection, String[] selectionArgs,
      boolean callerIsSyncAdapter) {
    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(
          Log.DEBUG, TAG, " >>> UPDATE(" + (callerIsSyncAdapter ? "SYNC" : "APP") +
          "): " + uri + ", " + values + "; " + selection + " " + Arrays.toString(selectionArgs));

    final DatabaseTable table = getTableByUri(uri); final boolean isItem = table.isItem(uri);
    validateQueryParameters(table.getAllowedQueryParams(), uri.getQueryParameterNames());
    verifyTransactionAllowed(TRANSACTION_UPDATE, isItem, table, uri, values,
        selection, selectionArgs, callerIsSyncAdapter);

    final int result = isItem ?
        table.update(uri.getLastPathSegment(), values):
        table.update(selection, selectionArgs, values);

    if (result != 0) sendUpdateNotification(uri, callerIsSyncAdapter);

    //if (Log.isLoggable(TAG, Log.DEBUG))
      Log.println(Log.DEBUG, TAG, " <<< UPDATE(RESULT): " + result);

    return result;
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final String getType(@NonNull Uri uri) {
    final DatabaseTable table = getTableByUri(uri);
    return table.getType(table.getCode(uri));
  }

  /** {@inheritDoc} */
  @Override
  public String[] getStreamTypes(@NonNull Uri uri, @NonNull
      String mimeTypeFilter) {
    final DatabaseTable table = getTableByUri(uri);
    return table.getStreamTypes(table.getCode(uri), mimeTypeFilter);
  }

  /**
   * @param uri resource uri
   * @return table helper
   */
  @CheckResult @NonNull
  private DatabaseTable getTableByUri(@NonNull Uri uri) {
    try {return mTables[mUriMatcher.match(uri) - 1];}
    catch (IndexOutOfBoundsException e)
    {throw new IllegalArgumentException("Unknown uri: " + uri, e);}
  }

  /** Handle messages */
  private boolean handleMessage(Message msg) {
    switch (msg.what) {
      case UPDATE_BROADCAST_MSG:
        // Broadcast a provider changed intent
        sendUpdateNotification((Uri) msg.obj);
        // Because the handler does not guarantee message delivery in
        // the case that the provider is killed, we need to make sure
        // that the provider stays alive long enough to deliver the
        // notification. This empty service is sufficient to "wedge" the
        // process until we stop it here.
        try {context.stopService(new Intent(context, EmptyService.class));}
        catch (UnsupportedOperationException ignore) {}

        break;
    }
    return true;
  }

  /**
   * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
   * This also provides a timeout, so any calls to this method will be batched
   * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.
   *
   * @param callerIsSyncAdapter whether or not the apply is being triggered by a sync
   */
  @SuppressWarnings("unused")
  private void sendUpdateNotification(boolean callerIsSyncAdapter) {
    // We use -1 to represent an apply to all events
    sendUpdateNotification(mContentUri, callerIsSyncAdapter);
  }

  /**
   * Call this to trigger a broadcast of the ACTION_PROVIDER_CHANGED intent.
   * This also provides a timeout, so any calls to this method will be batched
   * over a period of BROADCAST_TIMEOUT_MILLIS defined in this class.  The
   * actual sending of the intent is done in
   * {@link #sendUpdateNotification(Uri)}.
   *
   * @param data dest uri
   * @param callerIsSyncAdapter whether or not the apply is being triggered by a sync
   */
  private void sendUpdateNotification(Uri data, boolean callerIsSyncAdapter) {
    postNotifyUri(data);

    // Are there any pending broadcast requests?
    if (mHandler.hasMessages(UPDATE_BROADCAST_MSG))
      // Delete any pending requests, before requiring a fresh one
      mHandler.removeMessages(UPDATE_BROADCAST_MSG);
    else {
      // Because the handler does not guarantee message delivery in
      // the case that the provider is killed, we need to make sure
      // that the provider stays alive long enough to deliver the
      // notification. This empty service is sufficient to "wedge" the
      // process until we stop it here.
      try {context.startService(new Intent(context, EmptyService.class));}
      catch (UnsupportedOperationException ignore) {}
    }
    // We use a much longer delay for sync-related updates, to prevent any
    // receivers from slowing down the sync
    final long delay = callerIsSyncAdapter ?
        SYNC_UPDATE_BROADCAST_TIMEOUT_MILLIS :
        UPDATE_BROADCAST_TIMEOUT_MILLIS;
    // Despite the fact that we actually only ever use one message at a time
    // for now, it is really important to call obtainMessage() to created a
    // clean instance.  This avoids potentially infinite loops resulting
    // adding the same instance to the message queue twice, since the
    // message queue implements its linked list using a field from Message.
    final Message msg = mHandler.obtainMessage(UPDATE_BROADCAST_MSG); msg.obj = data;
    mHandler.sendMessageDelayed(msg, delay);
    //mBroadcastHandler.sendMessage(msg);
  }

  /**
   * This method should not ever be called directly, to prevent sending too
   * many potentially expensive broadcasts.  Instead, call
   * {@link #sendUpdateNotification(boolean)} instead.
   *
   * @see #sendUpdateNotification(boolean)
   */
  private void sendUpdateNotification(Uri uri) {
    final Intent intent = new Intent(Intent.ACTION_PROVIDER_CHANGED);
    intent.setDataAndTypeAndNormalize(uri, getType(uri));
    try{context.sendBroadcast(intent, null);}
    catch (UnsupportedOperationException ignore) {}
  }

  /** @param queryParameterNames to validation */
  private static void validateQueryParameters(@NonNull
      HashSet<String> allowedQueryParameters,
      @NonNull Set<String> queryParameterNames) {
    for (String parameterName : queryParameterNames)
      if (!ALLOWED_QUERY_PARAMETERS.contains(parameterName) &&
          !allowedQueryParameters.contains(parameterName))
        throw new IllegalArgumentException("Invalid URI parameter: " + parameterName);

  }

  /** Transaction verification. */
  @SuppressWarnings("unused")
  private static void verifyTransactionAllowed(int type, boolean isItem, DatabaseTable table,
      @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs,
      boolean callerIsSyncAdapter) {

    // Queries are never restricted to app- or sync-adapter-only, and we don't
    // restrict the set of columns that may be accessed.
    if (type == TRANSACTION_QUERY) return;

    // Selections validating
    if (type == TRANSACTION_UPDATE || type == TRANSACTION_DELETE)
      if (!TextUtils.isEmpty(selection)) {
        if (isItem) throw new IllegalArgumentException("Selection not permitted for " + uri);}
      else {if (!isItem) throw new IllegalArgumentException("Selection must be specified for " + uri);}
    else if (!table.preferences && isItem) throw new IllegalArgumentException("Insert not permitted for " + uri);

    if (!callerIsSyncAdapter && table.onlyForSync)
      throw new IllegalArgumentException("Only sync adapters may write using " + uri);

    switch (type) {
      case TRANSACTION_INSERT:
      case TRANSACTION_UPDATE:
        // Check there are no columns restricted to the provider
        verifyColumns(values, table.providerColumns);
        // check that sync only columns aren't included
        if (!callerIsSyncAdapter) verifyNoSyncColumns(values, table.syncColumns);
    }

  }

  /**
   * Check columns
   * @param values values
   */
  private static void verifyColumns(ContentValues values,  Set<String> providerWritableColumns) {
    if (values == null || values.size() == 0) return;
    for (String column : providerWritableColumns)
      if (values.containsKey(column))
        throw new IllegalArgumentException("Only the provider may write to " + column);
  }

  /**
   * Check no sync columns
   * @param values values
   */
  private static void verifyNoSyncColumns(ContentValues values, Set<String> syncWritableColumns) {
    if (values == null || values.size() == 0) return;
    for (String syncColumn : syncWritableColumns)
      if (values.containsKey(syncColumn))
        throw new IllegalArgumentException("Only sync adapters may write to " + syncColumn);

  }

  /** A fast re-implementation of {@link Uri#getQueryParameter} */
  @SuppressWarnings("unused")
  private static String getQueryParameter(Uri uri, String parameter) {

    final String query = uri.getEncodedQuery();
    if (query == null) return null;

    final int queryLength = query.length();
    final int parameterLength = parameter.length();

    int index = 0;
    while (true) {
      index = query.indexOf(parameter, index);
      if (index == -1) return null;
      index += parameterLength;
      if (queryLength == index) return null;
      if (query.charAt(index) == '=') {
        index++;
        break;
      }
    }

    final int ampIndex = query.indexOf('&', index);
    return Uri.decode(ampIndex == -1 ? query.substring(index) :
        query.substring(index, ampIndex));
  }

  /** Shutdown the provider */
  @Override
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public final void shutdown() {
    if (KEEP_DATABASE_BY_CLOSE) return;
    super.shutdown();
    DatabaseHelper.delete(context);
    //final File file = context.getDatabasePath(DatabaseHelper.sSingleton.name); file.delete();
    //new File(file.getAbsolutePath() + "-journal").delete();
  }

  /** Common callback */
  private static class Callback implements Handler.Callback  {

    /** The weak reference to {@link DatabaseProvider} */
    private final WeakReference<DatabaseProvider> mProvider;

    /**
     * Constructs a new {@link Callback} with {@link DatabaseProvider}
     * @param provider the reference of content provider
     */
    Callback(DatabaseProvider provider)
    {mProvider = new WeakReference<>(provider);}

    /** {@inheritDoc} */
    @Override
    public final boolean handleMessage(Message msg) {
      final DatabaseProvider provider = mProvider.get();
      return provider == null || provider.handleMessage(msg);
    }
  }

  /**
   * Background {@link Service} that is used to keep our process alive long enough
   * for background threads to finish. Started and stopped directly by specific
   * background tasks when needed.
   */
  public static class EmptyService extends Service
  {/** {@inheritDoc} */ @Override
  public final IBinder onBind(Intent intent) {return null;}}

  /**
   * Prints the contents of a Cursor to a String. The position is restored
   * after printing.
   *
   * @param cursor the cursor to print
   * @return a String that contains the dumped cursor
   */
  private static String dumpCursorToString(@NonNull Cursor cursor) {
    final StringBuilder stringBuilder = new StringBuilder();
    dumpCursor(cursor, stringBuilder);
    final String result = stringBuilder.toString();
    stringBuilder.setLength(0);
    return result;
  }



}

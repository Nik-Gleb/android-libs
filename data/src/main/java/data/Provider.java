/*
 * Storage.java
 * prefs-tools
 *
 * Copyright (C) 2017, Gleb Nikitenko. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package data;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static android.content.ClipDescription.compareMimeTypes;

/**
 * Storage interface.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 07/08/2017
 */
@SuppressWarnings("unused")
interface Provider {

  /** Default start offset. */
  int OFFSET = 0;

  /** Default length. */
  int LENGTH = -1;

  /** The Data Columns */
  String[] DATA_COLUMNS = { MediaStore.Files.FileColumns.DATA };

  /** The read mode. */
  String READ_MODE = "r";

  /** File name pattern. */
  Pattern FILE_PATTERN = Pattern.compile("[a-zA-Z_0-9.\\-()%]+");

  /** @param uri the unsupported uri */
  static <T> T stub(@NonNull Uri uri)
  {throw new IllegalArgumentException("Unsupported URI: " + uri);}

  /**
   * Implement this to initialize your content provider on startup.
   *
   * <p>This method is called for all registered content providers on the
   * application main thread at application launch time.  It must not perform
   * lengthy operations, or application startup will be delayed.
   *
   * <p>You should defer nontrivial initialization (such as opening,
   * upgrading, and scanning databases) until the content provider is used
   * (via {@link #query}, {@link #insert}, etc).  Deferred initialization
   * keeps application startup fast, avoids unnecessary work if the provider
   * turns out not to be needed, and stops database errors (such as a full
   * disk) from halting application launch.
   *
   * <p>If you use SQLite, {@link android.database.sqlite.SQLiteOpenHelper}
   * is a helpful utility class that makes it easy to manage databases,
   * and will automatically defer opening until first use.  If you do use
   * SQLiteOpenHelper, make sure to avoid calling
   * {@link android.database.sqlite.SQLiteOpenHelper#getReadableDatabase} or
   * {@link android.database.sqlite.SQLiteOpenHelper#getWritableDatabase}
   * from this method.  (Instead, override
   * {@link android.database.sqlite.SQLiteOpenHelper#onOpen} to initialize the
   * database when it is first opened.)
   *
   * @return true if the provider was successfully loaded, false otherwise
   */
  default boolean onCreate() {return false;}

  /**
   * Override this to handle requests to perform a batch of operations, or the
   * default implementation will iterate over the operations and read
   * {@link ContentProviderOperation#apply} on each of them.
   * If all calls to {@link ContentProviderOperation#apply} succeed
   * then a {@link ContentProviderResult} array with as many
   * elements as there were operations will be returned.  If any of the calls
   * fail, it is up to the implementation how many of the others take effect.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * @param operations the operations to apply
   * @return the results of the applications
   * @throws OperationApplicationException thrown if any operation fails.
   * @see ContentProviderOperation#apply
   */
  default @NonNull ContentProviderResult[] applyBatch
  (@NonNull ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    final int numOperations = operations.size();
    final ContentProviderResult[] results =
        new ContentProviderResult[numOperations];
    for (int i = 0; i < numOperations; i++)
      results[i] = operations.get(i).apply(mock(), results, i);
    return results;
  }

  /**
   * Implement this to handle requests to insert a new row.
   *
   * <p>As a courtesy, read {@link ContentResolver#notifyChange(Uri,
   * android.database.ContentObserver) notifyChange()} after inserting.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   * @param uri The content:// URI of the insertion request. This must not be {@code null}.
   * @param values A set of column_name/value pairs to add to the database.
   *     This must not be {@code null}.
   * @return The URI for the newly inserted item.
   */
  @Nullable
  default Uri insert(@NonNull Uri uri, @Nullable ContentValues values)
  {return stub(uri);}

  /**
   * Implement this to handle requests to update one or more rows.
   *
   * <p>The implementation should update all rows matching the selection
   * to set the columns according to the provided values map.
   * As a courtesy, read {@link ContentResolver#notifyChange(Uri,
   * android.database.ContentObserver) notifyChange()} after updating.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * @param uri The URI to query. This can potentially have a record ID if this
   * is an update request for a specific record.
   * @param values A set of column_name/value pairs to update in the database.
   *     This must not be {@code null}.
   * @param selection An optional filter to match rows to update.
   * @return the number of rows affected.
   */
  default int update(@NonNull Uri uri, @Nullable ContentValues values,
      @Nullable String selection, @Nullable String[] selectionArgs)
  {return stub(uri);}

  /**
   * Implement this to handle requests to delete one or more rows.
   *
   * <p>The implementation should apply the selection clause when performing
   * deletion, allowing the operation to affect multiple rows in a directory.
   * As a courtesy, read {@link ContentResolver#notifyChange(Uri,
   * android.database.ContentObserver) notifyChange()} after deleting.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>The implementation is responsible for parsing out a row ID at the end
   * of the URI, if a specific row is being deleted. That is, the client would
   * pass in <code>content://contacts/people/22</code> and the implementation is
   * responsible for parsing the record number (22) when creating a SQL statement.
   *
   * @param uri The full URI to query, including a row ID (if a specific record is requested).
   * @param selection An optional restriction to apply to rows when deleting.
   * @return The number of rows affected.
   */
  default int delete
  (@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs)
  {return stub(uri);}

  /**
   * Implement this to handle query requests from clients.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   * <p>
   * Example client read:<p>
   * <pre>// Request a specific record.
   * Cursor managedCursor = managedQuery(
   ContentUris.withAppendedId(Contacts.People.CONTENT_URI, 2),
   projection,    // Which columns to return.
   null,          // WHERE clause.
   null,          // WHERE clause value substitution
   People.NAME + " ASC");   // Sort order.</pre>
   * Example implementation:<p>
   * <pre>// SQLiteQueryBuilder is a helper class that creates the
   // proper SQL syntax for us.
   SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

   // Set the table we're querying.
   qBuilder.setTables(DATABASE_TABLE_NAME);

   // If the query ends in a specific record number, we're
   // being asked for a specific record, so set the
   // WHERE clause in our query.
   if((URI_MATCHER.match(uri)) == SPECIFIC_MESSAGE){
   qBuilder.appendWhere("_id=" + uri.getPathLeafId());
   }

   // Make the query.
   Cursor c = qBuilder.query(mDb,
   projection,
   selection,
   selectionArgs,
   groupBy,
   having,
   sortOrder);
   c.setNotificationUri(getContext().getContentResolver(), uri);
   return c;</pre>
   *
   * @param uri The URI to query. This will be the full URI sent by the client;
   *      if the client is requesting a specific record, the URI will end in a record number
   *      that the implementation should parse and add to a WHERE or HAVING clause, specifying
   *      that _id value.
   * @param projection The list of columns to put into the cursor. If
   *      {@code null} all columns are included.
   * @param selection A selection criteria to apply when filtering rows.
   *      If {@code null} then all rows are included.
   * @param selectionArgs You may include ?s in selection, which will be replaced by
   *      the values from selectionArgs, in order that they appear in the selection.
   *      The values will be bound as Strings.
   * @param sortOrder How the rows in the cursor should be sorted.
   *      If {@code null} then the provider is free to define the sort order.
   * @param cancellationSignal A signal to cancel the operation in progress, or
   * {@code null} if none. If the operation is canceled, then
   * {@link OperationCanceledException} will be thrown when the query is executed.
   * @return a Cursor or {@code null}.
   */
  default @Nullable
  Cursor query(@NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection, @Nullable String[] selectionArgs,
      @Nullable String sortOrder,
      @Nullable CancellationSignal cancellationSignal)
  {return query(uri, projection, selection, selectionArgs, sortOrder);}

  /**
   * Implement this to handle query requests from clients.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   * <p>
   * Example client read:<p>
   * <pre>// Request a specific record.
   * Cursor managedCursor = managedQuery(
   ContentUris.withAppendedId(Contacts.People.CONTENT_URI, 2),
   projection,    // Which columns to return.
   null,          // WHERE clause.
   null,          // WHERE clause value substitution
   People.NAME + " ASC");   // Sort order.</pre>
   * Example implementation:<p>
   * <pre>// SQLiteQueryBuilder is a helper class that creates the
   // proper SQL syntax for us.
   SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();

   // Set the table we're querying.
   qBuilder.setTables(DATABASE_TABLE_NAME);

   // If the query ends in a specific record number, we're
   // being asked for a specific record, so set the
   // WHERE clause in our query.
   if((URI_MATCHER.match(uri)) == SPECIFIC_MESSAGE){
   qBuilder.appendWhere("_id=" + uri.getPathLeafId());
   }

   // Make the query.
   Cursor c = qBuilder.query(mDb,
   projection,
   selection,
   selectionArgs,
   groupBy,
   having,
   sortOrder);
   c.setNotificationUri(getContext().getContentResolver(), uri);
   return c;</pre>
   *
   * @param uri The URI to query. This will be the full URI sent by the client;
   *      if the client is requesting a specific record, the URI will end in a record number
   *      that the implementation should parse and add to a WHERE or HAVING clause, specifying
   *      that _id value.
   * @param projection The list of columns to put into the cursor. If
   *      {@code null} all columns are included.
   * @param selection A selection criteria to apply when filtering rows.
   *      If {@code null} then all rows are included.
   * @param selectionArgs You may include ?s in selection, which will be replaced by
   *      the values from selectionArgs, in order that they appear in the selection.
   *      The values will be bound as Strings.
   * @param sortOrder How the rows in the cursor should be sorted.
   *      If {@code null} then the provider is free to define the sort order.
   * @return a Cursor or {@code null}.
   */
  default @Nullable
  Cursor query(@NonNull Uri uri,
      @Nullable String[] projection,
      @Nullable String selection, @Nullable String[] selectionArgs,
      @Nullable String sortOrder)
  {return stub(uri);}

  /**
   * Implement this to handle requests for the MIME type of the data at the
   * given URI.
   *
   * <p>The returned MIME type should start with
   * <code>vnd.android.cursor.item</code> for a single record,
   * or <code>vnd.android.cursor.dir/</code> for multiple items.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>Note that there are no permissions needed for an application to
   * access this information; if your content provider requires read and/or
   * write permissions, or is not exported, all applications can still read
   * this method regardless of their access permissions.  This allows them
   * to retrieve the MIME type for a URI when dispatching intents.
   *
   * @param uri the URI to query.
   * @return a MIME type string, or {@code null} if there is no type.
   */
  default @Nullable
  String getType(@NonNull Uri uri) {return stub(uri);}

  /**
   * Override this to handle requests to insert a set of new rows, or the
   * default implementation will iterate over the values and read
   * {@link #insert} on each of them.
   *
   * <p>As a courtesy, read {@link ContentResolver#notifyChange(Uri,
   * android.database.ContentObserver) notifyChange()} after inserting.
   *
   * <p>This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * @param uri The content:// URI of the insertion request.
   * @param values An array of sets of column_name/value pairs to add to the database.
   *    This must not be {@code null}.
   * @return The number of values that were inserted.
   */
  default int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    for (final ContentValues value : values) insert(uri, value);
    return values.length;
  }

  /**
   * Override this to handle requests to open a file blob.
   *
   * The default implementation always throws {@link FileNotFoundException}.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>This method returns a ParcelFileDescriptor, which is returned directly
   * to the caller.  This way large data (such as images and documents) can be
   * returned without copying the content.
   *
   * <p>The returned ParcelFileDescriptor is owned by the caller, so it is
   * their responsibility to close it when done.  That is, the implementation
   * of this method should create a new ParcelFileDescriptor for each read.
   *
   * <p>If opened with the exclusive "r" or "w" modes, the returned
   * ParcelFileDescriptor can be a pipe or socket pair to enable streaming
   * of data. Opening with the "rw" or "rwt" modes implies a file on disk that
   * supports seeking.
   *
   * <p> If you need to detect when the returned ParcelFileDescriptor has been
   * closed, or if the remote process has crashed or encountered some other
   * error, you can use {@link ParcelFileDescriptor#open(File, int,
   * android.os.Handler, ParcelFileDescriptor.OnCloseListener)},
   * {@link ParcelFileDescriptor#createReliablePipe()}, or
   * {@link ParcelFileDescriptor#createReliableSocketPair()}.
   *
   * <p class="note">For use in Intents, you will want to implement
   * {@link #getType} to return the appropriate MIME type for the data returned
   * here with the same URI.
   *
   * <p>This will allow intent resolution to automatically determine the data
   * MIME type and select the appropriate matching targets as part of its operation.
   *
   * <p class="note">For better interoperability with other applications, it is
   * recommended that for any URIs that can be opened, you also support queries
   * on them containing at least the columns specified by
   * {@link android.provider.OpenableColumns}.
   *
   * <p>You may also want to support other common columns if you have
   * additional meta-data to supply, such as
   * {@link MediaStore.MediaColumns#DATE_ADDED}
   * in {@link MediaStore.MediaColumns}.
   *
   * @param uri The URI whose file is to be opened.
   * @param mode Access mode for the file. May be "r" for read-only access,
   *            "w" for write-only access, "rw" for read and write access, or
   *            "rwt" for read and write access that truncates any existing
   *            file.
   * @return Returns a new ParcelFileDescriptor which you can use to access
   * the file.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the file.
   *
   * @see #getType(Uri)
   * @see ParcelFileDescriptor#parseMode(String)
   */
  default @Nullable ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException
  {throw new FileNotFoundException("No files supported by provider at " + uri);}


  /**
   * Override this to handle requests to open a file blob.
   *
   * The default implementation always throws {@link FileNotFoundException}.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>This method returns a ParcelFileDescriptor, which is returned directly
   * to the caller.  This way large data (such as images and documents) can be
   * returned without copying the content.
   *
   * <p>The returned ParcelFileDescriptor is owned by the caller, so it is
   * their responsibility to close it when done.  That is, the implementation
   * of this method should create a new ParcelFileDescriptor for each read.
   *
   * <p>If opened with the exclusive "r" or "w" modes, the returned
   * ParcelFileDescriptor can be a pipe or socket pair to enable streaming
   * of data. Opening with the "rw" or "rwt" modes implies a file on disk that
   * supports seeking.
   *
   * <p> If you need to detect when the returned ParcelFileDescriptor has been
   * closed, or if the remote process has crashed or encountered some other
   * error, you can use {@link ParcelFileDescriptor#open(File, int,
   * android.os.Handler, ParcelFileDescriptor.OnCloseListener)},
   * {@link ParcelFileDescriptor#createReliablePipe()}, or
   * {@link ParcelFileDescriptor#createReliableSocketPair()}.
   *
   * <p class="note">For use in Intents, you will want to implement
   * {@link #getType} to return the appropriate MIME type for the data returned
   * here with the same URI.
   *
   * <p>This will allow intent resolution to automatically determine the data
   * MIME type and select the appropriate matching targets as part of its operation.
   *
   * <p class="note">For better interoperability with other applications, it is
   * recommended that for any URIs that can be opened, you also support queries
   * on them containing at least the columns specified by
   * {@link android.provider.OpenableColumns}.
   *
   * <p>You may also want to support other common columns if you have
   * additional meta-data to supply, such as
   * {@link MediaStore.MediaColumns#DATE_ADDED}
   * in {@link MediaStore.MediaColumns}.
   *
   * @param uri The URI whose file is to be opened.
   * @param mode Access mode for the file. May be "r" for read-only access,
   *            "w" for write-only access, "rw" for read and write access, or
   *            "rwt" for read and write access that truncates any existing
   *            file.
   * @param signal A signal to cancel the operation in progress, or
   *            {@code null} if none. For example, if you are downloading a
   *            file from the network to service a "rw" mode request, you
   *            should periodically read
   *            {@link CancellationSignal#throwIfCanceled()} to check whether
   *            the client has canceled the request and abort the download.
   *
   * @return Returns a new ParcelFileDescriptor which you can use to access
   * the file.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the file.
   *
   * @see #getType(Uri)
   * @see ParcelFileDescriptor#parseMode(String)
   */
  @SuppressWarnings("unused")
  default @Nullable ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal)
      throws FileNotFoundException {return openFile(uri, mode);}

  /**
   * This is like {@link #openFile}, but can be implemented by providers
   * that need to be able to return sub-sections of files, often assets
   * inside of their .apk.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>If you implement this, your clients must be able to deal with such
   * file slices, either directly with
   * {@link ContentResolver#openAssetFileDescriptor}, or by using the higher-level
   * {@link ContentResolver#openInputStream DataSource.openInputStream}
   * or {@link ContentResolver#openOutputStream DataSource.openOutputStream}
   * methods.
   * <p>
   * The returned AssetFileDescriptor can be a pipe or socket pair to enable
   * streaming of data.
   *
   * <p class="note">If you are implementing this to return a full file, you
   * should create the AssetFileDescriptor with
   * {@link AssetFileDescriptor#UNKNOWN_LENGTH} to be compatible with
   * applications that cannot handle sub-sections of files.</p>
   *
   * <p class="note">For use in Intents, you will want to implement {@link #getType}
   * to return the appropriate MIME type for the data returned here with
   * the same URI.  This will allow intent resolution to automatically determine the data MIME
   * type and select the appropriate matching targets as part of its operation.</p>
   *
   * <p class="note">For better interoperability with other applications, it is recommended
   * that for any URIs that can be opened, you also support queries on them
   * containing at least the columns specified by {@link android.provider.OpenableColumns}.</p>
   *
   * @param uri The URI whose file is to be opened.
   * @param mode Access mode for the file.  May be "r" for read-only access,
   * "w" for write-only access (erasing whatever data is currently in
   * the file), "wa" for write-only access to append to any existing data,
   * "rw" for read and write access on any existing data, and "rwt" for read
   * and write access that truncates any existing file.
   *
   * @return Returns a new AssetFileDescriptor which you can use to access
   * the file.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the file.
   *
   * @see #openFile(Uri, String)
   * @see #getType(Uri)
   */
  default @Nullable
  AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException
  {final ParcelFileDescriptor fd = openFile(uri, mode);
    return fd != null ? new AssetFileDescriptor(fd, OFFSET, LENGTH) : null;}

  /**
   * This is like {@link #openFile}, but can be implemented by providers
   * that need to be able to return sub-sections of files, often assets
   * inside of their .apk.
   * This method can be called from multiple threads, as described in
   * <a href="{@docRoot}guide/topics/fundamentals/processes-and-threads.html#Threads">Processes
   * and Threads</a>.
   *
   * <p>If you implement this, your clients must be able to deal with such
   * file slices, either directly with
   * {@link ContentResolver#openAssetFileDescriptor}, or by using the higher-level
   * {@link ContentResolver#openInputStream DataSource.openInputStream}
   * or {@link ContentResolver#openOutputStream DataSource.openOutputStream}
   * methods.
   * <p>
   * The returned AssetFileDescriptor can be a pipe or socket pair to enable
   * streaming of data.
   *
   * <p class="note">If you are implementing this to return a full file, you
   * should create the AssetFileDescriptor with
   * {@link AssetFileDescriptor#UNKNOWN_LENGTH} to be compatible with
   * applications that cannot handle sub-sections of files.</p>
   *
   * <p class="note">For use in Intents, you will want to implement {@link #getType}
   * to return the appropriate MIME type for the data returned here with
   * the same URI.  This will allow intent resolution to automatically determine the data MIME
   * type and select the appropriate matching targets as part of its operation.</p>
   *
   * <p class="note">For better interoperability with other applications, it is recommended
   * that for any URIs that can be opened, you also support queries on them
   * containing at least the columns specified by {@link android.provider.OpenableColumns}.</p>
   *
   * @param uri The URI whose file is to be opened.
   * @param mode Access mode for the file.  May be "r" for read-only access,
   * "w" for write-only access (erasing whatever data is currently in
   * the file), "wa" for write-only access to append to any existing data,
   * "rw" for read and write access on any existing data, and "rwt" for read
   * and write access that truncates any existing file.
   *
   * @return Returns a new AssetFileDescriptor which you can use to access
   * the file.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the file.
   *
   * @see #openFile(Uri, String)
   * @see #getType(Uri)
   */
  default @Nullable AssetFileDescriptor openAssetFile (@NonNull Uri uri, @NonNull String mode,
  @NonNull CancellationSignal cancellationSignal) throws FileNotFoundException
  {return openAssetFile(uri, mode);}

  /**
   * Called by a client to open a read-only stream containing data of a
   * particular MIME type.
   *
   * <p>This is like {@link ContentProvider#openAssetFile(Uri, String)},
   * except the file can only be read-only and the content provider may
   * perform data conversions to generate data of the desired type.
   *
   * <p>The default implementation compares the given mimeType against the
   * result of {@link #getType(Uri)} and, if they match, simply calls
   * {@link ContentProvider#openAssetFile(Uri, String)}.
   *
   * <p>See {@link ClipData} for examples of the use and implementation
   * of this method.
   *
   * <p>The returned AssetFileDescriptor can be a pipe or socket pair to enable
   * streaming of data.
   *
   * <p class="note">For better interoperability with other applications, it is
   * recommended that for any URIs that can be opened, you also support queries
   * on them containing at least the columns specified by
   * {@link android.provider.OpenableColumns}.
   *
   * <p>You may also want to support other common columns if you have
   * additional meta-data to supply, such as
   * {@link MediaStore.MediaColumns#DATE_ADDED}
   * in {@link MediaStore.MediaColumns}.</p>
   *
   * @param uri The data in the content provider being queried.
   * @param mimeTypeFilter The type of data the client desires.  May be
   * a pattern, such as *&#47;*, if the caller does not have specific type
   * requirements; in this case the content provider will pick its best
   * type matching the pattern.
   * @param opts Additional options from the client.  The definitions of
   * these are specific to the content provider being called.
   *
   * @return Returns a new AssetFileDescriptor from which the client can
   * read data of the desired type.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the data.
   * @throws IllegalArgumentException Throws IllegalArgumentException if the
   * content provider does not support the requested MIME type.
   *
   * @see #getStreamTypes(Uri, String)
   * @see ClipDescription#compareMimeTypes(String, String)
   */
  default @Nullable AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
      @NonNull String mimeTypeFilter, @Nullable Bundle opts) throws
      FileNotFoundException {

    // If they can take anything, the untyped open read is good enough.
    if ("*/*".equals(mimeTypeFilter)) return openAssetFile(uri, READ_MODE);

    // Use old untyped open read if this provider has a type for this URI and
    // it matches the request.
    final String baseType = getType(uri);
    if (baseType != null && compareMimeTypes(baseType, mimeTypeFilter)) return openAssetFile(uri, READ_MODE);
    throw new FileNotFoundException ("Can't open " + uri + " as type " + mimeTypeFilter);
  }

  /**
   * Called by a client to open a read-only stream containing data of a
   * particular MIME type.
   *
   * <p>This is like {@link ContentProvider#openAssetFile(Uri, String)},
   * except the file can only be read-only and the content provider may
   * perform data conversions to generate data of the desired type.
   *
   * <p>The default implementation compares the given mimeType against the
   * result of {@link #getType(Uri)} and, if they match, simply calls
   * {@link ContentProvider#openAssetFile(Uri, String)}.
   *
   * <p>See {@link ClipData} for examples of the use and implementation
   * of this method.
   *
   * <p>The returned AssetFileDescriptor can be a pipe or socket pair to enable
   * streaming of data.
   *
   * <p class="note">For better interoperability with other applications, it is
   * recommended that for any URIs that can be opened, you also support queries
   * on them containing at least the columns specified by
   * {@link android.provider.OpenableColumns}.
   *
   * <p>You may also want to support other common columns if you have
   * additional meta-data to supply, such as
   * {@link MediaStore.MediaColumns#DATE_ADDED}
   * in {@link MediaStore.MediaColumns}.</p>
   *
   * @param uri The data in the content provider being queried.
   * @param mimeTypeFilter The type of data the client desires.  May be
   * a pattern, such as *&#47;*, if the caller does not have specific type
   * requirements; in this case the content provider will pick its best
   * type matching the pattern.
   * @param opts Additional options from the client.  The definitions of
   * these are specific to the content provider being called.
   *
   * @return Returns a new AssetFileDescriptor from which the client can
   * read data of the desired type.
   *
   * @throws FileNotFoundException Throws FileNotFoundException if there is
   * no file associated with the given URI or the mode is invalid.
   * @throws SecurityException Throws SecurityException if the caller does
   * not have permission to access the data.
   * @throws IllegalArgumentException Throws IllegalArgumentException if the
   * content provider does not support the requested MIME type.
   *
   * @see #getStreamTypes(Uri, String)
   * @see ClipDescription#compareMimeTypes(String, String)
   */
  default @Nullable AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String mimeTypeFilter, @Nullable Bundle opts,
      @Nullable CancellationSignal signal) throws FileNotFoundException
  {return openTypedAssetFile(uri, mimeTypeFilter, opts);}

  /**
   * Convenience for subclasses that wish to implement {@link #openFile}
   * by looking up a column named "_data" at the given URI.
   *
   * @param uri The URI to be opened.
   * @param mode The file mode.  May be "r" for read-only access,
   * "w" for write-only access (erasing whatever data is currently in
   * the file), "wa" for write-only access to append to any existing data,
   * "rw" for read and write access on any existing data, and "rwt" for read
   * and write access that truncates any existing file.
   *
   * @return Returns a new ParcelFileDescriptor that can be used by the
   * client to access the file.
   */
  default @NonNull ParcelFileDescriptor openFileHelperInternal
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    final String sel = null, sort = null;
    final String[] selArgs = null;
    final Cursor cursor = query(uri, DATA_COLUMNS, sel, selArgs, sort);
    int count = (cursor != null) ? cursor.getCount() : 0;
    if (count != 1) {
      // If there is not exactly one result, throw an appropriate exception.
      if (cursor != null) cursor.close();
      if (count == 0) throw new FileNotFoundException("No entry for " + uri);
      throw new FileNotFoundException("Multiple items at " + uri);
    }

    cursor.moveToFirst();
    final int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
    final String path = (index >= 0 ? cursor.getString(index) : null); cursor.close();
    if (path == null) throw new FileNotFoundException("Column _data not found.");

    final int modeBits = ParcelFileDescriptor.parseMode(mode);
    return ParcelFileDescriptor.open(new File(path), modeBits);
  }

  /**
   * Called by a client to determine the types of data streams that this
   * content provider supports for the given URI.
   *
   * The default implementation returns {@code null}, meaning no types.
   * If your content provider stores data of a particular type, return that MIME
   * type if it matches the given mimeTypeFilter.
   *
   * <p>If it can perform type conversions, return an array
   * of all supported MIME types that match mimeTypeFilter.
   *
   * @param uri The data in the content provider being queried.
   * @param mimeTypeFilter The type of data the client desires.  May be
   * a pattern, such as *&#47;* to retrieve all possible data types.
   * @return Returns {@code null} if there are no possible data streams for the
   * given mimeTypeFilter.  Otherwise returns an array of all available
   * concrete MIME types.
   *
   * @see #getType(Uri)
   * @see ClipDescription#compareMimeTypes(String, String)
   */
  default @Nullable String[] getStreamTypes
  (@NonNull Uri uri, @NonNull String mimeTypeFilter)
  {return null;}

  /**
   * Call a provider-defined method.  This can be used to implement interfaces
   * that are cheaper and/or unnatural for a table-like model.
   *
   * <p class="note"><strong>WARNING:</strong> The framework does no permission
   * checking on this entry into the content provider besides the basic ability
   * for the application to get access to the provider at all.
   *
   * <p>For example, it has no idea whether the read being executed may read or
   * write data in the provider, so can't enforce those individual permissions.
   *
   * <p>Any implementation of this method <strong>must</strong> do its own
   * permission checks on incoming calls to make sure they are allowed.</p>
   *
   * @param method method name to read.  Opaque to framework, but should not
   * be {@code null}.
   * @param arg provider-defined String argument.  May be {@code null}.
   * @param extras provider-defined Bundle argument.  May be {@code null}.
   * @return provider-defined return value.  May be {@code null}, which is also
   *   the default for providers which don't implement any read methods.
   */
  default @Nullable
  Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras)
  {return null;}

  /**
   * Implement this to shut down the ContentProvider instance. You can then
   * invoke this method in unit tests.
   *
   * <p>Android normally handles ContentProvider startup and shutdown
   * automatically. You do not need to start up or shut down a
   * ContentProvider. When you invoke a test method on a ContentProvider,
   * however, a ContentProvider instance is started and keeps running after
   * the test finishes, even if a succeeding test instantiates another
   * ContentProvider. A conflict develops because the two instances are
   * usually running against the same underlying data source (for example, an
   * sqlite database).
   *
   * <p>Implementing shutDown() avoids this conflict by providing a way to
   * terminate the ContentProvider. This method can also prevent memory leaks
   * from multiple instantiations of the ContentProvider, and it can ensure
   * unit test isolation by allowing you to completely clean up the test
   * fixture before moving on to the next test.
   */
  default void shutdown() {}

  /**
   * Print the Provider's state into the given stream.  This gets invoked if
   * you run "adb shell dumpsys activity provider &lt;provider_component_name&gt;".
   *
   * @param fd The raw file descriptor that the dump is being sent to.
   * @param writer The PrintWriter to which you should dump your state.
   * This will be closed for you after you return.
   * @param args additional arguments to the dump request.
   */
  default void dump
  (@NonNull FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args)
  {writer.println("nothing to dump");}

  /** @return all paths */
  @NonNull default String[] getSupportedPaths()
  {return new String[0];}

  /** @return self-mocked content provider */
  @NonNull default ContentProvider mock() {
    final Provider instance = this;
    return new ContentProvider() {

      /** {@inheritDoc} */
      @Override
      public final boolean onCreate() {return true;}

      /** {@inheritDoc} */
      @Nullable @Override
      public final Cursor query(@NonNull Uri uri,
          @Nullable String[] projection, @Nullable String selection,
          @Nullable String[] selectionArgs, @Nullable String sortOrder)
      {return instance.query(uri, projection, selection, selectionArgs, sortOrder);}

      /** {@inheritDoc} */
      @Nullable @Override
      public final String getType(@NonNull Uri uri)
      {return instance.getType(uri);}

      /** {@inheritDoc} */
      @Nullable @Override
      public final Uri insert
      (@NonNull Uri uri, @Nullable ContentValues values)
      {return instance.insert(uri, values);}

      /** {@inheritDoc} */
      @Override
      public final int delete(@NonNull Uri uri,
          @Nullable String selection, @Nullable String[] selectionArgs)
      {return instance.delete(uri, selection, selectionArgs);}

      /** {@inheritDoc} */
      @Override
      public final int update(@NonNull Uri uri,
          @Nullable ContentValues values, @Nullable String selection,
          @Nullable String[] selectionArgs)
      {return instance.update(uri, values, selection, selectionArgs);}
    };
  }

  /**
   * @param clazz provider class
   *
   * @return provider tag
   */
  @NonNull static String getTag(@NonNull Class<? extends Provider> clazz) {
    String[] result; if ((result = clazz.getName().split("\\.")).length <= 0)
      throw new IllegalArgumentException("Invalid class");
    else return result[result.length - 1].replace("Provider", "").toLowerCase();
  }

  /**
   * An optional insert, update or delete URI parameter that allows the caller
   * to specify that it is a sync adapter. The default value is false. If set
   * to true, the modified row is not marked as "dirty" (needs to be synced)
   * and when the provider calls
   * {@link ContentResolver#notifyChange(Uri, android.database.ContentObserver, boolean)},
   * the third parameter "syncToNetwork" is set to false.
   *
   * @see Uri.Builder#appendQueryParameter(String, String)
   */
  String CALLER_IS_SYNCADAPTER = "caller_is_syncadapter";

  /**
   * @param uri uri addresses
   * @return true - sync adapter
   */
  static boolean isCallerSyncAdapter(@NonNull Uri uri)
  {return uri.getBooleanQueryParameter(CALLER_IS_SYNCADAPTER, false);}

  /**
   * @param resolver  content resolver
   * @param uri       uri resource
   * @param observer  content observer
   */
  static void notifyUri(@NonNull ContentResolver resolver, @NonNull Uri uri, @Nullable ContentObserver observer)
  {resolver.notifyChange(uri, observer, uri.getBooleanQueryParameter(CALLER_IS_SYNCADAPTER, false));}

  /** @param signal cancellation signal for check */
  static void isCanceled(@Nullable CancellationSignal signal)
  {if (signal != null) signal.throwIfCanceled();}

  /**
   * @param path to file
   * @return mime-type
   */
  @NonNull static String getMimeType(@NonNull String path) {
    final String result =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension
            (MimeTypeMap.getFileExtensionFromUrl(path));
    return TextUtils.isEmpty(result) ? "application/octet-stream" : result;
  }

  /**
   * Returns the file extension or an empty string iff there is no
   * extension. This method is a convenience method for obtaining the
   * extension of a url and has undefined results for other Strings.
   *
   * @param uri uri resource
   *
   * @return The file extension of the given url.
   */
  @NonNull static String getFileExtensionFromUrl(@NonNull String uri) {
    if (!TextUtils.isEmpty(uri)) {
      final int fragment = uri.lastIndexOf('#');
      if (fragment > 0) uri = uri.substring(0, fragment);
      final int query = uri.lastIndexOf('?');
      if (query > 0) uri = uri.substring(0, query);
      final int filenamePos = uri.lastIndexOf('/');
      final String filename = 0 <= filenamePos ?
          uri.substring(filenamePos + 1) : uri;

      // if the filename contains special characters, we don't
      // consider it valid for our matching purposes:
      if (!filename.isEmpty() && FILE_PATTERN.matcher(filename).matches()) {
        final int dotPos = filename.lastIndexOf('.');
        if (0 <= dotPos) return filename.substring(dotPos + 1);
      }
    }
    return "";
  }

  /**
   * @param dir files directory
   * @param uri file uri resource
   *
   * @return java file object
   */
  @NonNull static File file(@NonNull File dir, @NonNull Uri uri)
  {return new File(dir, uri.getEncodedPath());}

  /**
   * @param file file instance
   *
   * @return the name of file without ext
   */
  @NonNull static String name(@NonNull File file)
  {return file.getName().replaceFirst("[.][^.]+$", "");}
}

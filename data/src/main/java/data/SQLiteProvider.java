/*
 * SQLiteProvider.java
 * data
 *
 * Copyright (C) 2018, Gleb Nikitenko. All Rights Reserved.
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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static data.Provider.isCallerSyncAdapter;

/**
 * General purpose {@link ContentProvider} base class that uses SQLiteDatabase for storage.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/09/2016
 */
@SuppressWarnings("WeakerAccess, unused")
abstract class SQLiteProvider implements Provider {

    /** The log-cat tag. */
    private static final String TAG = "SQLiteProvider";

    /** Sleep after yield delay. */
    private static final int SLEEP_AFTER_YIELD_DELAY = 400;
    /** Maximum number of operations allowed in a batch between yield points. */
    private static final int MAX_OPERATIONS_PER_YIELD_POINT = 512;
    /** Number of inserts performed in bulk to allow before yielding the transaction. */
    private static final int BULK_INSERTS_PER_YIELD_POINT = 256;

    /** SQLite helper */
    private SQLiteOpenHelper mOpenHelper = null;

    /** Changed Uri's */
    private final Set<Uri> mChangedUris = new HashSet<>();

    /** database */
    protected SQLiteDatabase mDb;
    private final ThreadLocal<Boolean> mApplyingBatch = new ThreadLocal<>();

    /** Application context. */
    protected final Context context;

    /**
     * Constructs a new {@link SQLiteProvider}.
     *
     * @param context application context
     */
    SQLiteProvider(@NonNull Context context)
    {this.context = context;}

    /** {@inheritDoc} */
    @Override public boolean onCreate() {
        mOpenHelper = getDatabaseHelper(context);
        return mOpenHelper != null;
    }

    /** Returns a {@link SQLiteOpenHelper} that can open the database. */
    protected abstract SQLiteOpenHelper getDatabaseHelper(Context context);

    /** The equivalent of the {@link #insert} method, but invoked within a transaction.*/
    protected abstract Uri insertInTransaction
    (@NonNull Uri uri, ContentValues values, boolean callerIsSyncAdapter);

    /** The equivalent of the {@link #update} method, but invoked within a transaction. */
    protected abstract int updateInTransaction
    (@NonNull Uri uri, @Nullable ContentValues values,
        @Nullable String sel, @Nullable String[] args, boolean callerIsSyncAdapter);

    /** The equivalent of the {@link #delete} method, but invoked within a transaction.*/
    protected abstract int deleteInTransaction
    (@NonNull Uri uri, @Nullable String sel, @Nullable String[] args, boolean callerIsSyncAdapter);

    /** Call this to add a URI to the list of URIs to be notified when the transaction is committed. */
    protected final void postNotifyUri(@NonNull Uri uri)
    {synchronized (mChangedUris) {mChangedUris.add(uri);}}

    /** @return is batch applying */
    private boolean applyingBatch()
    {return mApplyingBatch.get() != null && mApplyingBatch.get();}

    /** {@inheritDoc} */
    @Override
    public final Uri insert(@NonNull Uri uri, ContentValues values) {
        Uri result;
        final boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        final boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            //mDb.beginTransaction();
            mDb.beginTransactionNonExclusive();
            try {result = insertInTransaction(uri, values, callerIsSyncAdapter);
                mDb.setTransactionSuccessful();} finally {mDb.endTransaction();}
            onEndTransaction(callerIsSyncAdapter);
        } else result = insertInTransaction(uri, values, callerIsSyncAdapter);

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        final int numValues = values.length; int opCount = 0, result;
        final boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        mDb = mOpenHelper.getWritableDatabase();
        //mDb.beginTransaction();
        mDb.beginTransactionNonExclusive();
        try {
            for (ContentValues value : values) {
                insertInTransaction(uri, value, callerIsSyncAdapter);
                if (++opCount >= BULK_INSERTS_PER_YIELD_POINT) {
                    opCount = 0;
                    mDb.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY);
                }
            }
            result = numValues;
            mDb.setTransactionSuccessful();} finally {mDb.endTransaction();}
        onEndTransaction(callerIsSyncAdapter);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final int update(@NonNull Uri uri, ContentValues values, String sel, String[] args) {
        int result; boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            //mDb.beginTransaction();
            mDb.beginTransactionNonExclusive();
            try {result = updateInTransaction(uri, values, sel, args, callerIsSyncAdapter);
                mDb.setTransactionSuccessful();} finally {mDb.endTransaction();}
            onEndTransaction(callerIsSyncAdapter);
        } else result = updateInTransaction(uri, values, sel, args, callerIsSyncAdapter);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public final int delete(@NonNull Uri uri, String sel, String[] args) {
        int result; boolean callerIsSyncAdapter = isCallerSyncAdapter(uri);
        boolean applyingBatch = applyingBatch();
        if (!applyingBatch) {
            mDb = mOpenHelper.getWritableDatabase();
            //mDb.beginTransaction();
            mDb.beginTransactionNonExclusive();
            try {result = deleteInTransaction(uri, sel, args, callerIsSyncAdapter);
                mDb.setTransactionSuccessful();} finally {mDb.endTransaction();}
            onEndTransaction(callerIsSyncAdapter);
        } else result = deleteInTransaction(uri, sel, args, callerIsSyncAdapter);
        return result;
    }

    /** {@inheritDoc} */
    @NonNull
    @Override
    public final ContentProviderResult[] applyBatch(@NonNull
        ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        int ypCount = 0, opCount = 0;
        boolean callerIsSyncAdapter = false;
        mDb = mOpenHelper.getWritableDatabase();
        //mDb.beginTransaction();
        mDb.beginTransactionNonExclusive();
        try {
            mApplyingBatch.set(true);
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for (int i = 0; i < numOperations; i++) {
                if (++opCount >= MAX_OPERATIONS_PER_YIELD_POINT) {
                    throw new OperationApplicationException(
                            "Too many content provider operations between yield points. "
                                    + "The maximum number of operations per yield point is "
                                    + MAX_OPERATIONS_PER_YIELD_POINT, ypCount);
                }

                final ContentProviderOperation operation = operations.get(i);
                if (!callerIsSyncAdapter && isCallerSyncAdapter(operation.getUri()))
                    callerIsSyncAdapter = true;

                if (i > 0 && operation.isYieldAllowed()) {
                    opCount = 0;
                    if (mDb.yieldIfContendedSafely(SLEEP_AFTER_YIELD_DELAY)) ypCount++;
                }
                results[i] = operation.apply(mock(), results, i);
            }
            mDb.setTransactionSuccessful();
            return results;
        } finally {
            mApplyingBatch.set(false);
            mDb.endTransaction();
            onEndTransaction(callerIsSyncAdapter);
        }
    }

    /** @param callerIsSyncAdapter access mode flag */
    protected final void onEndTransaction(boolean callerIsSyncAdapter) {
        Set<Uri> changed;
        synchronized (mChangedUris) {
            changed = new HashSet<>(mChangedUris);
            mChangedUris.clear();
        }
        final ContentResolver resolver = context.getContentResolver();
        for (Uri uri : changed)resolver.notifyChange
            (uri, null, !callerIsSyncAdapter && syncToNetwork(uri));
    }

    /**
     * @param uri resource uri
     * @return need sync flag
     */
    @SuppressWarnings("SameReturnValue")
    protected boolean syncToNetwork(Uri uri) {return false;}

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        if (mDb != null) {mDb.close(); mDb = null;}
        if (mOpenHelper != null) {mOpenHelper.close(); mOpenHelper = null;}
    }
}
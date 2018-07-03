/*
 * BatchOps.java
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
 * The above copyright notice and this permission notice shall be included in
 * all
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

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.util.ArrayList;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.content.ContentProviderOperation.newDelete;
import static android.content.ContentProviderOperation.newInsert;
import static android.content.ContentProviderOperation.newUpdate;
import static android.content.ContentUris.withAppendedId;
import static android.provider.BaseColumns._ID;
import static data.DataSource.DATA;
import static data.DataSource.keyToId;

/**
 * Batch of Content Operations.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/07/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
public final class BatchOps {

  /** Content provider operations. */
  private final ArrayList<ContentProviderOperation>
      mOperations = new ArrayList<>();

  /** Data source. */
  private final DataSource mDataSource;

  /**
   * Constructs a new {@link BulkInsert}.
   *
   * @param source data source
   */
  BatchOps(@NonNull DataSource source) {mDataSource = source;}

  /**
   * @param size batch size
   *
   * @return true if necessary yield
   */
  private static boolean yield(int size) {return size % 511 == 0;}

  /**
   * @param resource data resource
   * @param value    raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps insert
  (@NonNull DataResource resource, @NonNull byte[] value) {
    mOperations.add(
        newInsert(resource.uri)
            .withValue(DATA, value)
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param resource data resource
   * @param key      key of row
   * @param value    raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps insert
  (@NonNull DataResource resource, @NonNull String key, @NonNull byte[] value) {
    mOperations.add(
        newInsert(resource.uri)
            .withValue(_ID, keyToId(key))
            .withValue(DATA, value)
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param resource data resource
   * @param values   content values
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  final BatchOps insert
  (@NonNull DataResource resource, @NonNull ContentValues values) {
    mOperations.add(
        newInsert(resource.uri)
            .withValues(values)
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param resource data resource
   * @param key      key of updatable
   * @param value    raw data
   *
   * @return this builder, to allow for chaining.
   */
  @SuppressWarnings("WeakerAccess")
  @NonNull
  public final BatchOps update
  (@NonNull DataResource resource, @NonNull String key, @NonNull byte[] value) {
    mOperations.add(
        newUpdate(withAppendedId(resource.uri, keyToId(key)))
            .withValue(DATA, value)
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param resource data resource
   * @param id       id of updatable
   * @param value    raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps update
  (@NonNull DataResource resource, long id, @NonNull byte[] value) {
    mOperations.add(
        newUpdate(withAppendedId(resource.uri, id))
            .withValue(DATA, value)
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param update   true for update, false for insert
   * @param resource data resource
   * @param key      key of updatable
   * @param value    raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps put(boolean update,
    @NonNull DataResource resource, @NonNull String key, @NonNull byte[] value) {
    return update ? update(resource, key, value) : insert(resource, key, value);
  }

  /**
   * @param resource data resource
   * @param key      key of deletable
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps delete
  (@NonNull DataResource resource, @NonNull String key) {
    mOperations.add(
        newDelete(withAppendedId(resource.uri, keyToId(key)))
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /**
   * @param resource data resource
   * @param id       id of deletable
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final BatchOps delete(@NonNull DataResource resource, long id) {
    mOperations.add(
        newDelete(withAppendedId(resource.uri, id))
            .withYieldAllowed(yield(mOperations.size()))
            .build()
    );
    return this;
  }

  /** @return count of rows */
  @WorkerThread
  @NonNull
  public final ContentProviderResult[] execute() {
    return mDataSource.applyBatch(mOperations);
  }
}

/*
 * BulkInsert.java
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

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static data.DataSource.DATA;
import static java.lang.System.arraycopy;

/**
 * Bulk Insert of data.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/07/2018
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("unused")
public final class BulkInsert {

  /** Data source. */
  private final DataSource mDataSource;

  /** Content uri. */
  private final Uri mContentUri;

  /** Content values. */
  private ContentValues[] mContentValues;

  /**
   * Constructs a new {@link BulkInsert}.
   *
   * @param source data source
   * @param uri    content uri
   */
  BulkInsert(@NonNull DataSource source, @NonNull Uri uri) {
    mDataSource = source;
    mContentUri = uri;
    mContentValues = new ContentValues[0];
  }

  /**
   * @param values source array of values
   * @param value  new value
   *
   * @return result array of values
   */
  @NonNull private static ContentValues[] addValue
  (@NonNull ContentValues[] values, @NonNull ContentValues value) {
    final ContentValues[] result = new ContentValues[values.length + 1];
    arraycopy(values, 0, result, 0, values.length);
    result[values.length] = value;
    return result;
  }

  /**
   * @param value raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull public final BulkInsert put(@NonNull byte[] value) {
    final ContentValues values = new ContentValues(1);
    values.put(DATA, value);
    mContentValues = addValue(mContentValues, values);
    return this;
  }

  /**
   * @param values content values
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull final BulkInsert put(@NonNull ContentValues values) {
    mContentValues = addValue(mContentValues, values);
    return this;
  }

  /** @return count of rows */
  @WorkerThread
  public final int execute()
  {return mDataSource.bulkInsert(mContentUri, mContentValues);}

}

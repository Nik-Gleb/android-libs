/*
 * Query.java
 * content
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

package repository;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.RemoteException;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * ContentContracts Provider Query Builder.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 12/07/2017
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Query implements Selectable<Query> {

  /** SQL Sort delimiter. */
  private static final String SORT_DELIMITER = ",";

  /** The resource uri. */
  @NonNull private final Uri mUri;

  /** The item id */
  private long mId = -1;

  /** The selection. */
  @Nullable private Selection mSelection = null;

  /** The columns. */
  @Nullable private String[] mProjection = null;
  /** The sort order. */
  @Nullable private String mSortOrder = null;

  /** The Limit */
  private int mLimit = -1;
  /** The Offset */
  private int mOffset = -1;

  /**
   * Construct a new {@link Query}
   *
   * @param uri content resource
   */
  public Query (@NonNull Uri uri) {
    mUri = uri;
  }

  /**
   * Access to item.
   *
   * @return current builder
   */
  @NonNull
  public final Query item (long id) {
    mId = id;
    return this;
  }

  /**
   * Define columns.
   *
   * @return current builder
   */
  @NonNull
  public final Query columns (@NonNull String... columns) {
    mProjection = columns;
    return this;
  }

  /**
   * Define selection.
   *
   * @return current builder
   */
  @NonNull
  public final Query select (@NonNull Selection selection) {
    mSelection = selection;
    return this;
  }

  /**
   * Define sort
   *
   * @return current builder
   */
  @NonNull
  public final Query sort (boolean asc, @NonNull String... columns) {
    mSortOrder =
        TextUtils.join(SORT_DELIMITER, columns) + " " + (asc ? "ASC" : "DESC");
    return this;
  }

  /**
   * Define limit
   *
   * @return current builder
   */
  @NonNull
  public final Query limit (@IntRange(from = 1) int limit) {
    if (limit > 0) {
      mLimit = limit;
    }
    return this;
  }

  /**
   * Define offset
   *
   * @return current builder
   */
  @NonNull
  public final Query offset (@IntRange(from = 1) int offset) {
    if (offset > 0) {
      mOffset = offset;
    }
    return this;
  }

  /**
   * @param client content provider client
   * @param signal cancellation signal
   *
   * @return content provider response
   **/
  @Nullable
  final Cursor exec (@NonNull ContentProviderClient client,
      @Nullable CancellationSignal signal)
      throws RemoteException {

    final String select = mSelection != null ? mSelection.getSelection() : null;
    final String[] selArgs =
        mSelection != null ? mSelection.getSelectionArgs() : null;
    final String sort = mSortOrder +
        (mLimit > 0 ? " LIMIT " + mLimit : "") +
        (mOffset > 0 ? " OFFSET " + mOffset : "");

    final Uri.Builder uriBuilder = (mId != -1 ? ContentUris.withAppendedId(mUri,
        mId) : mUri).buildUpon();
    final Uri uri = uriBuilder.build();

    return signal == null ?
        client.query(uri, mProjection, select, selArgs, sort) :
        client.query(uri, mProjection, select, selArgs, sort, signal);
  }

  /** The content uri. */
  @NonNull
  public final Uri getUri () {return mUri;}

}
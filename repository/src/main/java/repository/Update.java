/*
 * Update.java
 * cp-tools
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

package repository;

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Content Provider Update Builder.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 12/07/2017
 */
@SuppressWarnings("unused")
public final class Update implements Selectable<Update>, Valuesable<Update> {

  /** The resource uri. */
  @NonNull private final Uri mUri;

  /** The item id */
  private long mId = -1;

  /** The selection. */
  @Nullable private Selection mSelection = null;
  /** Insert content */
  @Nullable private ContentValues mValues = null;

  /**
   * Construct a new {@link Update}
   *
   * @param uri content resource
   */
  private Update (@NonNull Uri uri) {
    mUri = uri;
  }

  /**
   * Access to item.
   *
   * @return current builder
   */
  @NonNull
  public final Update item (long id) {
    mId = id;
    return this;
  }

  /**
   * Add bytes value.
   *
   * @return current builder
   */
  @NonNull
  public final Update values (@NonNull ContentValues values) {
    mValues = values;
    return this;
  }

  /**
   * Define selection.
   *
   * @return current builder
   */
  @NonNull
  public final Update select (@NonNull Selection selection) {
    mSelection = selection;
    return this;
  }

  /**
   * @param client content provider client
   *
   * @return content provider response
   **/
  public final int execute (@NonNull ContentProviderClient client)
      throws RemoteException {

    final String select = mSelection != null ? mSelection.getSelection() : null;
    final String[] selArgs =
        mSelection != null ? mSelection.getSelectionArgs() : null;

    final Uri.Builder uriBuilder = (mId != -1 ? ContentUris.withAppendedId(mUri,
        mId) : mUri).buildUpon();
    final Uri uri = uriBuilder.build();

    return client.update(uri, mValues, select, selArgs);
  }
}
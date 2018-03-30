/*
 * Insert.java
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
import android.content.ContentValues;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * ContentContracts Provider Insert Builder.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 12/07/2017
 */
@SuppressWarnings("unused")
public final class Insert implements Valuesable<Insert> {

  /** The resource uri. */
  @NonNull private final Uri mUri;

  /** Insert content */
  @Nullable private ContentValues mValues = null;

  /**
   * Construct a new {@link Insert}
   *
   * @param uri content resource
   */
  public Insert (@NonNull Uri uri) {
    mUri = uri;
  }

  /**
   * Add bytes value.
   *
   * @return current builder
   */
  @NonNull
  public final Insert values (@NonNull ContentValues values) {
    mValues = values;
    return this;
  }

  /**
   * @param client content provider client
   *
   * @return content provider response
   **/
  public final long execute (@NonNull ContentProviderClient client)
      throws RemoteException {
    final Uri.Builder uriBuilder = mUri.buildUpon();
    final Uri uri = uriBuilder.build();
    return ContentUris.parseId(client.insert(uri, mValues));
  }
}
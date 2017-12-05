/*
 * ContentContracts.java
 * content
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

package content;

import android.accounts.Account;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.content.ContentResolver.CURSOR_DIR_BASE_TYPE;
import static android.content.ContentResolver.CURSOR_ITEM_BASE_TYPE;
import static android.content.ContentResolver.SCHEME_CONTENT;
import static content.ContentContracts.ValueType.BLOB;
import static content.ContentContracts.ValueType.DOUBLE;
import static content.ContentContracts.ValueType.LONG;
import static content.ContentContracts.ValueType.NULL;
import static content.ContentContracts.ValueType.STRING;

/**
 * Content Contracts.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/07/2017
 */
@SuppressWarnings("unused")
final class ContentContracts {

  /** The account-stub. */
  static final Account ACCOUNT_STUB =
      new Account("noName", "noType");

  /** The content provider protocol. */
  static final String CONTENT_SCHEME = SCHEME_CONTENT + "://";

  /** The Account Name Query-Key. */
  private static final String ACCOUNT_NAME = "account_name";

  /** The Account Type Query-Key. */
  private static final String ACCOUNT_TYPE = "account_type";

  /** The Base Columns. */
  static final String[] BASE_COLUMNS = new String[]{ BaseColumns._ID};

  /** Standard content dir-type prefix. */
  static final String CONTENT_DIR_TYPE_PREFIX =
      CURSOR_DIR_BASE_TYPE + "/vnd.";

  /** Standard content item-type prefix. */
  static final String CONTENT_ITEM_TYPE_PREFIX =
      CURSOR_ITEM_BASE_TYPE + "/vnd.";

  /** The desc sort order suffix. */
  static final String DESC_SORT_ORDER_SUFFIX = " DESC";

  /** The asc sort order suffix. */
  static final String ASC_SORT_ORDER_SUFFIX = " ASC";


  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private ContentContracts() {throw new AssertionError();}


  /**
   * Creates an updated URI that includes query parameters that identify the
   * source as a sync adapter.
   *
   * @param builder source uri
   * @param account the account
   *
   * @return dest uri
   */
  @NonNull
  static Uri.Builder asSyncAdapter
  (@NonNull Uri.Builder builder, @NonNull Account account) {
    return builder
        .appendQueryParameter(ACCOUNT_NAME, account.name)
        .appendQueryParameter(ACCOUNT_TYPE, account.type);
  }

  /**
   * Check access mode.
   *
   * @param uri incoming uri
   * @return the account of owner or null
   */
  @Nullable
  @CheckResult
  static Account isSyncAdapter(@NonNull Uri uri) {
    try {
      return
          new Account (
              uri.getQueryParameter(ACCOUNT_NAME),
              uri.getQueryParameter(ACCOUNT_TYPE)
          );
    } catch(IllegalArgumentException exception) {return null;}
  }


  /**
   * The type of content value.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 11/09/2017
   */
  @IntDef({BLOB, STRING, LONG, DOUBLE, NULL})
  @Retention(RetentionPolicy.SOURCE)
  @interface ValueType {

    /** <b>BLOB</b>-value. */
    byte BLOB   = 0;

    /** <b>STRING</b>-value. */
    byte STRING = 1;

    /** <b>LONG</b>-value. */
    byte LONG   = 2;

    /** <b>DOUBLE</b>-value. */
    byte DOUBLE = 3;

    /** <b>NULL</b>-value. */
    byte NULL   = 4;

  }

}

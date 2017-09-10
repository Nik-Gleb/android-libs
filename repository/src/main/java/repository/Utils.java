/*
 * Utils.java
 * repository
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

import android.accounts.Account;
import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;

/**
 * Content Provider Tools.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/07/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Utils {

  /** The content provider protocol. */
  public static final String CONTENT_SCHEMA = "content://";

  /**
   * An optional insert, update or delete URI parameter that allows the caller
   * to specify that it is a sync adapter.
   * <p>
   * The default value is false. If set to true, the modified row is not marked
   * as "dirty" (needs to be synced) and when the provider calls
   * {@link ContentResolver#notifyChange(android.net.Uri,
   * android.database.ContentObserver, boolean)},
   * the third parameter "syncToNetwork" is set to false.
   * <p>
   * Furthermore, if set to true, the caller must also include
   * {@link Calendars#ACCOUNT_NAME} and
   * {@link Calendars#ACCOUNT_TYPE} as query parameters.
   *
   * @see Uri.Builder#appendQueryParameter(java.lang.String, java.lang.String)
   */
  private static final String CALLER_IS_SYNCADAPTER =
      CalendarContract.CALLER_IS_SYNCADAPTER;

  /** The Account Name Query-Key. */
  private static final String ACCOUNT_NAME = Calendars.ACCOUNT_NAME;
  /** The Account Type Query-Key. */
  private static final String ACCOUNT_TYPE = Calendars.ACCOUNT_TYPE;

  /** The Base Columns. */
  public static final String[] BASE_COLUMNS = new String[]{ BaseColumns._ID};

  /** Standard content-type prefix. */
  public static final String CONTENT_TYPE_PREFIX =
      "vnd.android.cursor.dir/vnd.";
  /** Standard content item-type prefix. */
  public static final String CONTENT_ITEM_TYPE_PREFIX =
      "vnd.android.cursor.item/vnd.";

  /** The desc sort order suffix. */
  public static final String DESC_SORT_ORDER_SUFFIX = " DESC";
  /** The asc sort order suffix. */
  public static final String ASC_SORT_ORDER_SUFFIX = " ASC";


  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Utils () {throw new AssertionError();}

  /**
   * Creates an updated URI that includes query parameters that identify the
   * source as a sync adapter.
   *
   * @param builder source uri
   * @param account the account
   *
   * @return dest uri
   */
  public static Uri.Builder asSyncAdapter (Uri.Builder builder,
      Account account) {
    return builder.appendQueryParameter(CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(ACCOUNT_NAME, account.name)
        .appendQueryParameter(ACCOUNT_TYPE, account.type);
  }

}

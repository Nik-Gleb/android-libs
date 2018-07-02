package data;

import android.content.ContentValues;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.provider.BaseColumns._ID;
import static data.DataSource.DATA;
import static data.DataSource.keyToId;
import static java.lang.System.arraycopy;

/**
 * Bulk Insert of data.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/07/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
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
   * @param uri content uri
   */
  BulkInsert(@NonNull DataSource source, @NonNull Uri uri)
  {mDataSource = source; mContentUri = uri; mContentValues = new ContentValues[0];}

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
   * @param key key of raw
   * @param value raw data
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull public final BulkInsert put(@NonNull String key, @NonNull byte[] value) {
    final ContentValues values = new ContentValues(2);
    values.put(_ID, keyToId(key)); values.put(DATA, value);
    mContentValues = addValue(mContentValues, values);
    return this;
  }

  /**
   * @param values content values
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull final BulkInsert put(@NonNull ContentValues values)
  {mContentValues = addValue(mContentValues, values); return this;}

  /** @return count of rows */
  @WorkerThread public final int execute()
  {return mDataSource.bulkInsert(mContentUri, mContentValues);}

  /**
   * @param values source array of values
   * @param value new value
   *
   * @return result array of values
   */
  @NonNull private static ContentValues[] addValue
  (@NonNull ContentValues[] values, @NonNull ContentValues value) {
    final ContentValues[] result = new ContentValues[values.length + 1];
    arraycopy(values, 0, result, 0, values.length);
    result[values.length] = value; return result;
  }
}

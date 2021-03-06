/*
 * DataResource.java
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.WorkerThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.content.ContentUris.withAppendedId;
import static android.text.TextUtils.isEmpty;
import static data.DataResource.Access.READ;
import static data.DataResource.Access.WRITE;
import static data.DataResource.Name.ASSETS;
import static data.DataResource.Name.CONTENT;
import static data.DataResource.Name.FILES;
import static data.DataResource.Name.HTTPS;
import static data.DataResource.Name.TABLES;
import static data.DataSource.MODE;
import static java.util.Objects.requireNonNull;

/**
 * Data Resource Description
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
@Keep@KeepPublicProtectedClassMembers
public final class DataResource {

  /** Content authority. */
  @Nullable static String AUTHORITY = null;

  /** Preferences name. */
  @Nullable private static String sPrefsName = null;

  /** Data Resource Roots */
  @Nullable private static DataResource
    sFiles = null, sHttps = null, sAssets = null, sTables = null, sPrefs = null;

  /** Data resource uri. */
  @NonNull final Uri uri;

  /**
   * Constructs a new {@link DataResource}.
   *
   * @param name  the resource name
   * @param auth  the resource auth
   */
  private DataResource(@Name @NonNull String name, @NonNull String auth)
  {uri = new Uri.Builder().scheme(name).authority(auth).build();}

  /** @param uri source uri. */
  private DataResource(@NonNull Uri uri) {this.uri = uri;}

  /**
   * @param auth authority
   *
   * @return content-resources
   */
  @NonNull public static DataResource content(@NonNull String auth)
  {return new DataResource(CONTENT, auth);}

  /** @return files-resources */
  @NonNull public static DataResource files() {
    return sFiles != null ? sFiles : (sFiles =
      new DataResource(FILES, requireNonNull(AUTHORITY)));
  }

  /** @return https-resources */
  @NonNull public static DataResource https() {
    return sHttps != null ? sHttps : (sHttps =
      new DataResource(HTTPS, requireNonNull(AUTHORITY)));
  }

  /** @return assets-resources */
  @NonNull public static DataResource assets() {
    return sAssets != null ? sAssets : (sAssets =
      new DataResource(ASSETS, requireNonNull(AUTHORITY)));
  }

  /** @return tables-resources */
  @NonNull public static DataResource tables() {
    return sTables != null ? sTables : (sTables =
      new DataResource(TABLES, requireNonNull(AUTHORITY)));
  }

  /** @return prefs-resources */
  @NonNull public static DataResource prefs() {
    return sPrefs != null ? sPrefs : (sPrefs = new DataResource(TABLES,
      requireNonNull(AUTHORITY)).path(requireNonNull(sPrefsName)));
  }

  /** @param authority content authority for static init */
  public static void init(@NonNull String authority, @NonNull String prefs) {
    if (isEmpty(AUTHORITY = authority)) throw new IllegalArgumentException();
    if (isEmpty(sPrefsName = prefs)) throw new IllegalArgumentException();
  }

  @NonNull public final DataResource path(@NonNull String path)
  {return new DataResource(uri.buildUpon().appendEncodedPath(path).build());}

  @NonNull public final DataResource query(@NonNull String key, @NonNull String value)
  {return new DataResource(uri.buildUpon().appendQueryParameter(key, value).build());}

  @NonNull public final DataResource row(long id)
  {return new DataResource(withAppendedId(uri, id));}

  public final DataResource key(@NonNull String key)
  {return row(DataSource.keyToId(key));}

  @NonNull final DataResource mime(@NonNull String type)
  {return new DataResource(uri.buildUpon().appendQueryParameter
      (DataSource.TYPE, type).build());}

  @NonNull public final DataResource read()
  {return new DataResource(uri.buildUpon()
    .appendQueryParameter(MODE, READ).build());}

  @NonNull public final DataResource write()
  {return new DataResource(uri.buildUpon()
      .appendQueryParameter(MODE, WRITE).build());}


  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof DataResource)) return false;
    final DataResource that = (DataResource) obj;
    return Objects.equals(uri, that.uri);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return Objects.hash(uri);}

  /** {@inheritDoc} */
  @Override public final String toString() {return uri.toString();}

  /**
   * @param source    data source
   * @param request   request body
   *
   * @return  response of request
   */
  @WorkerThread @NonNull public final ResponseBody call
  (@NonNull DataSource source, @NonNull RequestBody request)
  {return source.call(uri,request);}

  /**
   * @param source data source
   *
   * @return response of request
   */
  @WorkerThread @NonNull public final ResponseBody read(@NonNull DataSource source)
  {return source.read(uri.buildUpon().appendQueryParameter(MODE, READ).build());}

  /**
   * @param source data source
   * @param request request body
   */
  @WorkerThread public final void write
  (@NonNull DataSource source, @NonNull RequestBody request)
  {source.write(uri.buildUpon().appendQueryParameter(MODE, WRITE).build(), request);}

  /**
   * @param source data source
   *
   * @return resource descriptor
   */
  @WorkerThread
  @NonNull public final AssetFileDescriptor openFile
  (@NonNull DataSource source) {return source.openFile(uri, null);}

  /**
   * @param source data source
   *
   * @return resource descriptor
   */
  @WorkerThread
  @NonNull public final AssetFileDescriptor openFile
  (@NonNull DataSource source, @NonNull Bundle options)
  {return source.openFile(uri, options);}

  /**
   * @param source        data source
   *
   * @return              stream of values
   */
  @WorkerThread
  @NonNull public final <T> Optional<T> query
  (@NonNull DataSource source, @NonNull Function<byte[], T> mapper)
  {return source.query(uri, null, null, null, null,
    cursor -> mapper.apply(cursor.getBlob(1))).findFirst();}

  /**
   * @param source        data source
   *
   * @return              stream of values
   */
  @WorkerThread
  @NonNull public final <T> Stream<T> query
  (@NonNull DataSource source, @NonNull BiFunction<Integer, byte[], T> mapper)
  {return source.query(uri, null, null, null, null, cursor ->
      mapper.apply((int)cursor.getLong(0), cursor.getBlob(1)));}

  /**
   * @param source        data source
   * @param proj    columns projection
   * @param sel     selection params
   * @param args selection arguments
   * @param sort     sort order
   *
   * @return              stream of values
   */
  @WorkerThread
  @NonNull public final <T> Stream<T> query
  (@NonNull DataSource source, @Nullable String[] proj, @Nullable String sel,
  @Nullable String[] args, @Nullable String sort, @NonNull Function<Cursor, T> mapper)
  {return source.query(uri, proj, sel, args, sort, mapper);}

  /** {@inheritDoc} */
  @WorkerThread public final void put
  (@NonNull DataSource source, @NonNull byte[] raw)
  {source.put(uri, raw);}

  /**
   * @param source data source
   * @return data values-builder
   */
  @AnyThread
  @NonNull public final DataValues put(@NonNull DataSource source)
  {return new DataValues(source, this);}

  /**
   * @param source data source
   *
   * @return type of resource
   */
  @WorkerThread @NonNull public final
  String getType(@NonNull DataSource source)
  {return source.getType(uri);}

  /**
   * @param source data source
   * @param mimeTypeFilter mime-filter
   *
   * @return stream types of resource
   */
  @WorkerThread @NonNull public final String[] getStreamTypes
  (@NonNull DataSource source, @NonNull String mimeTypeFilter)
  {return source.getStreamTypes(uri, mimeTypeFilter);}

  /**
   * @param source data source
   * @param values content values
   *
   * @return result uri
   */
  @WorkerThread @NonNull public final Uri insert
  (@NonNull DataSource source, @Nullable ContentValues values)
  {return source.insert(uri, values);}

  /**
   * @param source  data source
   *
   * @return result count
   */
  @NonNull public final BulkInsert bulkInsert(@NonNull DataSource source)
  {return new BulkInsert(source, uri);}

  /**
   * @param source        data source
   * @param sel     selection string
   * @param args selection args
   *
   * @return result count
   */
  @WorkerThread public final int delete
  (@NonNull DataSource source, @Nullable String sel, @Nullable String[] args)
  {return source.delete(uri, sel, args);}

  /** @param source data source */
  @WorkerThread final void delete(@NonNull DataSource source) {source.delete(uri);}

  /**
     * @param source        data source
     * @param values        content values
     * @param sel     selection string
     * @param args selection args
     *
     * @return  count of values
     */
  @WorkerThread public final int update(@NonNull DataSource source,
  @Nullable ContentValues values, @Nullable String sel, @Nullable String[] args)
  {return source.update(uri, values, sel, args);}

  /**
   * @param source  data source
   * @param arg     method arguments
   * @param extras  extras
   *
   * @return  bundle result
   */
  @WorkerThread @NonNull public final Bundle call
  (@NonNull DataSource source, @Nullable String arg, @Nullable Bundle extras)
  {return source.call(Objects.requireNonNull(uri.getScheme()), arg, extras);}


  /** {@inheritDoc} */
  @NonNull public final ContentObserver register
  (@NonNull DataSource source, @NonNull BiConsumer<Boolean, Uri> observer)
  {return register(source, observer, null, true, true);}

  /** {@inheritDoc} */
  @NonNull public final ContentObserver register
  (@NonNull DataSource source, @NonNull BiConsumer<Boolean, Uri> observer,
      @Nullable Handler handler, boolean selfNotify, boolean descedants)
  {return source.register(uri, observer, handler, selfNotify, descedants);}

  /** {@inheritDoc} */
  public final void unregister
  (@NonNull DataSource source, @NonNull ContentObserver observer)
  {source.unregister(observer);}

  /**
   * @param source data source
   *
   * @return Intent with data
   */
  @WorkerThread
  @NonNull public final Intent getIntent
  (@NonNull DataSource source, @NonNull String action) {
    final Intent result = source.getIntent(uri).setAction(action);
    if (action.startsWith("android.media.action"))
      result.setDataAndType(null, null);
    return result;
  }

  /**
   * @param key bundle key
   * @param bundle bundle batch
   *
   * @return bundle batch
   */
  @NonNull public final Bundle pack(@NonNull String key, @NonNull Bundle bundle)
  {bundle.putParcelable(key, uri); return bundle;}

  /**
   * @param key bundle key
   * @param bundle bundle batch
   *
   * @return unpacked data resource or null
   */
  @Nullable public static DataResource unpack(@NonNull String key, @NonNull Bundle bundle)
  {final Uri uri = bundle.getParcelable(key); return uri != null ? new DataResource(uri) : null;}

  /**
   * @param descriptor asset-file descriptor
   *
   * @return media type
   */
  @Nullable public static MediaType type(@NonNull AssetFileDescriptor descriptor)
  {return OkUtils.mediaType(descriptor.getExtras());}

  /**
   * @param source data source
   *
   * @return resources stream
   */
  @NonNull public final Stream<DataResource> stream(@NonNull DataSource source)
  {return source.bind(uri).map(DataResource::new);}

  /**
   * Predefined source names.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @StringDef({ CONTENT, HTTPS, FILES, ASSETS, TABLES})
  @Retention(RetentionPolicy.SOURCE) @interface Name
  {String CONTENT = ContentResolver.SCHEME_CONTENT,
      HTTPS = "https", FILES = "files",
      ASSETS = "assets", TABLES = "tables";}

  /**
   * Predefined source names.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @StringDef({ READ, WRITE})
  @Retention(RetentionPolicy.SOURCE) @interface Access
  {String READ = "r", WRITE = "w";}

  /** Data Values Builder. */
  @Keep@KeepPublicProtectedClassMembers
  public static final class DataValues {

    /** Data source. */
    private final DataSource mSource;

    /** Data resource. */
    private final DataResource mResource;

    /** Content values. */
    private final ContentValues mValues = new ContentValues();

    /**
     * Constructs a new {@link DataValues}.
     *
     * @param source    data source
     * @param resource  data resource
     */
    DataValues
    (@NonNull DataSource source, @NonNull DataResource resource)
    {mSource = source; mResource = resource;}

    /** @return this builder, to allow for chaining. */
    @NonNull public final DataValues value(@NonNull String key)
    {try {return this;} finally {mValues.putNull(key);}}

    /** @return this builder, to allow for chaining. */
    @NonNull public final DataValues value(@NonNull String key, @NonNull String value)
    {try {return this;} finally {mValues.put(key, value);}}

    /** @return this builder, to allow for chaining. */
    @NonNull public final DataValues value(@NonNull String key, long value)
    {try {return this;} finally {mValues.put(key, value);}}

    /** @return this builder, to allow for chaining. */
    @NonNull public final DataValues value(@NonNull String key, double value)
    {try {return this;} finally {mValues.put(key, value);}}

    /** @return this builder, to allow for chaining. */
    @NonNull public final DataValues value(@NonNull String key, @NonNull byte[] value)
    {try {return this;} finally {mValues.put(key, value);}}


    /** @return inserted uri-resource. */
    @NonNull public final Uri insert()
    {return mResource.insert(mSource, mValues);}

    /** @return count of updated. */
    public final int update()
    {return mResource.update(mSource, mValues, null, null);}

    /** @return count of updated. */
    public final int update(@NonNull String selection, @Nullable String... args)
    {return mResource.update(mSource, mValues, selection, args);}

  }
}

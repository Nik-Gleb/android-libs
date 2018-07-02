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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.content.ContentUris.withAppendedId;
import static data.DataResource.Access.READ;
import static data.DataResource.Access.WRITE;
import static data.DataSource.MODE;

/**
 * Data Resource Description
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
@Keep@KeepPublicProtectedClassMembers
public final class DataResource {

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
  {return new DataResource(Name.CONTENT, auth);}

  /**
   * @param auth authority
   *
   * @return files-resources
   */
  @NonNull public static DataResource files(@NonNull String auth)
  {return new DataResource(Name.FILES, auth);}

  /**
   * @param auth authority
   *
   * @return https-resources
   */
  @NonNull public static DataResource https(@NonNull String auth)
  {return new DataResource(Name.HTTPS, auth);}

  /**
   * @param auth authority
   *
   * @return assets-resources
   */
  @NonNull public static DataResource assets(@NonNull String auth)
  {return new DataResource(Name.ASSETS, auth);}

  /**
   * @param auth authority
   *
   * @return tables-resources
   */
  @NonNull public static DataResource tables(@NonNull String auth)
  {return new DataResource(Name.TABLES, auth);}


  @NonNull public final DataResource path(@NonNull String path)
  {return new DataResource(uri.buildUpon().appendEncodedPath(path).build());}

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
   * @param executor  request executor
   *
   * @return  response of request
   */
  @WorkerThread
  @NonNull public final ResponseBody call(@NonNull DataSource source,
      @NonNull RequestBody request, @NonNull Executor executor) throws IOException
  {return source.call(uri,request, executor);}

  /**
   * @param source data source
   *
   * @return response of request
   */
  @WorkerThread
  @NonNull public final ResponseBody read(@NonNull DataSource source) throws IOException
  {return source.read(uri.buildUpon().appendQueryParameter(MODE, READ).build());}

  /**
   * @param source data source
   * @param request request body
   */
  @WorkerThread public final void write
  (@NonNull DataSource source, @NonNull RequestBody request) throws IOException
  {source.write(uri.buildUpon().appendQueryParameter(MODE, WRITE).build(), request);}


  /**
   * @param source data source
   *
   * @return resource descriptor
   */
  @WorkerThread
  @NonNull public final AssetFileDescriptor openFile
  (@NonNull DataSource source) throws IOException
  {return source.openFile(uri, null);}

  /**
   * @param source data source
   *
   * @return resource descriptor
   */
  @WorkerThread
  @NonNull public final AssetFileDescriptor openFile
  (@NonNull DataSource source, @NonNull Bundle options) throws IOException
  {return source.openFile(uri, options);}

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
  {return source.call(uri.getScheme(), arg, extras);}


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
  (@NonNull DataSource source, @NonNull String action) throws IOException {
    final Intent result = source.getIntent(uri).setAction(action);
    if (action.startsWith("android.media.action"))
      result.setDataAndType(null, null);
    return result;
  }

  /**
   * Predefined source names.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @StringDef({Name.CONTENT, Name.HTTPS, Name.FILES, Name.ASSETS, Name.TABLES})
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
}

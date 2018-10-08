/*
 * HttpsProvider.java
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

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

import static data.OkUtils.fromResponse;
import static data.Provider.isCanceled;
import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
@SuppressWarnings("unused") final class HttpsProvider implements Provider {

  /** Http client. */
  private final OkHttpClient mClient;

  /** Web host. */
  @NonNull private final String mAuthority;

  /**
   * Constructs a new {@link HttpsProvider}.
   *
   * @param context   app-context
   * @param authority authority
   */
  HttpsProvider(@NonNull Context context, @NonNull String authority) {
    final HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

    final Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(64); dispatcher.setMaxRequestsPerHost(32);
    final ConnectionPool pool = new ConnectionPool(4, 30L, TimeUnit.SECONDS);
    mClient =
      new OkHttpClient.Builder()
        .cache(null)
        //.addInterceptor(logging)
        .dispatcher(dispatcher)
        .connectionPool(pool)
        .connectTimeout(1L, TimeUnit.SECONDS)
        .readTimeout(1L, TimeUnit.SECONDS)
        .writeTimeout(1L, TimeUnit.SECONDS)
        .build();
    mAuthority = authority;
  }

  /** {@inheritDoc} */
  @NonNull @Override public final ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException
  {return openFile(uri, mode, null);}

  /** {@inheritDoc} */
  @NonNull @Override public final ParcelFileDescriptor openFile(@NonNull Uri uri,
    @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException
  {return perform(uri, mAuthority, null, mClient, signal).getParcelFileDescriptor();}

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException
  {return openAssetFile(uri, mode, null);}

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal)
    throws FileNotFoundException {return perform(uri, mAuthority, null, mClient, signal);}

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts)
    throws FileNotFoundException {return openTypedAssetFile(uri, filter, opts, null);}

  /** {@inheritDoc} */
  @NonNull @Override public AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts,
    @Nullable CancellationSignal signal) throws FileNotFoundException {
    if ("*/*".equals(filter) /*|| compareMimeTypes(getType(uri), filter)*/)
      return perform(uri, mAuthority, opts, mClient, signal);
    else throw new FileNotFoundException("Can't open " + uri + " as type " + filter);
  }

  /**
   * @return asset file descriptor
   */
  @SuppressWarnings("RedundantThrows")
  @NonNull private static AssetFileDescriptor perform
  (@NonNull Uri uri, @NonNull String host, @Nullable Bundle bundle, @NonNull OkHttpClient client,
    @Nullable CancellationSignal signal) throws FileNotFoundException {isCanceled(signal);
    final Request request = new Request.Builder().url(normalizeUri(uri, host)).build();
    try {return fromResponse(requireNonNull(client.newCall(request).execute().body()));}
    catch (IOException exception) {throw new OperationCanceledException(exception.getMessage());}
  }

  /**
   * @param uri source uri
   * @param host web host
   *
   * @return full url
   */
  private static String normalizeUri(@NonNull Uri uri, @NonNull String host) {
    final Set<String> keys = new HashSet<>(uri.getQueryParameterNames());
    final String mode = cutQuery(uri, keys, "mode");
    final String type = cutQuery(uri, keys, "type");
    final Uri.Builder builder = uri.buildUpon()
      .clearQuery().authority(host);
    for (final String key : keys)
      builder.appendQueryParameter
        (key, uri.getQueryParameter(key));
    return builder.build().toString();
  }

  /**
   * @param uri  uri resource
   * @param keys query keys
   * @param key  search key
   *
   * @return query value
   */
  @Nullable private static String cutQuery
  (@NonNull Uri uri, @NonNull Set<String> keys, @NonNull String key) {
    if (!keys.contains(key)) return null;
    else try {return uri.getQueryParameter(key);}
    finally {keys.remove(key);}
  }

}

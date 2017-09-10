/*
 * NetworkStorage.java
 * network
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

package network;

import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.IOException;
import java.util.Set;

import ok.OkUtils;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static network.NetworkContracts.BUNDLE_BODY_LENGTH;
import static network.NetworkContracts.BUNDLE_FILE_BODY;
import static network.NetworkContracts.BUNDLE_FORM_BODY;
import static network.NetworkContracts.BUNDLE_HEADERS;
import static network.NetworkContracts.BUNDLE_MEDIA_TYPE;
import static network.NetworkContracts.BUNDLE_MULTIPART_BODY;
import static network.NetworkContracts.BUNDLE_REQUEST_BODY;
import static network.NetworkContracts.BUNDLE_STRING_BODY;

/**
 * Network storage.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class NetworkStorage {

  /** The uri path-divider */
  private static final String PATH_DIVIDER = "/";
  /** Offset indexes. */
  private static final int FROM_INDEX = 1, CODE_POINT = 0,
      PATH_CODE = PATH_DIVIDER.codePointAt(CODE_POINT);

  /** The backend's params. */
  private final String mScheme, mHost, mPath;
  /** The ok http client instance. */
  private final OkHttpClient mClient;

  /**
   * Constructs a new {@link NetworkStorage} by copying the contents of a {@link Builder}
   *
   * @param builder the builder for this interceptor
   */
  private NetworkStorage(Builder builder) {
    mScheme = builder.scheme; mHost = builder.host;  mPath = builder.path;
    mClient = builder.okHttpClient;
  }

  /**
   * Create a {@link Builder} suitable for building a {@link NetworkStorage}.
   *
   * @param client the ok-http client instance
   * @return a {@link Builder} instance
   */
  @SuppressWarnings("WeakerAccess")
  public static Builder create(OkHttpClient client) {
    return new Builder(client);
  }


  /**
   * @param uri the source uri
   * @return target http-url
   */
  final HttpUrl convertUri(Uri uri) {
    final String encodedPath = PATH_DIVIDER + mPath;
    final String path = uri.getEncodedPath();
    if (!TextUtils.isEmpty(path) && path.length() != 1) {
      final int pos = path.indexOf(PATH_CODE, FROM_INDEX) + 1;
      if (pos >= 3) {
        final String segments = path.substring(pos);
        if (!TextUtils.isEmpty(segments))
          return new HttpUrl.Builder()
              .scheme(mScheme).host(mHost)
              .encodedPath(encodedPath)
              .addEncodedPathSegments(segments)
              .encodedQuery(uri.getEncodedQuery())
              .build();
      }
    }
    throw new IllegalArgumentException("Invalid uri: " + uri);
  }

  /**
   * @param hdrs the request headers
   * @param opt the bundled request body
   *
   * @return the ok-http compatibility request body
   */
  @SuppressWarnings("WeakerAccess")
  static RequestBody parseRequest(Headers.Builder hdrs, Bundle opt) {

    if (opt == null || opt.isEmpty()) return null;

    Bundle bundle = opt.getBundle(BUNDLE_HEADERS);
    if (bundle != null && !bundle.isEmpty())
      parseHeaders(hdrs, bundle);

    RequestBody requestBody = null;
    bundle = opt.getBundle(BUNDLE_FORM_BODY);
    if (bundle != null && !bundle.isEmpty())
      requestBody = parseFormBody(bundle);
    else {
      bundle = opt.getBundle(BUNDLE_MULTIPART_BODY);
      if (bundle != null && !bundle.isEmpty())
        requestBody = parseMultiPartBody(bundle);
      else {
        bundle = opt.getBundle(BUNDLE_REQUEST_BODY);
        if (bundle != null && !bundle.isEmpty())
          requestBody = parseRequestBody(bundle);
      }
    }

    return requestBody;
  }

  /**
   * @param hdrs the headers container
   * @param headers the bundle headers
   */
  @SuppressWarnings("WeakerAccess")
  static void parseHeaders(Headers.Builder hdrs, Bundle headers) {
    final Set<String> keys = headers.keySet();
    for (String key: keys) {
      final String value = headers.getString(key);
      if (!TextUtils.isEmpty(value)) hdrs.add(key, value);
    }
  }

  /**
   * @param form form data
   * @return request body
   */
  @SuppressWarnings("WeakerAccess")
  static RequestBody parseFormBody(Bundle form) {
    final FormBody.Builder formBody = new FormBody.Builder();
    final Set<String> keys = form.keySet();
    for (String key : keys) {
      final String value = form.getString(key);
      if (!TextUtils.isEmpty(value))
        formBody.add(key, value);
    }
    return formBody.build();
  }

  private static RequestBody parseMultiPartBody(Bundle multipart) {
    throw new IllegalArgumentException("invalid request body " + multipart);
  }

  /**
   * @param request bundle request container
   * @return the request body
   */
  @SuppressWarnings("WeakerAccess")
  static RequestBody parseRequestBody(Bundle request) {
    final String strMediaType = request.getString(BUNDLE_MEDIA_TYPE);
    if (TextUtils.isEmpty(strMediaType))
      throw new IllegalArgumentException(BUNDLE_MEDIA_TYPE + " == null");
    final MediaType mediaType = MediaType.parse(strMediaType);
    if (mediaType == null) throw
        new IllegalArgumentException("invalid " + BUNDLE_MEDIA_TYPE + " " + strMediaType);
    final String message = request.getString(BUNDLE_STRING_BODY);
    if (!TextUtils.isEmpty(message)) return RequestBody.create(mediaType, message);
    else {
      final Parcelable parcelable = request.getParcelable(BUNDLE_FILE_BODY);
      if (parcelable != null && parcelable instanceof ParcelFileDescriptor) {
        final ParcelFileDescriptor parcelFileDescriptor = (ParcelFileDescriptor) parcelable;
        final ParcelFileDescriptor.AutoCloseInputStream inputStream =
            new ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor);
        return new OkUtils.StreamBody(mediaType, inputStream,
            request.getLong(BUNDLE_BODY_LENGTH, -1));
      } else throw new IllegalArgumentException("invalid request body " + request);
    }
  }

  /**
   * Make Http-Call.
   *
   * @param uri the source uri
   * @param method the http-method
   * @param options the request options
   * @param sig the cancellation signal
   * @return the asset file descriptor as result
   * @throws IOException by any fails in a network
   */
  @SuppressWarnings("unused")
  public final AssetFileDescriptor request(Uri uri, String method,
      Bundle options, CancellationSignal sig) throws IOException {
    final HttpUrl url = convertUri(uri);
    final Headers.Builder headers = new Headers.Builder();
    final RequestBody body = parseRequest(headers, options);
    final Request request = new Request.Builder().url(url)
        .method(method, body).headers(headers.build()).build();
    return NetworkUtils.perform(mClient.newCall(request), sig);
  }
  /**
   * Used to add parameters to a {@link NetworkStorage}.
   *
   * The {@link Builder} is first created by calling {@link #create(OkHttpClient)}.
   *
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is allowed.
   * Call {@link #build()} to create the {@link NetworkStorage} once all the parameters
   * have been supplied.
   */
  @SuppressWarnings("WeakerAccess")
  public static final class Builder {

    /** The backend's params. */
    String scheme, host, path;

    /** The ok http client. */
    final OkHttpClient okHttpClient;

    Builder(OkHttpClient client) {okHttpClient = client;}

    /**
     * @param scheme the backend's scheme <b>(REQUIRED)</b>
     * @return this builder, to allow for chaining.
     */
    public final Builder scheme(String scheme)
    {this.scheme = scheme; return this;}

    /**
     * @param host the backend's host <b>(REQUIRED)</b>
     * @return this builder, to allow for chaining.
     */
    public final Builder host(String host)
    {this.host = host; return this;}

    /**
     * @param path the backend's path <b>(REQUIRED)</b>
     * @return this builder, to allow for chaining.
     */
    public final Builder path(String path)
    {this.path = path; return this;}

    /** Create a {@link NetworkStorage} from this {@link Builder}. */
    public final NetworkStorage build() {

      if (scheme == null) throw new IllegalStateException("scheme == null");
      if (host == null) throw new IllegalStateException("host == null");
      if (path == null) throw new IllegalStateException("path == null");

      final Builder builder = this; return new NetworkStorage(builder);
    }
  }
}
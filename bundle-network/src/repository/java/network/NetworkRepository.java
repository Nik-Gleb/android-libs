/*
 * NetworkRepository.java
 * network
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

package network;

import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import java.util.Map;

import static network.NetworkContracts.BUNDLE_BODY_LENGTH;
import static network.NetworkContracts.BUNDLE_FILE_BODY;
import static network.NetworkContracts.BUNDLE_FORM_BODY;
import static network.NetworkContracts.BUNDLE_HEADERS;
import static network.NetworkContracts.BUNDLE_MEDIA_TYPE;
import static network.NetworkContracts.BUNDLE_REQUEST_BODY;
import static network.NetworkContracts.BUNDLE_STRING_BODY;

/**
 * Network Repository.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class NetworkRepository {

  /** Content scheme */
  private static final String SCHEME = "content";

  /** The content provider authority. */
  private final String mAuthority;
  /** The content provider path. */
  private final String mPath;

  /**
   * Constructs a new {@link NetworkRepository} by copying the contents of a {@link Builder}
   *
   * @param builder the builder for this interceptor
   */
  private NetworkRepository(Builder builder) {
    mAuthority = builder.authority;
    mPath = builder.path;
  }

  /**
   * Create a {@link Builder} suitable for building a {@link NetworkRepository}.
   *
   * @return a {@link Builder} instance
   */
  @SuppressWarnings("WeakerAccess")
  public static Builder create(String authority)
  {return new Builder(authority);}

  /** @return new created request-uri */
  public Uri.Builder newUri()
  {return new Uri.Builder().scheme(SCHEME).authority(mAuthority).encodedPath(mPath);}

  /**
   * @param bundle options container
   * @param type the content type
   * @param body the content body
   */
  public void newRequestBody (Bundle bundle, String type, String body) {
    final Bundle result = new Bundle();
    bundle.putBundle(BUNDLE_REQUEST_BODY, result);
    result.putString(BUNDLE_MEDIA_TYPE, type);
    result.putString(BUNDLE_STRING_BODY, body);
  }

  /**
   * @param bundle options container
   * @param type the content type
   * @param body the content body
   */
  public void newRequestBody (Bundle bundle, String type,
      ParcelFileDescriptor body, long length) {
    final Bundle result = new Bundle();
    bundle.putBundle(BUNDLE_REQUEST_BODY, result);
    result.putString(BUNDLE_MEDIA_TYPE, type);
    result.putParcelable(BUNDLE_FILE_BODY, body);
    result.putLong(BUNDLE_BODY_LENGTH, length);
  }

  /**
   * @param bundle options container
   * @param map the map of content
   */
  public void newFormBody (Bundle bundle, Map<String, String> map) {
    final Bundle result = new Bundle();
    bundle.putBundle(BUNDLE_FORM_BODY, result);
    for (Map.Entry<String, String> entry : map.entrySet())
      result.putString(entry.getKey(), entry.getValue());
  }

  /**
   * @param bundle options container
   * @param map the map of headers
   */
  public void newHeaders (Bundle bundle, Map<String, String> map) {
    final Bundle result = new Bundle();
    bundle.putBundle(BUNDLE_HEADERS, result);
    for (Map.Entry<String, String> entry : map.entrySet())
      result.putString(entry.getKey(), entry.getValue());
  }

  /**
   * Used to add parameters to a {@link NetworkRepository}.
   *
   * The {@link Builder} is first created by calling {@link #create(String)}.
   *
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is allowed.
   * Call {@link #build()} to create the {@link NetworkRepository} once all the parameters
   * have been supplied.
   */
  @SuppressWarnings({"WeakerAccess", "unused"})
  public static final class Builder {

    /** Default content provider path. */
    private static final String DEFAULT_PATH = "api";

    /** The content provider authority. */
    final String authority;
    /** The content provider path. */
    String path;

    /**
     * Constructs a new {@link Builder} with an authority.
     *
     * @param authority the content - authority
     */
    private Builder(String authority) {
      this.authority = authority;
    }

    /**
     * @param path the content path <b>(REQUIRED)</b>
     * @return this builder, to allow for chaining.
     */
    @SuppressWarnings("SameParameterValue")
    public final Builder path(String path)
    {this.path = path; return this;}

    /** Create a {@link NetworkRepository} from this {@link Builder}. */
    public final NetworkRepository build() {

      if (TextUtils.isEmpty(path)) path = DEFAULT_PATH;
      final Builder builder = this; return new NetworkRepository(builder);
    }
  }
}
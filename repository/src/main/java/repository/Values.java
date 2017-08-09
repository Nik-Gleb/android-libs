/*
 * Values.java
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

import android.content.ContentValues;
import android.support.annotation.NonNull;

/**
 * Used to add parameters to a {@link ContentValues}.
 * <p>
 * The {@link Values} is first created by calling {@link #Values(int)}.
 * <p>
 * The value() methods can then be used to add parameters to the builder.
 * See the specific methods to find for which {@link Values} type each is
 * allowed.
 * Call {@link #build} to create the {@link ContentValues} once all the
 * parameters have been supplied.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 10/11/2016
 */
@SuppressWarnings("WeakerAccess, unused")
public final class Values {

  /** The content values */
  @NonNull private final ContentValues mContentValues;

  /**
   * Constructs a new {@link Values} of a given size.
   * The account may be null.
   *
   * @param size the size ov values set
   */
  public Values (int size) {mContentValues = new ContentValues(size);}

  /** Create a ProviderValues from this {@link Values}. */
  @NonNull
  public ContentValues build () {return mContentValues;}

  /**
   * Add byte-array value to set.
   *
   * @param key the name of the value to put
   * @param value the data for the value to put
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final Values value (@NonNull String key, byte[] value) {
    mContentValues.put(key, value);
    return this;
  }

  /**
   * Add string value to set.
   *
   * @param key the name of the value to put
   * @param value the data for the value to put
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final Values value (@NonNull String key, String value) {
    mContentValues.put(key, value);
    return this;
  }

  /**
   * Add long value to set.
   *
   * @param key the name of the value to put
   * @param value the data for the value to put
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final Values value (@NonNull String key, long value) {
    mContentValues.put(key, value);
    return this;
  }

  /**
   * Add double value to set.
   *
   * @param key the name of the value to put
   * @param value the data for the value to put
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final Values value (@NonNull String key, double value) {
    mContentValues.put(key, value);
    return this;
  }

  /**
   * Add null value to set.
   *
   * @param key the name of the value to put
   *
   * @return this builder, to allow for chaining.
   */
  @NonNull
  public final Values empty (@NonNull String key) {
    mContentValues.putNull(key);
    return this;
  }

}
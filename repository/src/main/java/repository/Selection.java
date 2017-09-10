/*
 * Selection.java
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

import android.database.DatabaseUtils;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/**
 * Content Provider Selection Container.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 09/11/2016
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Selection {

  /** The selection. */
  @Nullable private final String mSelection;

  /** The selection args. */
  @Nullable private final String[] mSelectionArgs;

  /**
   * Constructs a new {@link Selection} by builder
   *
   * @param builder the builder
   */
  private Selection (@NonNull Builder builder) {
    mSelection = builder.mSelection;
    mSelectionArgs = builder.mSelectionArgs;
  }

  /**
   * Create a {@link Builder} suitable for building a selection {@link
   * Selection}.
   *
   * @return a {@link Builder}
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static Builder create () {
    return new Builder();
  }

  /** @return {@link #mSelection} */
  @Nullable
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public final String getSelection () {
    return mSelection;
  }

  /** @return {@link #mSelectionArgs} */
  @Nullable
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public final String[] getSelectionArgs () {
    return mSelectionArgs;
  }

  /**
   * Used to add parameters to a {@link Selection}.
   * <p>
   * The {@link Builder} is first created by calling {@link #create()}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to create the {@link Selection} once all the parameters
   * have been
   * supplied.
   */
  public static final class Builder {

    /** The selection. */
    @Nullable private String mSelection = null;

    /** The selection args. */
    @Nullable private String[] mSelectionArgs = null;

    /** Constructs a new {@link Builder}. */
    private Builder () {}

    /** Create a ProviderSelection from this {@link Builder}. */
    @NonNull
    public Selection build () {
      final Builder builder = this;
      return new Selection(builder);
    }

    /**
     * Selection condition.
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public final Builder where (@NonNull String selection,
        @NonNull String... args) {
      if (mSelection != null) {
        selection = DatabaseUtils.concatenateWhere(mSelection, selection);
      }
      if (mSelectionArgs != null) {
        args = DatabaseUtils.appendSelectionArgs(mSelectionArgs, args);
      }
      mSelection = selection;
      mSelectionArgs = args;
      return this;
    }

  }
}
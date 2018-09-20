/*
 * Selected.java
 * camera
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

package camera;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;

import java.util.Objects;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 08/09/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
final class SelectedInternal<T> {

  /** Index of selected item. */
  @IntRange(from = 0)
  public final int index;

  /** SelectedProfile item. */
  @NonNull public final T item;

  /** Hash code. */
  private final int mHash;

  /**
   * Constructs a new {@link SelectedInternal}
   *
   * @param index selected index
   * @param item selected item
   */
  @VisibleForTesting() SelectedInternal(@IntRange(from = 0) int index, @Nullable T item)
  {this.index = index; this.item = Objects.requireNonNull(item);
    mHash = 31 * (31 + index) + item.hashCode();}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SelectedInternal)) return false;
    final SelectedInternal state = (SelectedInternal) obj;
    return index == state.index &&
      Objects.equals(item, state.item);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return mHash;}

  /** {@inheritDoc} */
  @Override public final String toString()
  {return "State{" + "index=" + index + ", item=" + item + '}';}

  /**
   * @param items new items set
   * @return new related selection
   */
  @Nullable public final SelectedInternal<T> items
  (@Nullable ArraySet<T> items)
  {return items(items, this.item, this.index);}

  /**
   * @param items current items set
   * @param index new selected index
   * @param <T> the type of items
   *
   * @return selected instance if possible
   */
  @Nullable public static <T> SelectedInternal<T> create
    (@Nullable ArraySet<T> items, int index)
  {return select(items, index);}

  /**
   * @param items current items set
   * @param <T> the type of items
   *
   * @return selected instance if possible
   */
  @Nullable public static <T> SelectedInternal<T> create
  (@Nullable ArraySet<T> items)
  {return items(items, null, 0);}

  /**
   * @param item previous item
   * @param index previous index
   * @param <T> the type of item
   *
   * @return selected state
   */
  @NonNull public static <T> SelectedInternal<T> create
  (@Nullable T item, @IntRange(from = 0) int index)
  {return new SelectedInternal<>(index, item);}

  /**
   * @param items current items set
   * @param index new selected index
   * @param <T> the type of items
   *
   * @return selected instance if possible
   */
  @VisibleForTesting()
  @Nullable static <T> SelectedInternal<T> select(@Nullable ArraySet<T> items, int index) {
    try {return new SelectedInternal<>(index, Objects.requireNonNull(items).valueAt(index));}
    catch (ArrayIndexOutOfBoundsException | NullPointerException e) {return null;}
  }

  /**
   * @param items new items for update
   * @param item previous item
   * @param index previous index
   * @param <T> the type of item
   *
   * @return selected state
   */
  @VisibleForTesting()
  @Nullable static <T> SelectedInternal<T> items
  (@Nullable ArraySet<T> items, @Nullable T item, @IntRange(from = 0) int index) {
    if (item == null) return select(items, 0);
    if (items == null || items.isEmpty()) return null;
    final int position = items.indexOf(item);
    if (position > 0) return new SelectedInternal<>(position, item);
    final SelectedInternal<T> reselected = select(items, index);
    if (reselected != null) return reselected;
    final int last = items.size() - 1;
    return new SelectedInternal<>(last, items.valueAt(last));
  }
}

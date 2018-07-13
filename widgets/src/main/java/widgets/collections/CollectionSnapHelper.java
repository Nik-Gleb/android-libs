/*
 * CollectionSnapHelper.java
 * widgets
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

package widgets.collections;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;

import java.io.Closeable;
import java.util.function.Supplier;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static java.util.Objects.requireNonNull;

/**
 * Collection Snap Helper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 29/04/2018
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep@KeepPublicProtectedClassMembers
public final class CollectionSnapHelper
    extends LinearSnapHelper implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "CollectionSnapHelper";

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /** Attached {@link RecyclerView} */
  @Nullable private RecyclerView mRecyclerView = null;

  /**
   * Constructs a new {@link CollectionSnapHelper}.
   *
   * @param scroller smooth scroller
   */
  public CollectionSnapHelper
  (@NonNull Supplier<RecyclerView.SmoothScroller>[] scroller)
  {scroller[0] = () -> requireNonNull(createScroller(requireNonNull(mRecyclerView).getLayoutManager()));}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void attachToRecyclerView
    (@Nullable RecyclerView recyclerView) throws IllegalStateException {
    super.attachToRecyclerView(recyclerView);
    if (mRecyclerView == recyclerView) return;
    if (mRecyclerView != null) mRecyclerView = null;
    mRecyclerView = recyclerView;
    if (mRecyclerView != null) mRecyclerView = mRecyclerView;
  }
}

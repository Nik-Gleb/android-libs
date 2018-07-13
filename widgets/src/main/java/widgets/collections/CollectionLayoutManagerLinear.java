/*
 * CollectionLayoutManagerLinear.java
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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.SmoothScroller;
import android.util.AttributeSet;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 10/05/2018
 */
@Keep
@KeepPublicProtectedClassMembers
public final class CollectionLayoutManagerLinear extends LinearLayoutManager {

  /** Without predictions class. */
  private boolean mWithoutPredictions = false;

  /** Post-Layout task. */
  @Nullable private SmoothScroller mScroller = null;

  /**
   * Creates a vertical LinearLayoutManager
   *
   * @param context Current context, will be used to access resources.
   */
  public CollectionLayoutManagerLinear(@NonNull Context context)
  {super(context); setItemPrefetchEnabled(false);}

  /**
   * @param context       Current context, will be used to access resources.
   * @param orientation   Layout orientation. Should be {@link #HORIZONTAL} or {@link
   *                      #VERTICAL}.
   * @param reverseLayout When set to true, layouts from end to start.
   */
  public CollectionLayoutManagerLinear
  (@NonNull Context context, int orientation, boolean reverseLayout)
  {super(context, orientation, reverseLayout); setItemPrefetchEnabled(false);}

  /**
   * Constructor used when layout manager is set in XML by RecyclerView attribute
   * "layoutManager". Defaults to vertical orientation.
   */
  public CollectionLayoutManagerLinear
  (@NonNull Context context, @NonNull AttributeSet attrs, int defStyleAttr, int defStyleRes)
  {super(context, attrs, defStyleAttr, defStyleRes); setItemPrefetchEnabled(false);}

  /** Skip layout predictions */
  public final void skipPredictions() {mWithoutPredictions = true;}

  /** @param scroller for post-layout execute */
  public final void postScroller(@NonNull SmoothScroller scroller)
  {mScroller = scroller;}

  /** {@inheritDoc} */
  @Override public final boolean supportsPredictiveItemAnimations()
  {return !mWithoutPredictions && super.supportsPredictiveItemAnimations();}

  /** {@inheritDoc} */
  @Override public final void onLayoutCompleted(RecyclerView.State state) {
    super.onLayoutCompleted(state); mWithoutPredictions = false;
    if (mScroller == null) return; startSmoothScroll(mScroller); mScroller = null;
  }

}

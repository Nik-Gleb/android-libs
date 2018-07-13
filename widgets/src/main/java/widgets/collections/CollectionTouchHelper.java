/*
 * CollectionTouchHelper.java
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
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import java.io.Closeable;
import java.util.function.BiConsumer;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Collection Touch Helper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 16/01/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
final class CollectionTouchHelper
    extends RecyclerView.SimpleOnItemTouchListener implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "CollectionTouchHelper";

  /** Selection listener. */
  @NonNull final BiConsumer<RecyclerView, Integer> listener;

  /** The internal callback. */
  private final Callback mCallback;

  /** The gesture detector compat. */
  private final CollectionGestureDetector mGestureDetectorCompat;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link CollectionTouchHelper} with a {@link Context}
   *
   * @param context an application context
   */
  CollectionTouchHelper(@NonNull Context context, @NonNull BiConsumer<RecyclerView, Integer> listener)
  {mGestureDetectorCompat = new CollectionGestureDetector(context, mCallback = new Callback(this.listener = listener));}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}


  /** {@inheritDoc} */
  @Override public final boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e)
  {mCallback.recyclerView = rv; return mGestureDetectorCompat.onTouchEvent(e);}

  /** {@inheritDoc} */
  @Override public final void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
  {super.onRequestDisallowInterceptTouchEvent(disallowIntercept);}

  /** Internal callback. */
  private static final class Callback extends CollectionGestureDetector.SimpleOnGestureListener {

    /** Handle near tap. */
    private boolean mHandle = false;

    /** Select listener. */
    private final BiConsumer<RecyclerView, Integer> mListener;

    /** The recycler view widget */
    RecyclerView recyclerView = null;

    /**
     * Constructs a new {@link CollectionTouchHelper.Callback} .
     *
     * @param listener selection listener
     */
    Callback(@NonNull BiConsumer<RecyclerView, Integer> listener)
    {mListener = listener;}

    /** {@inheritDoc} */
    @Override public final boolean onSingleTapUp(@NonNull MotionEvent e) {
      if (!mHandle) return false; mHandle = false;
      final RecyclerView recyclerView = this.recyclerView; this.recyclerView = null;
      final View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
      if (childView == null || !childView.isEnabled()) return false;
      final int position = recyclerView.getChildAdapterPosition(childView);
      if (position == RecyclerView.NO_POSITION) return false;
      childView.playSoundEffect(SoundEffectConstants.CLICK);
      childView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
      mListener.accept(recyclerView, position);
      return true;
    }

    /** {@inheritDoc} */
    @Override public final void onShowPress(@NonNull MotionEvent event)
    {mHandle = true; super.onShowPress(event);}

  }

}

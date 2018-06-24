/*
 * 	RecyclerViewTouchHelper.java
 * 	ommy-ar
 *
 * 	Copyright (C) 2017, Emoji Apps Inc. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of Emoji Apps Incorporated and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to Emoji Apps Incorporated and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from Emoji Apps Incorporated.
 */

package widgets.collections;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
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

  /** The internal callback. */
  private final Callback mCallback;

  /** The gesture detector compat. */
  private final GestureDetectorCompat mGestureDetectorCompat;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link CollectionTouchHelper} with a {@link Context}
   *
   * @param context an application context
   */
  CollectionTouchHelper(@NonNull Context context, @NonNull BiConsumer<RecyclerView, Integer> listener)
  {mGestureDetectorCompat = new GestureDetectorCompat(context, mCallback = new Callback(listener));}

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
  private static final class Callback extends GestureDetector.SimpleOnGestureListener {

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
    @Override public final boolean onSingleTapUp(MotionEvent e) {
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
  }

}

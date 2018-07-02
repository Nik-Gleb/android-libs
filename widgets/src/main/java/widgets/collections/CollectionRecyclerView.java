/*
 * CollectionRecyclerView.java
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.libs.widgets.R;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.StyleableRes;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.io.Closeable;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.support.v7.recyclerview.R.styleable.RecyclerView_android_orientation;
import static android.support.v7.recyclerview.R.styleable.RecyclerView_spanCount;

/**
 * Collection RecyclerView Widget.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/09/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
public class CollectionRecyclerView extends RecyclerView implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "CollectionRecyclerView";

  /** Empty attribute set. */
  private static final AttributeSet EMPTY_ATTRS_SET = null;
  /** The empty style resource. */
  @StyleRes private static final int EMPTY_STYLE = 0;

  /** The default attr resource. */
  @AttrRes private static final int DEFAULT_ATTRS = R.attr.collectionRecyclerView;
  /** The empty style resource. */
  @StyleRes private static final int DEFAULT_STYLE = R.style.CollectionRecyclerView;
  /** Default styleable attributes */
  @StyleableRes private static final int[] DEFAULT_STYLEABLE = R.styleable.CollectionRecyclerView;

  /** The default values. */
  private static final int
      DEFAULT_ORIENTATION = RecyclerView.HORIZONTAL,
      DEFAULT_SIDE = 3,
      DEFAULT_SPAN = 2;

  /** The current values. */
  @SuppressWarnings("FieldCanBeLocal")
  private final int mOrientation, mHorizontal, mVertical;

  /** Measure size. */
  private final boolean mMeasure;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link CollectionRecyclerView} with context.
   *
   * @param context current context
   */
  public CollectionRecyclerView(@NonNull Context context)
  {this(context, EMPTY_ATTRS_SET);}

  /**
   * Constructs a new {@link CollectionRecyclerView} with context and attributes.
   *
   * @param context current context
   * @param attrs   current attributes
   */
  public CollectionRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs)
  {this(context, attrs, DEFAULT_ATTRS);}


  /**
   * Constructs a new {@link CollectionRecyclerView} with context, attributes and
   * default-Style.
   *
   * @param context      current context
   * @param attrs        attributes
   * @param defStyleAttr default Style
   */
  @SuppressLint("PrivateResource")
  public CollectionRecyclerView
  (@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    final Resources.Theme theme = context.getTheme();

    @SuppressLint("CustomViewStyleable")
    final TypedArray recyclerAttrs =
        context.obtainStyledAttributes(attrs,
            android.support.v7.recyclerview.R.styleable.RecyclerView,
            defStyleAttr, EMPTY_STYLE);
    try {
      mOrientation = recyclerAttrs.getInteger
          (RecyclerView_android_orientation, DEFAULT_ORIENTATION);
      final int span = recyclerAttrs.getInteger
          (RecyclerView_spanCount, DEFAULT_SPAN);
      final TypedArray array = theme.obtainStyledAttributes
          (attrs, DEFAULT_STYLEABLE, defStyleAttr, DEFAULT_STYLE);
      try {
        final int side = array.getInteger
            (R.styleable.CollectionRecyclerView_side, DEFAULT_SIDE);
        if (mOrientation == DEFAULT_ORIENTATION)
        {mHorizontal = side; mVertical = span;}
        else {mHorizontal = span; mVertical = side;}
        mMeasure = array.getBoolean(R.styleable.CollectionRecyclerView_measure, false);
      } finally {array.recycle();}
    } finally {recyclerAttrs.recycle();}

    setHasFixedSize(true);
    setItemViewCacheSize(0);
  }


  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override protected final void onMeasure(int widthSpec, int heightSpec) {
    if (!mMeasure){super.onMeasure(widthSpec, heightSpec); return;}

    final int widthMode, heightMode;
    if (mOrientation == RecyclerView.HORIZONTAL) {
      heightSpec = MeasureSpec.getSize(widthSpec) * mVertical / mHorizontal;
      widthMode = MeasureSpec.getMode(widthSpec); heightMode = MeasureSpec.EXACTLY;
    } else {
      widthSpec = MeasureSpec.getSize(heightSpec) * mHorizontal / mVertical;
      heightMode = MeasureSpec.getMode(heightSpec); widthMode = MeasureSpec.EXACTLY;
    }
    super.onMeasure
        (MeasureSpec.makeMeasureSpec(widthSpec, widthMode),
        MeasureSpec.makeMeasureSpec(heightSpec, heightMode));
  }
}

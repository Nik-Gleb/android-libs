/*
 * CollectionItemView.java
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
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.libs.widgets.R;
import android.net.Uri;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.annotation.StyleableRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.Closeable;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;
import widgets.TintableHelper;
import widgets.TintableHelper.Callback;
import widgets.collections.CollectionAdapter.Item;

import static android.view.ViewConfiguration.getTapTimeout;
import static widgets.collections.CollectionGestureDetector.TAP_TIMEOUT;

//import com.bumptech.glide.RequestBuilder;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/09/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
public class CollectionItemView extends View implements Callback,
  Closeable, Consumer<Item>, BooleanSupplier {

  /** The log cat tag. */
  private static final String TAG = "CollectionItemView";

  /** Empty attribute set. */
  private static final AttributeSet EMPTY_ATTRS_SET = null;
  /** The empty style resource. */
  @StyleRes private static final int EMPTY_STYLE = 0;

  /** The default attr resource. */
  @AttrRes private static final int DEFAULT_ATTRS = R.attr.collectionItemView;
  /** The empty style resource. */
  @StyleRes private static final int DEFAULT_STYLE = R.style.CollectionItemView;
  /** Default styleable attributes */
  @StyleableRes private static final int[] DEFAULT_STYLEABLE = R.styleable.CollectionItemView;

  /** This instance. */
  private final CollectionItemView mInstance = this;

  /** The tintable helper. */
  private final TintableHelper mTintableHelper;

  /** Quick tap mode. */
  private boolean mQuickTap = false;

  /* Glide request builder. */
  //@Nullable private RequestBuilder<Bitmap> mGlideRequestBuilder;

  /** Glide drawable factory. */
  @Nullable private BiFunction<View, Bitmap, Drawable> mDrawableFactory;

  /* Glide simple view target. */
  //@Nullable private BitmapSimpleViewTarget mGlideTarget;

  /** Current item value. */
  @NonNull private Item mItem = Item.EMPTY;

  /** Square mode. */
  private boolean mSquare = false;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;


  /**
   * Constructs a new {@link CollectionItemView} with context.
   *
   * @param context current context
   */
  public CollectionItemView(@NonNull Context context)
  {this(context, EMPTY_ATTRS_SET);}

  /**
   * Constructs a new {@link CollectionItemView} with context and attributes.
   *
   * @param context current context
   * @param attrs   current attributes
   */
  public CollectionItemView(@NonNull Context context, @Nullable AttributeSet attrs)
  {this(context, attrs, DEFAULT_ATTRS);}


  /**
   * Constructs a new {@link CollectionItemView} with context, attributes and
   * default-Style.
   *
   * @param context      current context
   * @param attrs        attributes
   * @param defStyleAttr default Style
   */
  public CollectionItemView
  (@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr)
  {this(context, attrs, defStyleAttr, EMPTY_STYLE);}

  /**
   * Constructs a new {@link CollectionItemView} with context, attributes,
   * default-Style and default Resource-Style.
   *
   * @param context      current context
   * @param attrs        attributes
   * @param defStyleAttr default Style
   * @param defStyleRes  default Resource-Style
   */
  public CollectionItemView(@NonNull Context context, @Nullable AttributeSet attrs,
      @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    final Resources.Theme theme = context.getTheme();
    final TypedArray typedArray = theme.obtainStyledAttributes(attrs,
        DEFAULT_STYLEABLE, defStyleAttr, defStyleRes != EMPTY_STYLE ?
            defStyleRes : DEFAULT_STYLE);

    //noinspection EmptyTryBlock
    try {mSquare = typedArray.getBoolean
      (R.styleable.CollectionItemView_square, false);}
    finally {typedArray.recycle();}


    final CollectionItemView me = this;

    setOutlineProvider(new TintableHelper.TintableOutlineProvider(me));

    final Rect padding = new Rect
        (getPaddingLeft(), getPaddingTop(),
            getPaddingRight(), getPaddingBottom());
    mTintableHelper = new TintableHelper(me, padding);

    accept(mItem);
  }


  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Drawable getBackground() {
    return mTintableHelper != null ?
        mTintableHelper.getDrawable() :
        super.getBackground();
  }

  /** {@inheritDoc} */
  @Override
  public final void setBackground(@Nullable Drawable background) {
    final TintableHelper tintableHelper = mTintableHelper;
    if (tintableHelper != null) {
      try {tintableHelper.setDrawable(background);}
      catch (NullPointerException e) {super.setBackground(background);}
    } else super.setBackground(background);
  }

  /** {@inheritDoc} */
  @Override
  protected final void drawableStateChanged() {
    super.drawableStateChanged();
    if (mTintableHelper != null && mTintableHelper.onStateChanged())
      invalidate();
  }

  /** {@inheritDoc} */
  @Override
  public boolean performClick() {
    try {return super.performClick();}
    finally {
      if (mTintableHelper != null)
        mTintableHelper.performClick();
    }
  }

  /** {@inheritDoc} */
  @Override public final boolean postDelayed(@NonNull Runnable action, long delay) {
    if (delay == getTapTimeout()) delay = TAP_TIMEOUT;
    return super.postDelayed(action, delay);
  }

  /** Skip pressed state flag. */
  private boolean mSkipPressed = false;

  @SuppressLint("ClickableViewAccessibility")
  @Override public final boolean onTouchEvent(@NonNull MotionEvent event) {
    mSkipPressed = event.getAction() == MotionEvent.ACTION_MOVE;
    return super.onTouchEvent(event);
  }

  /**{@inheritDoc}*/
  @Override public final void setPressed(boolean pressed)
  {if (mSkipPressed) return; super.setPressed(pressed);}

  /**{@inheritDoc}*/
  @Override public final void drawableHotspotChanged(float x, float y)
  {if (mSkipPressed) return; super.drawableHotspotChanged(x, y);}

  /** @param drawable new drawable for super-call. */
  @Override public final void setSuper(@Nullable Drawable drawable)
  {super.setBackground(drawable);}

  /** @return current drawable by super-call. */
  @Nullable @Override public final Drawable getSuper()
  {return super.getBackground();}

  /** {@inheritDoc} */
  public final void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (mSquare) {
      int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
      setMeasuredDimension(size, size);
    }
  }

  /* @param builder glide request builder */
  /*public final void setGlideRequestBuilder
  (@Nullable RequestBuilder<Bitmap> builder) {
    if (Objects.equals(mGlideRequestBuilder, builder)) return;
    mGlideRequestBuilder = builder;
  }*/

  /** @param factory glide drawable factory */
  public final void setDrawableFactory
      (@Nullable BiFunction<View, Bitmap, Drawable> factory) {
    if (Objects.equals(mDrawableFactory, factory)) return;
    mDrawableFactory = factory; /*mGlideTarget = mDrawableFactory != null ?
        new BitmapSimpleViewTarget(mInstance, mDrawableFactory) : null;*/
  }

  /** @param uri image resource */
  public final void load(@NonNull Uri uri) {
    /*if (mGlideRequestBuilder == null || mGlideTarget == null) return;
    mGlideRequestBuilder.load(uri).into(mGlideTarget);*/
  }

  /** {@inheritDoc} */
  @Override public final boolean getAsBoolean() {return false;}

  /** {@inheritDoc} */
  @Override public final void accept(@Nullable Item item) {
    item = item == null ? Item.EMPTY : item;
    if (Objects.equals(mItem, item)) return;
    /*if (!Objects.equals(mItem.uri, item.uri) && item.uri != null) load(item.uri);*/
    if (!Objects.equals(getBackground(), item.drawable)) setBackground(item.drawable);
    mItem = item;
  }


}

/*
 * SpriteDrawable.java
 * drawables
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

package drawables;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import java.util.Objects;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.graphics.Shader.TileMode.CLAMP;
import static java.lang.Math.round;

/**
 * Sprite Drawable.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/04/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
@Keep@KeepPublicProtectedClassMembers
public final class SpriteDrawable extends BitmapDrawable {

  /** Log cat tag */
  private static final String TAG = "Sprite";

  /** Rounded modes. */
  public static final int SQUARE = -1, CIRCLE = 0;

  /** Draw bounds. */
  private final RectF mDrawRect = new RectF();
  /** Rounded values. */
  private final PointF mRoundPoint = new PointF();
  /** Shader Matrix. */
  private Matrix mMatrix = new Matrix();

  /** Rounded Mode. */
  private final int mRounded;
  /** Matrix Scale. */
  private final float mScale;
  /** Step level. */
  private final int mLevel;

  /**
   * Constructs a new {@link SpriteDrawable}.
   *
   * @param resources platform resources
   * @param bitmap draw bitmap
   * @param rounded rounded factor
   */
  public SpriteDrawable
  (@NonNull Resources resources, @NonNull Bitmap bitmap, int rounded) {
    super(resources, bitmap);
    final DisplayMetrics metrics =
        resources.getDisplayMetrics();
    mRounded = calcRound(metrics, rounded);
    mScale = calcScale(metrics, bitmap);
    mLevel = calcLevel(metrics, bitmap);
    getPaint().setShader
        (new BitmapShader(bitmap, CLAMP, CLAMP));
    setAntiAlias(true); setDither(false);
  }

  /**
   * @param metrics display metrics
   * @param rounded rounded in dp's
   *
   * @return rounded in pixels
   */
  private static int calcRound(@NonNull DisplayMetrics metrics, int rounded)
  {return rounded > CIRCLE ? round(metrics.density * (float)rounded) : rounded;}

  /**
   * @param metrics display metrics
   * @param bitmap source bitmap
   *
   * @return dp-scale
   */
  private static float calcScale
  (@NonNull DisplayMetrics metrics, @NonNull Bitmap bitmap)
  {return (float) metrics.densityDpi / (float) bitmap.getDensity();}

  /**
   * @param metrics display metrics
   * @param bitmap source bitmap
   *
   * @return rounded in pixels
   */
  private static int calcLevel(@NonNull DisplayMetrics metrics, @NonNull Bitmap bitmap)
  {return Math.round(bitmap.getHeight() * calcScale(metrics, bitmap));}

  /** {@inheritDoc} */
  @Override public final int getIntrinsicHeight() {return mLevel;}
  /** {@inheritDoc} */
  @Override public final int getIntrinsicWidth() {return mLevel;}

  /** {@inheritDoc} */
  @Override protected final void onBoundsChange(@NonNull Rect bounds) {
    if (mRounded == CIRCLE) mRoundPoint.set
        (bounds.width() * 0.5f, bounds.height() * 0.5f);
    else if (mRounded == SQUARE) mRoundPoint.set(0, 0);
    else mRoundPoint.set(mRounded, mRounded);
    mDrawRect.set(bounds); invalidate();
  }

  /** {@inheritDoc} */
  @Override protected final boolean onLevelChange(int level)
  {invalidate(); return true;}

  /** Invalidate matrix state. */
  private void invalidate() {
    final Rect bounds = getBounds();
    final int offset = bounds.left - mLevel * getLevel();
    mMatrix.reset(); mMatrix.setScale(mScale, mScale);
    mMatrix.postTranslate(offset, bounds.top);
    getPaint().getShader().setLocalMatrix(mMatrix);
    invalidateSelf();
  }

  /**
   * Draw in its bounds (set via setBounds) respecting optional effects such
   * as alpha (set via setAlpha) and color filter (set via setColorFilter).
   *
   * @param canvas The canvas to draw into
   */
  @Override public final void draw(@NonNull Canvas canvas) {
    canvas.drawRoundRect(mDrawRect, mRoundPoint.x, mRoundPoint.y, getPaint());
  }


  /* @return related context */
  /*@Nullable
  private static Context getContext(@NonNull Drawable drawable) {
    final Callback callback = drawable.getCallback();
    return callback instanceof View ? ((View) callback).getContext() :
        (callback instanceof Drawable) ? getContext((Drawable) callback) : null;
  }*/

  /* @return actual scaling */
  /*@Nullable
  private DisplayMetrics getDisplayMetrics() {
    final Drawable instance = this;
    final Context context = getContext(instance);
    if (context == null) return null;
    final Resources resources = context.getResources();
    if (resources == null) return null;
    return resources.getDisplayMetrics();
  }*/

  /** {@inheritDoc} */
  @Override public final int getOpacity()
  {return mRounded == SQUARE ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SpriteDrawable)) return false;
    final SpriteDrawable that = (SpriteDrawable) obj;
    return Objects.equals(getBitmap(), that.getBitmap())
        && getLevel() == that.getLevel();
  }

  /**
   * Constructs a new {@link SpriteDrawable}
   *
   * @param resources android resources
   * @param bitmap bitmap data
   * @param rounded rounded factor
   * @param level index of icon
   * @param color optionally color-filter
   *
   * @return instance of drawable
   */
  @NonNull public static SpriteDrawable create(@NonNull Resources resources,
      @NonNull Bitmap bitmap, int rounded, int level, @Nullable ColorFilter color) {
    final SpriteDrawable result = new SpriteDrawable(resources, bitmap, rounded);
    result.setLevel(level); result.setColorFilter(color); return result;
  }

}

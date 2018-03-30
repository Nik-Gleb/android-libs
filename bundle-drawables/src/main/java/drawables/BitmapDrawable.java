/*
 * BitmapDrawable.java
 * bundle-drawables
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.util.Log;

import java.io.Closeable;
import java.util.Arrays;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * A Drawable that wraps a bitmap and can be tiled, stretched, or aligned.
 *
 * You can create a BitmapDrawable from a file path, an input stream, through XML inflation, or from
 * a {@link Bitmap} object.
 *
 * <p>It can be defined in an XML file with the <code>&lt;bitmap></code> element.  For more
 * information, see the guide to <a
 * href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.</p>
 * <p>
 *
 * Also see the {@link Bitmap} class, which handles the management and transformation of raw bitmap
 * graphics, and should be used when drawing to a {@link Canvas}. </p>
 */
@Keep@KeepPublicProtectedClassMembers
@SuppressWarnings({"WeakerAccess", "unused"})
@SuppressLint("ObsoleteSdkInt")
public class BitmapDrawable extends Drawable implements Closeable {

    /* Static initialization. */
    static {
        // Attempt to load native library.
        try {System.loadLibrary("reflection");}
        catch (UnsatisfiedLinkError e)
        {Log.w("AlphaMatrixFilter", "Can't load reflection library ", e);}
    }

    /** The default paint flags */
    private static final int DEFAULT_PAINT_FLAGS =
            Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG;

    /** The dst rect (float-based) */
    private final RectF mDstRectF = new RectF();// #updateDstRectAndInsetsIfDirty() sets this
    /** The origin rect (int-based) */
    private final RectF mOriginRectF = new RectF();
    /** Outline rect */
    private final Rect mOutlineRect = new Rect();
    /** The rounded point. */
    private final PointF mRoundedPoint = new PointF(0.0f, 0.0f);
    /** Current size */
    private final Point mSize = new Point();
    /** The smallest size */
    private final Point mSmallestSize = new Point();
    /** The intrinsic size. */
    private final Point mIntrinsicSize = new Point(-1,-1);
    /** The paint */
    private final Paint mPaint = new Paint(DEFAULT_PAINT_FLAGS);

    /** Rounding size */
    private final int mRounded;

    /** The tint filter */
    private PorterDuffColorFilter mTintFilter = null;
    /** The target density */
    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    /** ReCalc coordinates flag */
    private boolean mDstRectAndInsetsDirty = true;

    // These are scaled to match the target density.
    private int mBitmapWidth, mBitmapHeight;

    /** Min of smallest size */
    private int mBitmapMin;

    // Mirroring matrix for using with Shaders
    private Matrix mMirrorMatrix;

    /** Rounded for using with Shaders */
    private Matrix mRoundedMatrix = null;

    /** Drawing bitmap */
    private Bitmap mBitmap = null;
    /** The tint color */
    private ColorStateList mTint = null;
    /** The tint mode */
    private Mode mTintMode = Mode.SRC_IN;

    /** The tile modes */
    private Shader.TileMode mTileModeX = null, mTileModeY = null;

    private boolean mAutoMirrored = false;
    boolean mRebuildShader;

    /** The alpha */
    private int mAlpha = mPaint.getAlpha();

    /** The alpha matrix filter. */
    private final AlphaMatrixFilter mAlphaMatrixFilter;


    /**
     * Constructs a new {@link BitmapDrawable}.
     *
     * @param bitmapDrawable horizontal size
     */
    public BitmapDrawable(@NonNull BitmapDrawable bitmapDrawable) {
        mDstRectF.set(bitmapDrawable.mDstRectF); mOriginRectF.set(bitmapDrawable.mOriginRectF);
        mOutlineRect.set(bitmapDrawable.mOutlineRect); mRoundedPoint.set(bitmapDrawable.mRoundedPoint);
        mSize.set(bitmapDrawable.mSize.x, bitmapDrawable.mSize.y);
        mSmallestSize.set(bitmapDrawable.mSmallestSize.x, bitmapDrawable.mSmallestSize.y);
        mIntrinsicSize.set(bitmapDrawable.mIntrinsicSize.x, bitmapDrawable.mIntrinsicSize.y);
        mPaint.set(bitmapDrawable.mPaint); mRounded = bitmapDrawable.mRounded;
        mTintFilter = bitmapDrawable.mTintFilter; mTargetDensity = bitmapDrawable.mTargetDensity;
        mDstRectAndInsetsDirty = bitmapDrawable.mDstRectAndInsetsDirty;
        mBitmapWidth = bitmapDrawable.mBitmapWidth; mBitmapHeight = bitmapDrawable.mBitmapHeight;
        mBitmapMin = bitmapDrawable.mBitmapMin; mMirrorMatrix = bitmapDrawable.mMirrorMatrix;
        mRoundedMatrix = bitmapDrawable.mRoundedMatrix; mBitmap = bitmapDrawable.mBitmap;
        mTint = bitmapDrawable.mTint; mTintMode = bitmapDrawable.mTintMode;
        mTileModeX = bitmapDrawable.mTileModeX; mTileModeY = bitmapDrawable.mTileModeY;
        mAutoMirrored = bitmapDrawable.mAutoMirrored; mRebuildShader = bitmapDrawable.mRebuildShader;
        mAlphaMatrixFilter = bitmapDrawable.mAlphaMatrixFilter;
    }

    /**
     * Constructs a new {@link BitmapDrawable}.
     *
     * @param width horizontal size
     * @param height vertical size
     * @param round rounding size
     */
    public BitmapDrawable(int width, int height, int round) {
        mAlphaMatrixFilter = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                null : new AlphaMatrixFilter(); updateColorFilter();
        mIntrinsicSize.set(width, height); mRounded = round;
        if (!isRounded()) setTileModeXY(null, null);
        else setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mBitmapWidth = width; mBitmapHeight = height;
    }

    /**
     * Constructs a new {@link BitmapDrawable}.
     *
     * @param bitmap content bitmap
     * @param round rounding size
     */
    public BitmapDrawable(Bitmap bitmap, int round) {
        mAlphaMatrixFilter = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                null : new AlphaMatrixFilter(); updateColorFilter();
        mIntrinsicSize.set(bitmap.getWidth(), bitmap.getHeight());
        mRounded = round; setBitmap(bitmap);
        if (!isRounded()) setTileModeXY(null, null);
        else setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    /**
     * Constructs a new {@link BitmapDrawable}.
     *
     * @param bitmap content bitmap
     * @param round rounding size
     */
    public BitmapDrawable(Bitmap bitmap, int width, int height, int round) {
        mAlphaMatrixFilter = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP ?
                null : new AlphaMatrixFilter(); updateColorFilter();
        mIntrinsicSize.set(width, height);
        mRounded = round; setBitmap(bitmap);
        if (!isRounded()) setTileModeXY(null, null);
        else setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
    }

    /** Recycle the drawable */
    public void close() {
        if (mBitmap == null) return;
        setBitmap(null);
    }

    /** @return true if drawable is recycled */
    protected final boolean isRecycled() {return mBitmap == null || mBitmap.isRecycled();}

    /** The drawing bitmap */
    protected final Bitmap getBitmap() {return mBitmap;}

    /** Update color filter */
    private void updateColorFilter()
    {mPaint.setColorFilter(mAlphaMatrixFilter); invalidateSelf();}

    /** Computing bitmap size */
    private void computeBitmapSize() {
        final Bitmap bitmap = mBitmap;
        if (bitmap != null) {
            mBitmapWidth = bitmap.getScaledWidth(mTargetDensity);
            mBitmapHeight = bitmap.getScaledHeight(mTargetDensity);
            mSmallestSize.set(
                    Math.min(mBitmapWidth, mIntrinsicSize.x),
                    Math.min(mBitmapHeight, mIntrinsicSize.y));
        } else {
            mBitmapWidth = mIntrinsicSize.x;
            mBitmapHeight = mIntrinsicSize.y;
            mSmallestSize.set(mIntrinsicSize.x, mIntrinsicSize.y);
            //mSize.set(mIntrinsicSize.x, mIntrinsicSize.y);
        }
        mBitmapMin = Math.min(mSmallestSize.x, mSmallestSize.y);
    }

    /**
     * @param bitmap new bitmap-content
     * @return true if swapped
     */
    @SuppressWarnings("UnusedReturnValue")
    public final boolean setBitmap(@Nullable Bitmap bitmap) {
        if (mBitmap != bitmap) {

            mDstRectAndInsetsDirty = !(mBitmap != null && bitmap != null &&
                    bitmap.getWidth() == mBitmap.getWidth() &&
                            bitmap.getHeight() == mBitmap.getHeight());

            mBitmap = bitmap;

            if (bitmap != null) {
                bitmap.setDensity(mTargetDensity);
                mRebuildShader = true;
            }

            computeBitmapSize();
            invalidateSelf();
            return true;
        } else return false;
    }

    /**
     * Set the density scale at which this drawable will be rendered. This
     * method assumes the drawable will be rendered at the same density as the
     * specified canvas.
     *
     * @param canvas The Canvas from which the density scale must be obtained.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public final void setTargetDensity(Canvas canvas)
    {setTargetDensity(canvas.getDensity());}

    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public final void setTargetDensity(DisplayMetrics metrics)
    {setTargetDensity(metrics.densityDpi);}

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see Bitmap#setDensity(int)
     * @see Bitmap#getDensity()
     */
    public final void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mBitmap != null) computeBitmapSize();
            invalidateSelf();
        }
    }

    /**
     * Enables or disables the mipmap hint for this drawable's bitmap.
     * See {@link Bitmap#setHasMipMap(boolean)} for more information.
     *
     * If the bitmap is null calling this method has no effect.
     *
     * @param mipMap True if the bitmap should use mipmaps, false otherwise.
     *
     * @see #hasMipMap()
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final void setMipMap(boolean mipMap) {
        if (mBitmap == null) return;
        mBitmap.setHasMipMap(mipMap);
        invalidateSelf();
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's bitmap.
     *
     * @return True if the mipmap hint is set, false otherwise. If the bitmap
     *         is null, this method always returns false.
     *
     * @see #setMipMap(boolean)
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public final boolean hasMipMap()
    {return mBitmap != null && mBitmap.hasMipMap();}

    /**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     *
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     *
     * @see #hasAntiAlias()
     */
    public final void setAntiAlias(boolean aa)
    {mPaint.setAntiAlias(aa); invalidateSelf();}

    /**
     * Indicates whether anti-aliasing is enabled for this drawable.
     *
     * @return True if anti-aliasing is enabled, false otherwise.
     *
     * @see #setAntiAlias(boolean)
     */
    public final boolean hasAntiAlias()
    {return mPaint.isAntiAlias();}

    /** {@inheritDoc} */
    @Override public final void setFilterBitmap(boolean filter)
    {mPaint.setFilterBitmap(filter); invalidateSelf();}

    /** {@inheritDoc} */
    @Override public final boolean isFilterBitmap()
    {return mPaint.isFilterBitmap();}

    /** {@inheritDoc} */
    @Override@SuppressWarnings("deprecation") public final void setDither(boolean dither)
    {mPaint.setDither(dither); invalidateSelf();}

    /**
     * Sets the repeat behavior of this drawable on both axis. By default, the drawable
     * does not repeat its bitmap. Using {@link Shader.TileMode#REPEAT} or
     * {@link Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled)
     * if the bitmap is smaller than this drawable.
     *
     * @param xmode The X repeat mode for this drawable.
     * @param ymode The Y repeat mode for this drawable.
     */
    private void setTileModeXY(Shader.TileMode xmode, Shader.TileMode ymode) {
        if (mTileModeX != xmode || mTileModeY != ymode) {
            mTileModeX = xmode;
            mTileModeY = ymode;
            mRebuildShader = true;
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    /** {@inheritDoc} */
    @Override public final void setAutoMirrored(boolean mirrored)
    {if (mAutoMirrored == mirrored) return; mAutoMirrored = mirrored; invalidateSelf();}

    /** {@inheritDoc} */
    @Override public final boolean isAutoMirrored()
    {return mAutoMirrored;}

    /** @return rounded */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public final boolean isRounded()
    {return mRounded != -1;}

    /** @return true when need mirroring */
    private boolean needMirroring() {
        //noinspection WrongConstant
        return isAutoMirrored() && DrawableCompat.getLayoutDirection(this) == LayoutDirection.RTL;
    }

    /** @param dx the offset */
    private void updateMirrorMatrix(float dx) {
        if (mMirrorMatrix == null) mMirrorMatrix = new Matrix();
        mMirrorMatrix.setTranslate(dx, 0);
        mMirrorMatrix.preScale(-1.0f, 1.0f);
    }

    /** Update rounded matrix. */
    private void updateRoundedMatrix(float x, float y, int width, int height) {
        mRoundedMatrix = new Matrix();
        mRoundedMatrix.setTranslate(x, y);
        mRoundedMatrix.preScale(mDstRectF.width() / width, mDstRectF.height() / height);
        if (mMirrorMatrix != null) mRoundedMatrix.preConcat(mMirrorMatrix);
    }

    /** {@inheritDoc} */
    @Override public void onBoundsChange(Rect bounds)
    {mDstRectAndInsetsDirty = true; mSize.set(bounds.width(), bounds.height());}

    /** {@inheritDoc} */
    @Override public void draw(@NonNull Canvas canvas) {

        final Bitmap bitmap = mBitmap;
        if (bitmap == null) return;

        final Paint paint = mPaint;

        if (mRebuildShader) {
            final Shader.TileMode tmx = mTileModeX;
            final Shader.TileMode tmy = mTileModeY;
            if (tmx == null && tmy == null)
                paint.setShader(null);
            else
                paint.setShader(new BitmapShader(bitmap,
                        tmx == null ? Shader.TileMode.CLAMP : tmx,
                        tmy == null ? Shader.TileMode.CLAMP : tmy));


            mRebuildShader = false;
        }


        updateDstRectAndInsetsIfDirty();

        /*canvas.save();
        canvas.saveLayer(mDstRectF, paint, Canvas.CLIP_TO_LAYER_SAVE_FLAG);*/

        //if (!bitmap.isRecycled()) {
            if (mRounded == -1)
                canvas.drawBitmap(bitmap, mDstRectF.left, mDstRectF.top, paint);
            else
                canvas.drawRoundRect(mDstRectF, mRoundedPoint.x, mRoundedPoint.y, paint);
        //}

        //canvas.restore();



    }

    /** Calc coordinates */
    private void updateDstRectAndInsetsIfDirty() {
        if (!mDstRectAndInsetsDirty) return;

        if (mRounded != -1) {
            if (mRounded == 0)
                calcDestRect(mBitmapMin, mBitmapMin, mDstRectF);
            else calcDestRect(mSmallestSize.x, mSmallestSize.y, mDstRectF);
            calcDestRect(mBitmapWidth, mBitmapHeight, mOriginRectF);
        } else calcDestRect(mBitmapWidth, mBitmapHeight, mDstRectF);

        if (mRounded == -1) return;

        final Shader bitmapShader = mPaint.getShader();
        if (bitmapShader != null) {
                float rounded = mRounded;
                if (mRounded == 0) {rounded = mDstRectF.width() * 0.5f;
                    updateRoundedMatrix(mOriginRectF.left, mOriginRectF.top, mBitmapMin, mBitmapMin);
                } else updateRoundedMatrix(mDstRectF.left, mDstRectF.top, mSmallestSize.x, mSmallestSize.y);
                bitmapShader.setLocalMatrix(mRoundedMatrix);
                mRoundedPoint.set(rounded, rounded);
            mPaint.setShader(bitmapShader);
        }

        mDstRectAndInsetsDirty = false;
    }

    /**
     * @param width source width
     * @param height source height
     * @param dest destination
     */
    private void calcDestRect(int width, int height, @NonNull RectF dest) {
        dest.left = (mSize.x - width) >> 1; dest.right = mSize.x - dest.left;
        dest.top = (mSize.y - height) >> 1; dest.bottom = mSize.y - dest.top;
    }


    /** {@inheritDoc} */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override public void getOutline(@NonNull Outline outline) {
        //updateDstRectAndInsetsIfDirty();

        /*mDstRectF.round(mOutlineRect);
        if (isRounded()) outline.setRoundRect(mOutlineRect, Math.max(mRounded.x, mRounded.y));
        else outline.setRect(mOutlineRect);*/

        // Only opaque Bitmaps can report a non-0 alpha,
        // since only they are guaranteed to fill their bounds
        boolean opaqueOverShape = mBitmap != null
                && !mBitmap.hasAlpha();
        outline.setAlpha(opaqueOverShape ? getAlpha() / 255.0f : 0.0f);
    }

    /** {@inheritDoc} */
    @Override public void setAlpha(int alpha) {
        if (alpha == mAlpha) return;

        if (mAlphaMatrixFilter != null) {
            mAlphaMatrixFilter.setAlpha(alpha / 255.0f);
            updateColorFilter();
        } else {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }

        mAlpha = alpha;
    }

    /** {@inheritDoc} */
    @Override public int getAlpha() {return mAlpha;}

    /** {@inheritDoc} */
    @Override public final void setColorFilter(ColorFilter colorFilter)
    {/*mPaint.setColorFilter(colorFilter); invalidateSelf();*/}

    /** {@inheritDoc} */
    @Override public final ColorFilter getColorFilter()
    {return mPaint.getColorFilter();}

    /** {@inheritDoc} */
    @Override public final void setTintList(ColorStateList tint)
    { mTint = tint; mTintFilter = updateTintFilter(mTintFilter, tint, mTintMode); invalidateSelf();}

    /** {@inheritDoc} */
    @Override public final void setTintMode(@NonNull Mode tintMode)
    {mTintMode = tintMode; mTintFilter = updateTintFilter(mTintFilter, mTint, tintMode); invalidateSelf();}

    /** {@inheritDoc} */
    @Override protected final boolean onStateChange(int[] stateSet) {
        if (mTint != null && mTintMode != null) {
            mTintFilter = updateTintFilter(mTintFilter, mTint, mTintMode);
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override public boolean isStateful() {return (mTint != null && mTint.isStateful()) || super.isStateful();}

    /** {@inheritDoc} */
    @Override public final int getIntrinsicWidth() {return mBitmapWidth;}

    /** {@inheritDoc} */
    @Override public final int getIntrinsicHeight() {return mBitmapHeight;}

    /** {@inheritDoc} */
    @Override public final int getOpacity()
    {return (mBitmap == null || mBitmap.hasAlpha() || mPaint.getAlpha() < 255) ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;}


    /** Ensures the tint filter is consistent with the current tint color and mode. */
    PorterDuffColorFilter updateTintFilter(PorterDuffColorFilter tintFilter,
                                           ColorStateList tint, Mode tintMode) {
        if (tint == null || tintMode == null) return null;
        final int color = tint.getColorForState(getState(), Color.TRANSPARENT);
        if (tintFilter == null)
            return new PorterDuffColorFilter(color, tintMode);

        /*tintFilter.setColor(color);
        tintFilter.setMode(tintMode);*/
        return tintFilter;
    }

    /** Update ColorMatrixColorFilter. */
    @SuppressWarnings("JniMissingFunction")
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private static native boolean update(@NonNull ColorMatrixColorFilter filter);

    /** @return color-matrix. */
    @SuppressWarnings("JniMissingFunction")
    @NonNull private static native ColorMatrix getMatrix(@NonNull ColorMatrixColorFilter filter);

    /**
     * @author Nikitenko Gleb
     * @since 1.0, 08/10/2016
     */
    @Keep@KeepPublicProtectedClassMembers
    @SuppressWarnings("WeakerAccess, unused")
    private static final class AlphaMatrixFilter extends ColorMatrixColorFilter {

        /** Color matrix filter. */
        private static final String TAG = "AlphaMatrixFilter";

        /** Color matrix. */
        @SuppressWarnings("CanBeFinal") private float[] mMatrix;

        /** Create a color filter that transforms colors through a 4x5 color matrix. */
        public AlphaMatrixFilter() {
            super(createEmpty());
            mMatrix = getMatrix(this).getArray();
        }

        /** @param alpha alpha-fraction */
        public final void setAlpha(float alpha) {
            if (mMatrix == null) return;

            // There are 3 phases so we multiply fraction by that amount
            final float phase = alpha * 3;

            mMatrix[18] = Math.min(phase, 2f) / 2f;

            // We substract to make the picture look darker, it will automatically clamp
            // This is spread over period [0, 2.5]
            final int MaxBlacker = 100;
            final float blackening = (float)Math.round((1 - Math.min(phase, 2.5f) / 2.5f) * MaxBlacker);
            mMatrix[4] = mMatrix[9] = mMatrix[14] = 1 - alpha;//-blackening;

            // Finally we desaturate over [0, 3], taken from ColorMatrix.SetSaturation
            final float invSat = 1 - Math.max(0.2f, alpha),
                    R = 0.213f * invSat, G = 0.715f * invSat, B = 0.072f * invSat;

            mMatrix[0] = R + alpha; mMatrix[1] = G;         mMatrix[2] = B;
            mMatrix[5] = R;         mMatrix[6] = G + alpha; mMatrix[7] = B;
            mMatrix[10] = R;        mMatrix[11] = G;        mMatrix[12] = B + alpha;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final boolean b = update(this);
            }
        }

        /**
         * Set this colorMatrix to identity:
         * <pre>
         * [ 1 0 0 0 0   - red vector
         *   0 1 0 0 0   - green vector
         *   0 0 1 0 0   - blue vector
         *   0 0 0 1 0 ] - alpha vector
         * </pre>
         *
         * @return empty matrix
         *
         **/
        private static float[] createEmpty() {
            final float[] result = new float[20]; Arrays.fill(result, 0);
            result[0] = result[6] = result[12] = result[18] = 1;
            return result;
        }
    }

}
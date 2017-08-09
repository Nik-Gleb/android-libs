package drawables;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.view.Gravity;
import android.view.View;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * A Drawable that manages an array of other Drawables. These are drawn in array
 * order, so the element with the largest index will be drawn on top.
 * <p>
 * It can be defined in an XML file with the <code>&lt;layer-list></code> element.
 * Each Drawable in the layer is defined in a nested <code>&lt;item></code>.
 * <p>
 * For more information, see the guide to
 * <a href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 17/05/2017
 */
@Keep@KeepPublicProtectedClassMembers
@SuppressWarnings({"WeakerAccess", "unused"})
public class LayerDrawable extends Drawable implements Drawable.Callback {

    /**
     * Padding mode used to nest each layer inside the padding of the previous layer.
     *
     * @see #setPaddingMode(int)
     */
    static final int PADDING_MODE_NEST = 0;

    /**
     * Value used for undefined start and end insets.
     *
     * @see #getLayerInsetStart(int)
     * @see #getLayerInsetEnd(int)
     */
    static final int INSET_UNDEFINED = Integer.MIN_VALUE;

    /**
     * Padding mode used to stack each layer directly atop the previous layer.
     *
     * @see #setPaddingMode(int)
     */
    private static final int PADDING_MODE_STACK = 1;

    /** Temp Rect's */
    private final Rect
            mTmpRect = new Rect(),
            mTmpOutRect = new Rect(),
            mTmpContainer = new Rect();

    /** The layer state */
    private LayerState mLayerState;

    /** The padding. */
    private int[] mPaddingL, mPaddingT, mPaddingR, mPaddingB;

    /** The hot-spot */
    private Rect mHotspotBounds;

    /** The boolean flags. */
    private boolean mMutated, mSuspendChildInvalidation, mChildRequestedInvalidation;


    /**
     * Creates a new layer drawable with the list of specified layers.
     *
     * @param layers a list of drawables to use as layers in this new drawable,
     *               must be non-null
     */
    public LayerDrawable(@NonNull Drawable[] layers) {
        this(layers, null);
    }

    /**
     * Creates a new layer drawable with the specified list of layers and the
     * specified constant state.
     *
     * @param layers The list of layers to add to this drawable.
     * @param state  The constant drawable state.
     */
    public LayerDrawable(@NonNull Drawable[] layers, @Nullable LayerState state) {
        this(state, null);

        final int length = layers.length;
        final ChildDrawable[] childDrawables = new ChildDrawable[length];
        for (int i = 0; i < length; i++) {
            layers[i].setCallback(this);
            final ChildDrawable childDrawable = new ChildDrawable(mLayerState.mDensity);
            childDrawable.mDrawable = layers[i];
            childDrawables[i] = childDrawable;
            mLayerState.mChildrenChangingConfigurations |= layers[i].getChangingConfigurations();
        }
        mLayerState.mNum = length;
        mLayerState.mChildren = childDrawables;

        ensurePadding();
        refreshPadding();
    }

    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    private LayerDrawable(@Nullable LayerState state, @Nullable Resources res) {
        mLayerState = createConstantState(state, res);
        if (mLayerState.mNum > 0) {
            ensurePadding();
            refreshPadding();
        }
    }

    /**
     * Resolves layer gravity given explicit gravity and dimensions.
     * <p>
     * If the client hasn't specified a gravity but has specified an explicit
     * dimension, defaults to START or TOP. Otherwise, defaults to FILL to
     * preserve legacy behavior.
     *
     * @param gravity layer gravity
     * @param width   width of the layer if set, -1 otherwise
     * @param height  height of the layer if set, -1 otherwise
     * @return the default gravity for the layer
     */
    private static int resolveGravity(int gravity, int width, int height,
            int intrinsicWidth, int intrinsicHeight) {
        if (!Gravity.isHorizontal(gravity)) {
            if (width < 0) {
                gravity |= Gravity.FILL_HORIZONTAL;
            } else {
                gravity |= Gravity.START;
            }
        }

        if (!Gravity.isVertical(gravity)) {
            if (height < 0) {
                gravity |= Gravity.FILL_VERTICAL;
            } else {
                gravity |= Gravity.TOP;
            }
        }

        // If a dimension if not specified, either implicitly or explicitly,
        // force FILL for that dimension's gravity. This ensures that colors
        // are handled correctly and ensures backward compatibility.
        if (width < 0 && intrinsicWidth < 0) {
            gravity |= Gravity.FILL_HORIZONTAL;
        }

        if (height < 0 && intrinsicHeight < 0) {
            gravity |= Gravity.FILL_VERTICAL;
        }

        return gravity;
    }

    /**
     * @param resources     the resources manager
     * @param parentDensity the density of parent
     * @return resolved value
     */
    public static int resolveDensity(@Nullable Resources resources, int parentDensity) {
        final int densityDpi = resources == null ?
                parentDensity : resources.getDisplayMetrics().densityDpi;
        return densityDpi == 0 ? DisplayMetrics.DENSITY_DEFAULT : densityDpi;
    }

    /**
     * Scales a floating-point pixel value from the source density to the
     * target density.
     *
     * @param pixels        the pixel value for use in source density
     * @param sourceDensity the source density
     * @param targetDensity the target density
     * @return the scaled pixel value for use in target density
     */
    private static float scaleFromDensity(float pixels, int sourceDensity, int targetDensity) {
        return pixels * targetDensity / sourceDensity;
    }

    /**
     * Scales a pixel value from the source density to the target density,
     * optionally handling the resulting pixel value as a size rather than an
     * offset.
     * <p>
     * A size conversion involves rounding the base value and ensuring that
     * a non-zero base value is at least one pixel in size.
     * <p>
     * An offset conversion involves simply truncating the base value to an
     * integer.
     *
     * @param pixels        the pixel value for use in source density
     * @param sourceDensity the source density
     * @param targetDensity the target density
     * @param isSize        {@code true} to handle the resulting scaled value as a
     *                      size, or {@code false} to handle it as an offset
     * @return the scaled pixel value for use in target density
     */
    static int scaleFromDensity(
            int pixels, int sourceDensity, int targetDensity, boolean isSize) {
        if (pixels == 0 || sourceDensity == targetDensity) {
            return pixels;
        }

        final float result = pixels * targetDensity / (float) sourceDensity;
        if (!isSize) {
            return (int) result;
        }

        final int rounded = Math.round(result);
        if (rounded != 0) {
            return rounded;
        } else if (pixels > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Creates constant state.
     *
     * @param state the state
     * @param res   the resources
     * @return new state
     */
    @NonNull
    private LayerState createConstantState(@Nullable LayerState state, @Nullable Resources res) {
        return new LayerState(state, this, res);
    }

    /** {@inheritDoc} */
    @Override
    public final void applyTheme(@NonNull Theme t) {
        super.applyTheme(t);

        final LayerState state = mLayerState;
        if (state == null) {
            return;
        }

        int density = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            density = resolveDensity(t.getResources(), 0);
            state.setDensity(density);
        }

        final ChildDrawable[] array = state.mChildren;
        final int N = state.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable layer = array[i];
            if (density != 0) {
                layer.setDensity(density);
            }

            final Drawable d = layer.mDrawable;
            if (d != null && DrawableCompat.canApplyTheme(d)) {
                DrawableCompat.applyTheme(d, t);

                // Update cached mask of child changing configurations.
                state.mChildrenChangingConfigurations |= d.getChangingConfigurations();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canApplyTheme() {
        return (mLayerState != null && mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    /**
     * Adds a new layer at the end of list of layers and returns its index.
     *
     * @param layer The layer to add.
     * @return The index of the layer.
     */
    private int addLayer(@NonNull ChildDrawable layer) {
        final LayerState st = mLayerState;
        final int N = st.mChildren != null ? st.mChildren.length : 0;
        final int i = st.mNum;
        assert st.mChildren != null;
        if (i >= N) {
            final ChildDrawable[] nu = new ChildDrawable[N + 10];
            if (i > 0) {
                System.arraycopy(st.mChildren, 0, nu, 0, i);
            }

            st.mChildren = nu;
        }

        st.mChildren[i] = layer;
        st.mNum++;
        st.invalidateCache();
        return i;
    }

    /**
     * Add a new layer to this drawable. The new layer is identified by an id.
     *
     * @param dr         The drawable to add as a layer.
     * @param themeAttrs Theme attributes extracted from the layer.
     * @param id         The id of the new layer.
     * @param left       The left padding of the new layer.
     * @param top        The top padding of the new layer.
     * @param right      The right padding of the new layer.
     * @param bottom     The bottom padding of the new layer.
     */
    @NonNull
    private ChildDrawable addLayer(Drawable dr, int[] themeAttrs, int id,
            int left, int top, int right, int bottom) {
        final ChildDrawable childDrawable = createLayer(dr);
        childDrawable.mId = id;
        childDrawable.mThemeAttrs = themeAttrs;
        childDrawable.mDrawable.setAutoMirrored(isAutoMirrored());
        childDrawable.mInsetL = left;
        childDrawable.mInsetT = top;
        childDrawable.mInsetR = right;
        childDrawable.mInsetB = bottom;

        addLayer(childDrawable);

        mLayerState.mChildrenChangingConfigurations |= dr.getChangingConfigurations();
        dr.setCallback(this);

        return childDrawable;
    }

    /**
     * @param dr drawable for new layer
     * @return child drawable
     */
    @NonNull
    private ChildDrawable createLayer(@NonNull Drawable dr) {
        final ChildDrawable layer = new ChildDrawable(mLayerState.mDensity);
        layer.mDrawable = dr;
        return layer;
    }

    /**
     * Adds a new layer containing the specified {@code drawable} to the end of
     * the layer list and returns its index.
     *
     * @param dr The drawable to add as a new layer.
     * @return The index of the new layer.
     */
    public final int addLayer(@NonNull Drawable dr) {
        final ChildDrawable layer = createLayer(dr);
        final int index = addLayer(layer);
        ensurePadding();
        refreshChildPadding(index, layer);
        return index;
    }

    /**
     * Looks for a layer with the given ID and returns its {@link Drawable}.
     * <p>
     * If multiple layers are found for the given ID, returns the
     * {@link Drawable} for the matching layer at the highest index.
     *
     * @param id The layer ID to search for.
     * @return The {@link Drawable} for the highest-indexed layer that has the
     * given ID, or null if not found.
     */
    public final Drawable findDrawableByLayerId(int id) {
        final ChildDrawable[] layers = mLayerState.mChildren;
        for (int i = mLayerState.mNum - 1; i >= 0; i--) {
            if (layers[i].mId == id) {
                return layers[i].mDrawable;
            }
        }

        return null;
    }

    /**
     * Sets the ID of a layer.
     *
     * @param index The index of the layer to modify, must be in the range
     *              {@code 0...getNumberOfLayers()-1}.
     * @param id    The id to assign to the layer.
     * @see #getId(int)
     */
    private void setId(int index, int id) {
        mLayerState.mChildren[index].mId = id;
    }

    /**
     * Returns the ID of the specified layer.
     *
     * @param index The index of the layer, must be in the range
     *              {@code 0...getNumberOfLayers()-1}.
     * @return The id of the layer or {@link View#NO_ID} if the
     * layer has no id.
     * @see #setId(int, int)
     */
    private int getId(int index) {
        if (index >= mLayerState.mNum) {
            throw new IndexOutOfBoundsException();
        }
        return mLayerState.mChildren[index].mId;
    }

    /**
     * Returns the number of layers contained within this layer drawable.
     *
     * @return The number of layers.
     */
    public final int getNumberOfLayers() {
        return mLayerState.mNum;
    }

    /**
     * Replaces the {@link Drawable} for the layer with the given id.
     *
     * @param id       The layer ID to search for.
     * @param drawable The replacement {@link Drawable}.
     * @return Whether the {@link Drawable} was replaced (could return false if
     * the id was not found).
     */
    public final boolean setDrawableByLayerId(int id, Drawable drawable) {
        final int index = findIndexByLayerId(id);
        if (index < 0) {
            return false;
        }

        setDrawable(index, drawable);
        return true;
    }

    /**
     * Returns the layer with the specified {@code id}.
     * <p>
     * If multiple layers have the same ID, returns the layer with the lowest
     * index.
     *
     * @param id The ID of the layer to return.
     * @return The index of the layer with the specified ID.
     */
    private int findIndexByLayerId(int id) {
        final ChildDrawable[] layers = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable childDrawable = layers[i];
            if (childDrawable.mId == id) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Sets the drawable for the layer at the specified index.
     *
     * @param index    The index of the layer to modify, must be in the range
     *                 {@code 0...getNumberOfLayers()-1}.
     * @param drawable The drawable to set for the layer.
     * @see #getDrawable(int)
     */
    private void setDrawable(int index, Drawable drawable) {
        if (index >= mLayerState.mNum) {
            throw new IndexOutOfBoundsException();
        }

        final ChildDrawable[] layers = mLayerState.mChildren;
        final ChildDrawable childDrawable = layers[index];
        if (childDrawable.mDrawable != null) {
            if (drawable != null) {
                final Rect bounds = childDrawable.mDrawable.getBounds();
                drawable.setBounds(bounds);
            }

            childDrawable.mDrawable.setCallback(null);
        }

        if (drawable != null) {
            drawable.setCallback(this);
        }

        childDrawable.mDrawable = drawable;
        mLayerState.invalidateCache();

        refreshChildPadding(index, childDrawable);
    }

    /**
     * Returns the drawable for the layer at the specified index.
     *
     * @param index The index of the layer, must be in the range
     *              {@code 0...getNumberOfLayers()-1}.
     * @return The {@link Drawable} at the specified layer index.
     * @see #setDrawable(int, Drawable)
     */
    @NonNull
    private Drawable getDrawable(int index) {
        if (index >= mLayerState.mNum) {
            throw new IndexOutOfBoundsException();
        }
        return mLayerState.mChildren[index].mDrawable;
    }

    /**
     * Sets an explicit size for the specified layer.
     * <p>
     * <strong>Note:</strong> Setting an explicit layer size changes the
     * default layer gravity behavior. See {@link #setLayerGravity(int, int)}
     * for more information.
     *
     * @param index the index of the layer to adjust
     * @param w     width in pixels, or -1 to use the intrinsic width
     * @param h     height in pixels, or -1 to use the intrinsic height
     * @see #getLayerWidth(int)
     * @see #getLayerHeight(int)
     */
    private void setLayerSize(int index, int w, int h) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mWidth = w;
        childDrawable.mHeight = h;
    }

    /**
     * @param index the index of the layer to adjust
     * @param w     width in pixels, or -1 to use the intrinsic width
     */
    public final void setLayerWidth(int index, int w) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mWidth = w;
    }

    /**
     * @param index the index of the drawable to adjust
     * @return the explicit width of the layer, or -1 if not specified
     * @see #setLayerSize(int, int, int)
     */
    private int getLayerWidth(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mWidth;
    }

    /**
     * @param index the index of the layer to adjust
     * @param h     height in pixels, or -1 to use the intrinsic height
     */
    public final void setLayerHeight(int index, int h) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mHeight = h;
    }

    /**
     * @param index the index of the drawable to adjust
     * @return the explicit height of the layer, or -1 if not specified
     * @see #setLayerSize(int, int, int)
     */
    private int getLayerHeight(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mHeight;
    }

    /**
     * Sets the gravity used to position or stretch the specified layer within
     * its container. Gravity is applied after any layer insets (see
     * {@link #setLayerInset(int, int, int, int, int)}) or padding (see
     * {@link #setPaddingMode(int)}).
     * <p>
     * If gravity is specified as {@link Gravity#NO_GRAVITY}, the default
     * behavior depends on whether an explicit width or height has been set
     * (see {@link #setLayerSize(int, int, int)}), If a dimension is not set,
     * gravity in that direction defaults to {@link Gravity#FILL_HORIZONTAL} or
     * {@link Gravity#FILL_VERTICAL}; otherwise, gravity in that direction
     * defaults to {@link Gravity#LEFT} or {@link Gravity#TOP}.
     *
     * @param index   the index of the drawable to adjust
     * @param gravity the gravity to set for the layer
     * @see #getLayerGravity(int)
     */
    protected void setLayerGravity(int index, int gravity) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mGravity = gravity;
    }

    /**
     * @param index the index of the layer
     * @return the gravity used to position or stretch the specified layer
     * within its container
     * @see #setLayerGravity(int, int)
     */
    private int getLayerGravity(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mGravity;
    }

    /**
     * Specifies the insets in pixels for the drawable at the specified index.
     *
     * @param index the index of the drawable to adjust
     * @param l     number of pixels to add to the left bound
     * @param t     number of pixels to add to the top bound
     * @param r     number of pixels to subtract from the right bound
     * @param b     number of pixels to subtract from the bottom bound
     */
    private void setLayerInset(int index, int l, int t, int r, int b) {
        setLayerInsetInternal(index, l, t, r, b, INSET_UNDEFINED, INSET_UNDEFINED);
    }

    /**
     * Specifies the relative insets in pixels for the drawable at the
     * specified index.
     *
     * @param index the index of the layer to adjust
     * @param s     number of pixels to inset from the start bound
     * @param t     number of pixels to inset from the top bound
     * @param e     number of pixels to inset from the end bound
     * @param b     number of pixels to inset from the bottom bound
     */
    public final void setLayerInsetRelative(int index, int s, int t, int e, int b) {
        setLayerInsetInternal(index, 0, t, 0, b, s, e);
    }

    /**
     * @param index the index of the layer to adjust
     * @param l     number of pixels to inset from the left bound
     */
    public final void setLayerInsetLeft(int index, int l) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetL = l;
    }

    /**
     * @param index the index of the layer
     * @return number of pixels to inset from the left bound
     */
    public final int getLayerInsetLeft(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetL;
    }

    /**
     * @param index the index of the layer to adjust
     * @param r     number of pixels to inset from the right bound
     */
    public final void setLayerInsetRight(int index, int r) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetR = r;
    }

    /**
     * @param index the index of the layer
     * @return number of pixels to inset from the right bound
     */
    public final int getLayerInsetRight(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetR;
    }

    /**
     * @param index the index of the layer to adjust
     * @param t     number of pixels to inset from the top bound
     */
    public final void setLayerInsetTop(int index, int t) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetT = t;
    }

    /**
     * @param index the index of the layer
     * @return number of pixels to inset from the top bound
     */
    public final int getLayerInsetTop(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetT;
    }

    /**
     * @param index the index of the layer to adjust
     * @param b     number of pixels to inset from the bottom bound
     */
    public final void setLayerInsetBottom(int index, int b) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetB = b;
    }

    /**
     * @param index the index of the layer
     * @return number of pixels to inset from the bottom bound
     */
    public final int getLayerInsetBottom(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetB;
    }

    /**
     * @param index the index of the layer to adjust
     * @param s     number of pixels to inset from the start bound
     */
    public final void setLayerInsetStart(int index, int s) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetS = s;
    }

    /**
     * @param index the index of the layer
     * @return the number of pixels to inset from the start bound, or
     * {@link #INSET_UNDEFINED} if not specified
     */
    private int getLayerInsetStart(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetS;
    }

    /**
     * @param index the index of the layer to adjust
     * @param e     number of pixels to inset from the end bound, or
     *              {@link #INSET_UNDEFINED} if not specified
     */
    public final void setLayerInsetEnd(int index, int e) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetE = e;
    }

    /**
     * @param index the index of the layer
     * @return number of pixels to inset from the end bound
     */
    private int getLayerInsetEnd(int index) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        return childDrawable.mInsetE;
    }

    /**
     * @param index the index of layer
     * @param l     left position
     * @param t     top position
     * @param r     right position
     * @param b     bottom position
     * @param s     s-position
     * @param e     e-position
     */
    private void setLayerInsetInternal(int index, int l, int t, int r, int b, int s, int e) {
        final ChildDrawable childDrawable = mLayerState.mChildren[index];
        childDrawable.mInsetL = l;
        childDrawable.mInsetT = t;
        childDrawable.mInsetR = r;
        childDrawable.mInsetB = b;
        childDrawable.mInsetS = s;
        childDrawable.mInsetE = e;
    }

    /**
     * @return the current padding mode
     * @see #setPaddingMode(int)
     */
    private int getPaddingMode() {
        return mLayerState.mPaddingMode;
    }

    /**
     * Specifies how layer padding should affect the bounds of subsequent
     * layers. The default value is {@link #PADDING_MODE_NEST}.
     *
     * @param mode padding mode, one of:
     *             <ul>
     *             <li>{@link #PADDING_MODE_NEST} to nest each layer inside the
     *             padding of the previous layer
     *             <li>{@link #PADDING_MODE_STACK} to stack each layer directly
     *             atop the previous layer
     *             </ul>
     * @see #getPaddingMode()
     */
    private void setPaddingMode(int mode) {
        if (mLayerState.mPaddingMode != mode) {
            mLayerState.mPaddingMode = mode;
        }
    }

    /**
     * Temporarily suspends child invalidation.
     *
     * @see #resumeChildInvalidation()
     */
    private void suspendChildInvalidation() {
        mSuspendChildInvalidation = true;
    }

    /**
     * Resumes child invalidation after suspension, immediately performing an
     * invalidation if one was requested by a child during suspension.
     *
     * @see #suspendChildInvalidation()
     */
    private void resumeChildInvalidation() {
        mSuspendChildInvalidation = false;

        if (mChildRequestedInvalidation) {
            mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void invalidateDrawable(@NonNull Drawable who) {
        if (mSuspendChildInvalidation) {
            mChildRequestedInvalidation = true;
        } else {
            invalidateSelf();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
        scheduleSelf(what, when);
    }

    /** {@inheritDoc} */
    @Override
    public final void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
        unscheduleSelf(what);
    }

    /** {@inheritDoc} */
    @Override
    public final void draw(@NonNull Canvas canvas) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.draw(canvas);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int getChangingConfigurations() {
        return super.getChangingConfigurations() | mLayerState.getChangingConfigurations();
    }

    /** {@inheritDoc} */
    @Override
    public final boolean getPadding(@NonNull Rect padding) {
        final LayerState layerState = mLayerState;
        if (layerState.mPaddingMode == PADDING_MODE_NEST) {
            computeNestedPadding(padding);
        } else {
            computeStackedPadding(padding);
        }

        final int paddingT = layerState.mPaddingTop;
        final int paddingB = layerState.mPaddingBottom;

        // Resolve padding for RTL. Relative padding overrides absolute
        // padding.
        final boolean isLayoutRtl =
                DrawableCompat.getLayoutDirection(this) == LayoutDirection.RTL;
        final int paddingRtlL = isLayoutRtl ? layerState.mPaddingEnd : layerState.mPaddingStart;
        final int paddingRtlR = isLayoutRtl ? layerState.mPaddingStart : layerState.mPaddingEnd;
        final int paddingL = paddingRtlL >= 0 ? paddingRtlL : layerState.mPaddingLeft;
        final int paddingR = paddingRtlR >= 0 ? paddingRtlR : layerState.mPaddingRight;

        // If padding was explicitly specified (e.g. not -1) then override the
        // computed padding in that dimension.
        if (paddingL >= 0) {
            padding.left = paddingL;
        }

        if (paddingT >= 0) {
            padding.top = paddingT;
        }

        if (paddingR >= 0) {
            padding.right = paddingR;
        }

        if (paddingB >= 0) {
            padding.bottom = paddingB;
        }

        return padding.left != 0 || padding.top != 0 || padding.right != 0 || padding.bottom != 0;
    }

    /**
     * Sets the absolute padding.
     * <p>
     * If padding in a dimension is specified as {@code -1}, the resolved
     * padding will use the value computed according to the padding mode (see
     * {@link #setPaddingMode(int)}).
     * <p>
     * Calling this method clears any relative padding values previously set
     * using {@link #setPaddingRelative(int, int, int, int)}.
     *
     * @param left   the left padding in pixels, or -1 to use computed padding
     * @param top    the top padding in pixels, or -1 to use computed padding
     * @param right  the right padding in pixels, or -1 to use computed padding
     * @param bottom the bottom padding in pixels, or -1 to use computed
     *               padding
     * @see #setPaddingRelative(int, int, int, int)
     */
    private void setPadding(int left, int top, int right, int bottom) {
        final LayerState layerState = mLayerState;
        layerState.mPaddingLeft = left;
        layerState.mPaddingTop = top;
        layerState.mPaddingRight = right;
        layerState.mPaddingBottom = bottom;

        // Clear relative padding values.
        layerState.mPaddingStart = -1;
        layerState.mPaddingEnd = -1;
    }

    /**
     * Sets the relative padding.
     * <p>
     * If padding in a dimension is specified as {@code -1}, the resolved
     * padding will use the value computed according to the padding mode (see
     * {@link #setPaddingMode(int)}).
     * <p>
     * Calling this method clears any absolute padding values previously set
     * using {@link #setPadding(int, int, int, int)}.
     *
     * @param start  the start padding in pixels, or -1 to use computed padding
     * @param top    the top padding in pixels, or -1 to use computed padding
     * @param end    the end padding in pixels, or -1 to use computed padding
     * @param bottom the bottom padding in pixels, or -1 to use computed
     *               padding
     * @see #setPadding(int, int, int, int)
     */
    private void setPaddingRelative(int start, int top, int end, int bottom) {
        final LayerState layerState = mLayerState;
        layerState.mPaddingStart = start;
        layerState.mPaddingTop = top;
        layerState.mPaddingEnd = end;
        layerState.mPaddingBottom = bottom;

        // Clear absolute padding values.
        layerState.mPaddingLeft = -1;
        layerState.mPaddingRight = -1;
    }

    /**
     * Returns the left padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the left padding in pixels, or -1 if not explicitly specified
     * @see #setPadding(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getLeftPadding() {
        return mLayerState.mPaddingLeft;
    }

    /**
     * Returns the right padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the right padding in pixels, or -1 if not explicitly specified
     * @see #setPadding(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getRightPadding() {
        return mLayerState.mPaddingRight;
    }

    /**
     * Returns the start padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the start padding in pixels, or -1 if not explicitly specified
     * @see #setPaddingRelative(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getStartPadding() {
        return mLayerState.mPaddingStart;
    }

    /**
     * Returns the end padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the end padding in pixels, or -1 if not explicitly specified
     * @see #setPaddingRelative(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getEndPadding() {
        return mLayerState.mPaddingEnd;
    }

    /**
     * Returns the top padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the top padding in pixels, or -1 if not explicitly specified
     * @see #setPadding(int, int, int, int)
     * @see #setPaddingRelative(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getTopPadding() {
        return mLayerState.mPaddingTop;
    }

    /**
     * Returns the bottom padding in pixels.
     * <p>
     * A return value of {@code -1} means there is no explicit padding set for
     * this dimension. As a result, the value for this dimension returned by
     * {@link #getPadding(Rect)} will be computed from the child layers
     * according to the padding mode (see {@link #getPaddingMode()}.
     *
     * @return the bottom padding in pixels, or -1 if not explicitly specified
     * @see #setPadding(int, int, int, int)
     * @see #setPaddingRelative(int, int, int, int)
     * @see #getPadding(Rect)
     */
    public final int getBottomPadding() {
        return mLayerState.mPaddingBottom;
    }

    /** @param padding for compute nested */
    private void computeNestedPadding(@NonNull Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;

        // Add all the padding.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);

            padding.left += mPaddingL[i];
            padding.top += mPaddingT[i];
            padding.right += mPaddingR[i];
            padding.bottom += mPaddingB[i];
        }
    }

    /** @param padding for compute stacked */
    private void computeStackedPadding(@NonNull Rect padding) {
        padding.left = 0;
        padding.top = 0;
        padding.right = 0;
        padding.bottom = 0;

        // Take the max padding.
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);

            padding.left = Math.max(padding.left, mPaddingL[i]);
            padding.top = Math.max(padding.top, mPaddingT[i]);
            padding.right = Math.max(padding.right, mPaddingR[i]);
            padding.bottom = Math.max(padding.bottom, mPaddingB[i]);
        }
    }

    /**
     * Populates <code>outline</code> with the first available (non-empty) layer outline.
     *
     * @param outline Outline in which to place the first available layer outline
     */
    @Override
    public final void getOutline(@NonNull Outline outline) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    dr.getOutline(outline);
                    if (!outline.isEmpty()) {
                        return;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setHotspot(float x, float y) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                DrawableCompat.setHotspot(dr, x, y);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setHotspotBounds(int left, int top, int right, int bottom) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                DrawableCompat.setHotspot(dr, left, top);
            }
        }

        if (mHotspotBounds == null) {
            mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            mHotspotBounds.set(left, top, right, bottom);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void getHotspotBounds(@NonNull Rect outRect) {
        if (mHotspotBounds != null) {
            outRect.set(mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean setVisible(boolean visible, boolean restart) {
        final boolean changed = super.setVisible(visible, restart);
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setVisible(visible, restart);
            }
        }

        return changed;
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public final void setDither(boolean dither) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setDither(dither);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int getAlpha() {
        final Drawable dr = getFirstNonNullDrawable();
        if (dr != null) {
            return dr.getAlpha();
        } else {
            return super.getAlpha();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setAlpha(int alpha) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setAlpha(alpha);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setColorFilter(ColorFilter colorFilter) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setColorFilter(colorFilter);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setTintList(ColorStateList tint) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                DrawableCompat.setTintList(dr, tint);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setTintMode(@NonNull Mode tintMode) {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                DrawableCompat.setTintMode(dr, tintMode);
            }
        }
    }

    /** @return first non-null drawable */
    @Nullable
    private Drawable getFirstNonNullDrawable() {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                return dr;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public final int getOpacity() {
        if (mLayerState.mOpacityOverride != PixelFormat.UNKNOWN) {
            return mLayerState.mOpacityOverride;
        }
        return mLayerState.getOpacity();
    }

    /**
     * Sets the opacity of this drawable directly instead of collecting the
     * states from the layers.
     *
     * @param opacity The opacity to use, or {@link PixelFormat#UNKNOWN
     *                PixelFormat.UNKNOWN} for the default behavior
     * @see PixelFormat#UNKNOWN
     * @see PixelFormat#TRANSLUCENT
     * @see PixelFormat#TRANSPARENT
     * @see PixelFormat#OPAQUE
     */
    public final void setOpacity(int opacity) {
        mLayerState.mOpacityOverride = opacity;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isAutoMirrored() {
        return mLayerState.mAutoMirrored;
    }

    /** {@inheritDoc} */
    @Override
    public final void setAutoMirrored(boolean mirrored) {
        mLayerState.mAutoMirrored = mirrored;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setAutoMirrored(mirrored);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void jumpToCurrentState() {
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.jumpToCurrentState();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean isStateful() {
        return mLayerState.isStateful();
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean onStateChange(int[] state) {
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null && dr.isStateful() && dr.setState(state)) {
                refreshChildPadding(i, array[i]);
                changed = true;
            }
        }

        if (changed) {
            updateLayerBounds(getBounds());
        }

        return changed;
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean onLevelChange(int level) {
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null && dr.setLevel(level)) {
                refreshChildPadding(i, array[i]);
                changed = true;
            }
        }

        if (changed) {
            updateLayerBounds(getBounds());
        }

        return changed;
    }

    /** {@inheritDoc} */
    @Override
    protected final void onBoundsChange(@NonNull Rect bounds) {
        updateLayerBounds(bounds);
    }

    /** @param bounds for update */
    private void updateLayerBounds(@NonNull Rect bounds) {
        try {
            suspendChildInvalidation();
            updateLayerBoundsInternal(bounds);
        } finally {
            resumeChildInvalidation();
        }
    }

    /** @param bounds for update */
    private void updateLayerBoundsInternal(@NonNull Rect bounds) {
        int paddingL = 0;
        int paddingT = 0;
        int paddingR = 0;
        int paddingB = 0;

        final Rect outRect = mTmpOutRect;
        final int layoutDirection = DrawableCompat.getLayoutDirection(this);
        final boolean isLayoutRtl = layoutDirection == LayoutDirection.RTL;
        final boolean isPaddingNested = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final ChildDrawable[] array = mLayerState.mChildren;

        for (int i = 0, count = mLayerState.mNum; i < count; i++) {
            final ChildDrawable r = array[i];
            final Drawable d = r.mDrawable;
            if (d == null) {
                continue;
            }

            final int insetT = r.mInsetT;
            final int insetB = r.mInsetB;

            // Resolve insets for RTL. Relative insets override absolute
            // insets.
            final int insetRtlL = isLayoutRtl ? r.mInsetE : r.mInsetS;
            final int insetRtlR = isLayoutRtl ? r.mInsetS : r.mInsetE;
            final int insetL = insetRtlL == INSET_UNDEFINED ? r.mInsetL : insetRtlL;
            final int insetR = insetRtlR == INSET_UNDEFINED ? r.mInsetR : insetRtlR;

            // Establish containing region based on aggregate padding and
            // requested insets for the current layer.
            final Rect container = mTmpContainer;
            container.set(bounds.left + insetL + paddingL, bounds.top + insetT + paddingT,
                    bounds.right - insetR - paddingR, bounds.bottom - insetB - paddingB);

            // Compute a reasonable default gravity based on the intrinsic and
            // explicit dimensions, if specified.
            final int intrinsicW = d.getIntrinsicWidth();
            final int intrinsicH = d.getIntrinsicHeight();
            final int layerW = r.mWidth;
            final int layerH = r.mHeight;
            final int gravity = resolveGravity(r.mGravity, layerW, layerH, intrinsicW, intrinsicH);

            // Explicit dimensions override intrinsic dimensions.
            final int resolvedW = layerW < 0 ? intrinsicW : layerW;
            final int resolvedH = layerH < 0 ? intrinsicH : layerH;
            Gravity.apply(gravity, resolvedW, resolvedH, container, outRect, layoutDirection);
            d.setBounds(outRect);

            if (isPaddingNested) {
                paddingL += mPaddingL[i];
                paddingR += mPaddingR[i];
                paddingT += mPaddingT[i];
                paddingB += mPaddingB[i];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int getIntrinsicWidth() {
        int width = -1;
        int padL = 0;
        int padR = 0;

        final boolean nest = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final boolean isLayoutRtl =
                DrawableCompat.getLayoutDirection(this) == LayoutDirection.RTL;
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            if (r.mDrawable == null) {
                continue;
            }

            // Take the resolved layout direction into account. If start / end
            // padding are defined, they will be resolved (hence overriding) to
            // left / right or right / left depending on the resolved layout
            // direction. If start / end padding are not defined, use the
            // left / right ones.
            final int insetRtlL = isLayoutRtl ? r.mInsetE : r.mInsetS;
            final int insetRtlR = isLayoutRtl ? r.mInsetS : r.mInsetE;
            final int insetL = insetRtlL == INSET_UNDEFINED ? r.mInsetL : insetRtlL;
            final int insetR = insetRtlR == INSET_UNDEFINED ? r.mInsetR : insetRtlR;

            // Don't apply padding and insets for children that don't have
            // an intrinsic dimension.
            final int minWidth = r.mWidth < 0 ? r.mDrawable.getIntrinsicWidth() : r.mWidth;
            final int w = minWidth < 0 ? -1 : minWidth + insetL + insetR + padL + padR;
            if (w > width) {
                width = w;
            }

            if (nest) {
                padL += mPaddingL[i];
                padR += mPaddingR[i];
            }
        }

        return width;
    }

    /** {@inheritDoc} */
    @Override
    public final int getIntrinsicHeight() {
        int height = -1;
        int padT = 0;
        int padB = 0;

        final boolean nest = mLayerState.mPaddingMode == PADDING_MODE_NEST;
        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final ChildDrawable r = array[i];
            if (r.mDrawable == null) {
                continue;
            }

            // Don't apply padding and insets for children that don't have
            // an intrinsic dimension.
            final int minHeight = r.mHeight < 0 ? r.mDrawable.getIntrinsicHeight() : r.mHeight;
            final int h = minHeight < 0 ? -1 : minHeight + r.mInsetT + r.mInsetB + padT + padB;
            if (h > height) {
                height = h;
            }

            if (nest) {
                padT += mPaddingT[i];
                padB += mPaddingB[i];
            }
        }

        return height;
    }

    /**
     * Refreshes the cached padding values for the specified child.
     *
     * @return true if the child's padding has changed
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean refreshChildPadding(int i, @NonNull ChildDrawable r) {
        if (r.mDrawable != null) {
            final Rect rect = mTmpRect;
            r.mDrawable.getPadding(rect);
            if (rect.left != mPaddingL[i] || rect.top != mPaddingT[i]
                    || rect.right != mPaddingR[i] || rect.bottom != mPaddingB[i]) {
                mPaddingL[i] = rect.left;
                mPaddingT[i] = rect.top;
                mPaddingR[i] = rect.right;
                mPaddingB[i] = rect.bottom;
                return true;
            }
        }
        return false;
    }

    /** Ensures the child padding caches are large enough. */
    private void ensurePadding() {
        final int N = mLayerState.mNum;
        if (mPaddingL != null && mPaddingL.length >= N) {
            return;
        }

        mPaddingL = new int[N];
        mPaddingT = new int[N];
        mPaddingR = new int[N];
        mPaddingB = new int[N];
    }

    /** Refresh padding */
    private void refreshPadding() {
        final int N = mLayerState.mNum;
        final ChildDrawable[] array = mLayerState.mChildren;
        for (int i = 0; i < N; i++) {
            refreshChildPadding(i, array[i]);
        }
    }

    /** @return the constant state */
    @Override
    @Nullable
    public final ConstantState getConstantState() {
        if (mLayerState.canConstantState()) {
            mLayerState.mChangingConfigurations = getChangingConfigurations();
            return mLayerState;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    @NonNull
    public final Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mLayerState = createConstantState(mLayerState, null);
            final ChildDrawable[] array = mLayerState.mChildren;
            final int N = mLayerState.mNum;
            for (int i = 0; i < N; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.mutate();
                }
            }
            mMutated = true;
        }
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean onLayoutDirectionChanged(int layoutDirection) {
        boolean changed = false;

        final ChildDrawable[] array = mLayerState.mChildren;
        final int N = mLayerState.mNum;
        for (int i = 0; i < N; i++) {
            final Drawable dr = array[i].mDrawable;
            if (dr != null) {
                changed |= DrawableCompat.setLayoutDirection(dr, layoutDirection);
            }
        }

        updateLayerBounds(getBounds());
        return changed;
    }

    /** The child drawable */
    static final class ChildDrawable {

        /** The drawable. */
        Drawable mDrawable;

        /** Theme attributes. */
        int[] mThemeAttrs;

        /** Int constants */
        @SuppressWarnings("WeakerAccess")
        int
                mDensity = DisplayMetrics.DENSITY_DEFAULT,
                mInsetL, mInsetT, mInsetR, mInsetB,
                mInsetS = INSET_UNDEFINED, mInsetE = INSET_UNDEFINED,
                mWidth = -1, mHeight = -1, mGravity = Gravity.NO_GRAVITY,
                mId = View.NO_ID;


        /**
         * Constructs a new {@link ChildDrawable} with density.
         *
         * @param density a density
         */
        ChildDrawable(int density) {
            mDensity = density;
        }

        /**
         * Constructs a new {@link ChildDrawable}.
         *
         * @param orig origin child
         * @param owner the child owner
         * @param res the resources manager
         */
        ChildDrawable (@NonNull ChildDrawable orig, @NonNull LayerDrawable owner, @Nullable Resources res) {

            final Drawable dr = orig.mDrawable;
            final Drawable clone;
            if (dr != null) {
                final Drawable.ConstantState cs = dr.getConstantState();
                if (cs == null) {
                    clone = dr;
                } else if (res != null) {
                    clone = cs.newDrawable(res);
                } else {
                    clone = cs.newDrawable();
                }
                clone.setCallback(owner);
                DrawableCompat.setLayoutDirection(clone, DrawableCompat.getLayoutDirection(dr));
                clone.setBounds(dr.getBounds());
                clone.setLevel(dr.getLevel());
            } else {
                clone = null;
            }

            mDrawable = clone;
            mThemeAttrs = orig.mThemeAttrs;
            mInsetL = orig.mInsetL;
            mInsetT = orig.mInsetT;
            mInsetR = orig.mInsetR;
            mInsetB = orig.mInsetB;
            mInsetS = orig.mInsetS;
            mInsetE = orig.mInsetE;
            mWidth = orig.mWidth;
            mHeight = orig.mHeight;
            mGravity = orig.mGravity;
            mId = orig.mId;

            mDensity = resolveDensity(res, orig.mDensity);
            if (orig.mDensity != mDensity) {
                applyDensityScaling(orig.mDensity, mDensity);
            }
        }

        /** @return true when theme may be applied */
        boolean canApplyTheme() {
            return mThemeAttrs != null || (mDrawable != null && DrawableCompat.canApplyTheme(mDrawable));
        }

        /** @param targetDensity the density for setup */
        void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                final int sourceDensity = mDensity;
                mDensity = targetDensity;

                applyDensityScaling(sourceDensity, targetDensity);
            }
        }

        /**
         * Apply density scaling.
         *
         * @param sourceDensity the source density
         * @param targetDensity the target density
         */
        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            mInsetL = scaleFromDensity(mInsetL, sourceDensity, targetDensity, false);
            mInsetT = scaleFromDensity(mInsetT, sourceDensity, targetDensity, false);
            mInsetR = scaleFromDensity(mInsetR, sourceDensity, targetDensity, false);
            mInsetB = scaleFromDensity(mInsetB, sourceDensity, targetDensity, false);
            if (mInsetS != INSET_UNDEFINED) {
                mInsetS = scaleFromDensity(mInsetS, sourceDensity, targetDensity, false);
            }
            if (mInsetE != INSET_UNDEFINED) {
                mInsetE = scaleFromDensity(mInsetE, sourceDensity, targetDensity, false);
            }
            if (mWidth > 0) {
                mWidth = scaleFromDensity(mWidth, sourceDensity, targetDensity, true);
            }
            if (mHeight > 0) {
                mHeight = scaleFromDensity(mHeight, sourceDensity, targetDensity, true);
            }
        }
    }


    /** The layer state */
    static class LayerState extends ConstantState {
        /** The attributes of theme. */
        private int[] mThemeAttrs;
        /** The child drawable. */
        private ChildDrawable[] mChildren;

        /** All int properties. */
        private int mNum, mDensity, mOpacity,
                mPaddingTop = -1, mPaddingBottom = -1,
                mPaddingLeft = -1, mPaddingRight = -1,
                mPaddingStart = -1, mPaddingEnd = -1,
                mOpacityOverride = PixelFormat.UNKNOWN,
                mChangingConfigurations, mChildrenChangingConfigurations,
                mPaddingMode = PADDING_MODE_NEST;

        /** All flags. */
        private boolean mHaveOpacity, mHaveIsStateful, mIsStateful,
                mAutoMirrored = false;


        /**
         * Constructs a new {@link LayerState}
         *
         * @param orig origin state
         * @param owner the state owner
         * @param res the resource manager
         */
        public LayerState(@Nullable LayerState orig, @NonNull LayerDrawable owner,
                @Nullable Resources res) {

            mDensity = resolveDensity(res, orig != null ? orig.mDensity : 0);

            if (orig != null) {
                final ChildDrawable[] origChildDrawable = orig.mChildren;
                final int N = orig.mNum;

                mNum = N;
                mChildren = new ChildDrawable[N];

                mChangingConfigurations = orig.mChangingConfigurations;
                mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;

                for (int i = 0; i < N; i++) {
                    final ChildDrawable or = origChildDrawable[i];
                    mChildren[i] = new ChildDrawable(or, owner, res);
                }

                mHaveOpacity = orig.mHaveOpacity;
                mOpacity = orig.mOpacity;
                mHaveIsStateful = orig.mHaveIsStateful;
                mIsStateful = orig.mIsStateful;
                mAutoMirrored = orig.mAutoMirrored;
                mPaddingMode = orig.mPaddingMode;
                mThemeAttrs = orig.mThemeAttrs;
                mPaddingTop = orig.mPaddingTop;
                mPaddingBottom = orig.mPaddingBottom;
                mPaddingLeft = orig.mPaddingLeft;
                mPaddingRight = orig.mPaddingRight;
                mPaddingStart = orig.mPaddingStart;
                mPaddingEnd = orig.mPaddingEnd;
                mOpacityOverride = orig.mOpacityOverride;

                if (orig.mDensity != mDensity) {
                    applyDensityScaling(orig.mDensity, mDensity);
                }
            } else {
                mNum = 0;
                mChildren = null;
            }
        }

        /** @param targetDensity the target density for setup */
        private void setDensity(int targetDensity) {
            if (mDensity != targetDensity) {
                final int sourceDensity = mDensity;
                mDensity = targetDensity;

                onDensityChanged(sourceDensity, targetDensity);
            }
        }

        /** The density changed callback. */
        private void onDensityChanged(int sourceDensity, int targetDensity) {
            applyDensityScaling(sourceDensity, targetDensity);
        }

        /**
         * Applying density scaling.
         *
         * @param sourceDensity the source density
         * @param targetDensity the target density
         */
        private void applyDensityScaling(int sourceDensity, int targetDensity) {
            if (mPaddingLeft > 0) {
                mPaddingLeft = scaleFromDensity(
                        mPaddingLeft, sourceDensity, targetDensity, false);
            }
            if (mPaddingTop > 0) {
                mPaddingTop = scaleFromDensity(
                        mPaddingTop, sourceDensity, targetDensity, false);
            }
            if (mPaddingRight > 0) {
                mPaddingRight = scaleFromDensity(
                        mPaddingRight, sourceDensity, targetDensity, false);
            }
            if (mPaddingBottom > 0) {
                mPaddingBottom = scaleFromDensity(
                        mPaddingBottom, sourceDensity, targetDensity, false);
            }
            if (mPaddingStart > 0) {
                mPaddingStart = scaleFromDensity(
                        mPaddingStart, sourceDensity, targetDensity, false);
            }
            if (mPaddingEnd > 0) {
                mPaddingEnd = scaleFromDensity(
                        mPaddingEnd, sourceDensity, targetDensity, false);
            }
        }

        /** {@inheritDoc} */
        @Override
        public final boolean canApplyTheme() {
            if (mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            for (int i = 0; i < N; i++) {
                final ChildDrawable layer = array[i];
                if (layer.canApplyTheme()) {
                    return true;
                }
            }

            return false;
        }

        /** {@inheritDoc} */
        @Override
        public final Drawable newDrawable() {
            return new LayerDrawable(this, null);
        }

        /** {@inheritDoc} */
        @Override
        public final Drawable newDrawable(@Nullable Resources res) {
            return new LayerDrawable(this, res);
        }

        /** {@inheritDoc} */
        @Override
        public final int getChangingConfigurations() {
            return mChangingConfigurations | mChildrenChangingConfigurations;
        }

        /** The opacity */
        private int getOpacity() {
            if (mHaveOpacity) {
                return mOpacity;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;

            // Seek to the first non-null drawable.
            int firstIndex = -1;
            for (int i = 0; i < N; i++) {
                if (array[i].mDrawable != null) {
                    firstIndex = i;
                    break;
                }
            }

            int op;
            if (firstIndex >= 0) {
                op = array[firstIndex].mDrawable.getOpacity();
            } else {
                op = PixelFormat.TRANSPARENT;
            }

            // Merge all remaining non-null drawables.
            for (int i = firstIndex + 1; i < N; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    op = Drawable.resolveOpacity(op, dr.getOpacity());
                }
            }

            mOpacity = op;
            mHaveOpacity = true;
            return op;
        }

        /** @return stateful */
        private boolean isStateful() {
            if (mHaveIsStateful) {
                return mIsStateful;
            }

            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            boolean isStateful = false;
            for (int i = 0; i < N; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null && dr.isStateful()) {
                    isStateful = true;
                    break;
                }
            }

            mIsStateful = isStateful;
            mHaveIsStateful = true;
            return isStateful;
        }

        /** @return can constant state */
        private boolean canConstantState() {
            final ChildDrawable[] array = mChildren;
            final int N = mNum;
            for (int i = 0; i < N; i++) {
                final Drawable dr = array[i].mDrawable;
                if (dr != null && dr.getConstantState() == null) {
                    return false;
                }
            }

            // Don't cache the result, this method is not called very often.
            return true;
        }

        /** Cache invalidation. */
        private void invalidateCache() {
            mHaveOpacity = false;
            mHaveIsStateful = false;
        }

    }
}

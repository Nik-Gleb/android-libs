/*
 * 	TintableHelper.java
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

package widgets;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.libs.widgets.R;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.os.SystemClock.uptimeMillis;
import static android.support.v4.content.ContextCompat.getColorStateList;
import static android.support.v7.widget.TintSupport.getTintList;
import static android.support.v7.widget.TintSupport.tint;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.obtain;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 26/09/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings({ "unused", "WeakerAccess" })
@SuppressLint("ObsoleteSdkInt")
public final class TintableHelper {

  /** The min layers. */
  private static final int LAYERS = 2;
  /** The background layer. */
  private static final int BACKGROUND = 0;
  /** The content layer. */
  private static final int CONTENT = 1;


  /** The helper's callback. */
  private final Callback mCallback;

  /** The empty drawable */
  private final Drawable mEmpty = new ColorDrawable(Color.TRANSPARENT);

  /**
   * Constructs a new {@link TintableHelper}
   *
   * @param callback the helper's callback
   * @param inset content padding
   */
  public TintableHelper(@NonNull Callback callback, @Nullable Rect inset) {
    mCallback = callback;
    final Context context = mCallback.getContext();
    final Drawable background = mCallback.getSuper();
    if (background != null && context != null)
      mCallback.setSuper(makeSelectable(context, background, mEmpty, inset));
  }

  /** @return content drawable */
  @Nullable public final Drawable getDrawable()
  {return output(mCallback.getSuper(), CONTENT);}

  /**
   * @param in the super drawable
   * @param index index of layer
   * @return converted drawable
   */
  @Nullable
  private static Drawable output(@Nullable Drawable in, int index) {
    final LayerDrawable layer;
    return in != null && in instanceof LayerDrawable
        && ((layer = (LayerDrawable) in)).getNumberOfLayers() >= LAYERS ?
        layer.getDrawable(index) : in;
  }

  /** @param drawable content drawable */
  public final void setDrawable(@Nullable Drawable drawable) {
    final Drawable last = mCallback.getSuper();
    mCallback.setSuper(input(mEmpty,
        mCallback.getSuper(), drawable) ?
        mCallback.getSuper() : drawable);
    if (last == mCallback.getSuper() && last != null)
      last.invalidateSelf();
  }

  /**
   * @param def the default drawable
   * @param in the super drawable
   * @param out incoming drawable
   * @return result for super-set
   */
  private static boolean input
  (@NonNull Drawable def, @Nullable Drawable in, @Nullable Drawable out) {
    out = out != null ? out : def; final LayerDrawable layer;
    return in != null && in instanceof LayerDrawable
        && ((layer = (LayerDrawable) in)).getNumberOfLayers() >= LAYERS
        && layer.setDrawableByLayerId(layer.getId(CONTENT), out);
  }

  @SuppressLint("PrivateResource")
  private static void apply
      (@NonNull Context ctx, @NonNull Drawable to, @Nullable final int[] state)
  {tint(to, getTintList(ctx, android.support.v7.appcompat.R.drawable.abc_btn_default_mtrl_shape), PorterDuff.Mode.SRC_IN, state);}

  /** On drawables state changed. */
  public final boolean onStateChanged() {
    final Context context = mCallback.getContext();
    if (context == null) return false;
    final Drawable layer = output(mCallback.getSuper(), BACKGROUND);
    if (layer != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
    {apply(context, layer, mCallback.getDrawableState()); return true;}
    else {return false;}
  }

  /**
   * Prepare selectable background programmatically
   *
   * @param context the android context
   * @param background the background mask
   * @param inset the content insets
   *
   * @return selectable background
   */
  @NonNull
  @SuppressWarnings("ConstantConditions")
  public static LayerDrawable makeSelectable(@NonNull Context context,
      @NonNull Drawable background, @NonNull Drawable empty, @Nullable Rect inset) {
    final LayerDrawable result;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      final TypedValue value = new TypedValue(); final boolean refs = true;
      context.getTheme().resolveAttribute
          (R.attr.colorControlHighlight, value, refs);
      final ColorStateList color = getColorStateList(context, value.resourceId);
      result = new RippleDrawable(color, background, background);
    } else result = new LayerDrawable(new Drawable[] {background, empty});
    result.setId(BACKGROUND,BACKGROUND); result.setId(CONTENT,CONTENT);
    if (inset != null) result.setLayerInset(CONTENT,
        inset.left, inset.top, inset.right, inset.bottom);
    return result;
  }

  /** Redirect click to parent */
  public void performClick() {

    final ViewParent parent = mCallback.getParent();
    if (!(parent instanceof ViewGroup)) return;

    final ViewGroup group = (ViewGroup) parent;
    float x = mCallback.getX() + mCallback.getWidth() * 0.5f;
    float y = mCallback.getY() + mCallback.getHeight() * 0.5f;
    final int meta = 0;

    group.onInterceptTouchEvent
        (obtain(uptimeMillis(), uptimeMillis(), ACTION_DOWN, x, y, meta));
    group.onInterceptTouchEvent
        (obtain(uptimeMillis(), uptimeMillis(), ACTION_UP, x, y, meta));
  }

  /** Tintable Helper Callback. */
  @Keep
  @KeepPublicClassMembers
  public interface Callback {

    /** @param drawable new drawable for super-call. */
    void setSuper(@Nullable Drawable drawable);

    /** @return current drawable by super-call. */
    @Nullable Drawable getSuper();

    /** The android-context. */
    @Nullable Context getContext();

    /** The current drawable state. */
    @Nullable int[] getDrawableState();

    /** The parent view. */
    @NonNull ViewParent getParent();

    /** The start position. */
    float getX();
    /** The top position. */
    float getY();
    /** The width of view. */
    int getWidth();
    /** The height of view. */
    int getHeight();
  }

  /** The drawable outline provider. */
  @Keep
  @KeepPublicClassMembers
  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  @SuppressWarnings("unused")
  public static final class TintableOutlineProvider extends ViewOutlineProvider {

    /** The helper's callback. */
    private final Callback mCallback;

    /**
     * Constructs a new {@link TintableOutlineProvider}.
     *
     * @param callback the helper's callback
     */
    public TintableOutlineProvider(@NonNull Callback callback)
    {mCallback = callback;}

    /**
     * Called to get the provider to populate the Outline.
     *
     * This method will be called by a View when its owned Drawables are
     * invalidated, when the
     * View's size changes, or if {@link View#invalidateOutline()} is called
     * explicitly.
     *
     * The input outline is empty and has an alpha of <code>1.0f</code>.
     *
     * @param view The view building the outline.
     * @param outline The empty outline to be populated.
     */
    @Override
    public final void getOutline(@NonNull View view, @NonNull Outline outline) {
      final Drawable layer = output(mCallback.getSuper(), TintableHelper.BACKGROUND);
      if (layer != null) layer.getOutline(outline);
    }
  }
}

package android.support.v7.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.ScaleDrawable;
import android.libs.widgets.R;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.LruCache;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.graphics.drawable.DrawableWrapper;

import java.lang.reflect.Field;
import java.util.WeakHashMap;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.support.v4.graphics.ColorUtils.compositeColors;
import static android.support.v7.content.res.AppCompatResources.getColorStateList;
import static android.support.v7.widget.ThemeUtils.getDisabledThemeAttrColor;
import static android.support.v7.widget.ThemeUtils.getThemeAttrColor;
import static android.support.v7.widget.ThemeUtils.getThemeAttrColorStateList;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 26/09/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressLint("PrivateResource")
@SuppressWarnings("unused")
public final class TintSupport {

  /** The v7 drawable wrapper-class. */
  private static final Class WRAPPER_CLASS = DrawableWrapper.class;
  /** The internal drawable field name. */
  private static final String DRAWABLE_FILED_NAME = "mDrawable";
  /** The internal drawable field. */
  @Nullable private static final Field DRAWABLE_FIELD =
      getField(WRAPPER_CLASS, DRAWABLE_FILED_NAME);

  /** The max size of ColorFilter LRU-Cache. */
  private static final int COLOR_FILTER_CACHE_SIZE = 6;
  /** The color filter lru-cache. */
  private static final ColorFilterLruCache COLOR_FILTER_CACHE =
      new ColorFilterLruCache(COLOR_FILTER_CACHE_SIZE);

  /** The tint lists hash-map. */
  private static final WeakHashMap<Context, SparseArrayCompat<ColorStateList>>
      TINTS = new WeakHashMap<>();

  /**
   * Drawables which should be tinted with the value of {@code R.attr.colorControlNormal}, using
   * {@link DrawableCompat}'s tinting functionality.
   */
  private static final int[] TINT_COLOR_CONTROL_NORMAL = {
      android.support.v7.appcompat.R.drawable.abc_ic_commit_search_api_mtrl_alpha,
      android.support.v7.appcompat.R.drawable.abc_seekbar_tick_mark_material,
      android.support.v7.appcompat.R.drawable.abc_ic_menu_share_mtrl_alpha,
      android.support.v7.appcompat.R.drawable.abc_ic_menu_copy_mtrl_am_alpha,
      android.support.v7.appcompat.R.drawable.abc_ic_menu_cut_mtrl_alpha,
      android.support.v7.appcompat.R.drawable.abc_ic_menu_selectall_mtrl_alpha,
      android.support.v7.appcompat.R.drawable.abc_ic_menu_paste_mtrl_am_alpha
  };

  /**
   * Drawables which should be tinted using a state list containing values of
   * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated}
   */
  private static final int[] TINT_COLOR_CONTROL_STATE_LIST = {
      android.support.v7.appcompat.R.drawable.abc_tab_indicator_material,
      android.support.v7.appcompat.R.drawable.abc_textfield_search_material
  };

  /**
   * Drawables which should be tinted using a state list containing values of
   * {@code R.attr.colorControlNormal} and {@code R.attr.colorControlActivated} for the checked
   * state.
   */
  private static final int[] TINT_CHECKABLE_BUTTON_LIST = {
      android.support.v7.appcompat.R.drawable.abc_btn_check_material,
      android.support.v7.appcompat.R.drawable.abc_btn_radio_material
  };

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private TintSupport() {throw new AssertionError();}

  /**
   * @param ctx the android context
   * @param id the standard id
   * @return the color state list
   */
  @Nullable
  public static ColorStateList getTintList
  (@NonNull Context ctx, @DrawableRes int id) {

    // Try the cache first (if it exists)
    ColorStateList tint = getTintListFromCache(ctx, id);

    if (tint == null) {
      // ...if the cache did not contain a color state list, try and create one
      if (id == android.support.v7.appcompat.R.drawable.abc_edit_text_material) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_edittext);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_switch_track_mtrl_alpha) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_switch_track);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_switch_thumb_material) {
        tint = createSwitchThumbColorStateList(ctx);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_btn_default_mtrl_shape) {
        tint = createDefaultButtonColorStateList(ctx);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_btn_borderless_material) {
        tint = createBorderlessButtonColorStateList(ctx);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_btn_colored_material) {
        tint = createColoredButtonColorStateList(ctx);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_spinner_mtrl_am_alpha
          || id == android.support.v7.appcompat.R.drawable.abc_spinner_textfield_background_material) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_spinner);
      } else if (contains(TINT_COLOR_CONTROL_NORMAL, id)) {
        tint = getThemeAttrColorStateList(ctx, R.attr.colorControlNormal);
      } else if (contains(TINT_COLOR_CONTROL_STATE_LIST, id)) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_default);
      } else if (contains(TINT_CHECKABLE_BUTTON_LIST, id)) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_btn_checkable);
      } else if (id == android.support.v7.appcompat.R.drawable.abc_seekbar_thumb_material) {
        tint = getColorStateList(ctx, android.support.v7.appcompat.R.color.abc_tint_seek_thumb);
      }
      if (tint != null) addTintListToCache(ctx, id, tint);
    }
    return tint;
  }

  /**
   * @param array the source array
   * @param value the search value
   * @return true if value exists, otherwise - false
   */
  private static boolean contains(@NonNull int[] array, int value)
  {for (int id : array) if (id == value) return true; return false;}

  /**
   * @param context android-context
   * @param resId the resource id
   * @return the color state list
   */
  @Nullable private static ColorStateList getTintListFromCache
  (@NonNull Context context, @DrawableRes int resId) {
    final SparseArrayCompat<ColorStateList> tints = TINTS.get(context);
    return tints != null ? tints.get(resId) : null;
  }

  /**
   * Add tint-list to cache
   *
   * @param ctx the context
   * @param id the resource id
   * @param list the tint-list
   */
  private static void addTintListToCache
  (@NonNull Context ctx, @DrawableRes int id, @NonNull ColorStateList list) {
    SparseArrayCompat<ColorStateList> tints = TINTS.get(ctx);
    if (tints == null) {tints = new SparseArrayCompat<>(); TINTS.put(ctx, tints);}
    tints.append(id, list);
  }

  /**
   * @param context the android-context
   * @return default-button color state list
   */
  @NonNull
  private static ColorStateList createDefaultButtonColorStateList(@NonNull Context context)
  {return createButtonColorStateList(context, getThemeAttrColor(context, R.attr.colorButtonNormal));}

  /**
   * @param context the android context
   * @param resId the standard id
   * @return the color state list
   */
  @SuppressWarnings("unused")
  @NonNull private static ColorStateList createDefaultButtonColorStateList
  (@NonNull Context context, @DrawableRes int resId) {
    // Try the cache first (if it exists)
    ColorStateList result = getTintListFromCache(context, resId);
    if (result != null) return result;
    result = createButtonColorStateList(context,
        getThemeAttrColor(context, R.attr.colorButtonNormal));
    addTintListToCache(context, resId, result); return result;
  }

  /**
   * @param context the android-context
   * @return borderless-button color state list
   */
  private static ColorStateList createBorderlessButtonColorStateList
  (@NonNull Context context) {
    // We ignore the custom tint for borderless buttons
    return createButtonColorStateList(context, Color.TRANSPARENT);
  }

  /**
   * @param context the android-context
   * @return colored-button color state list
   */
  private static ColorStateList createColoredButtonColorStateList
  (@NonNull Context context) {
    return createButtonColorStateList(context,
        getThemeAttrColor(context, R.attr.colorAccent));
  }

  /**
   * @param context android-context
   * @param color base color
   * @return color state list
   */
  @NonNull private static ColorStateList createButtonColorStateList
  (@NonNull final Context context, @ColorInt final int color) {
    final int[][] states = new int[4][];
    final int[] colors = new int[4];
    int i = 0;

    final int colorControlHighlight = getThemeAttrColor(context, R.attr.colorControlHighlight);
    final int disabledColor = getDisabledThemeAttrColor(context, R.attr.colorButtonNormal);

    // Disabled state
    states[i] = ThemeUtils.DISABLED_STATE_SET;
    colors[i] = disabledColor;
    i++;

    states[i] = ThemeUtils.PRESSED_STATE_SET;
    colors[i] = compositeColors(colorControlHighlight, color);
    i++;

    states[i] = ThemeUtils.FOCUSED_STATE_SET;
    colors[i] = compositeColors(colorControlHighlight, color);
    i++;

    // Default enabled state
    states[i] = ThemeUtils.EMPTY_STATE_SET;
    colors[i] = color;

    return new ColorStateList(states, colors);
  }

  /**
   * @param context android-context
   * @return color state list
   */
  private static ColorStateList createSwitchThumbColorStateList
  (@NonNull Context context) {
    final int[][] states = new int[3][];
    final int[] colors = new int[3];
    int i = 0;

    final ColorStateList thumbColor = getThemeAttrColorStateList(context,
        R.attr.colorSwitchThumbNormal);

    if (thumbColor != null && thumbColor.isStateful()) {

      // If colorSwitchThumbNormal is a valid ColorStateList, extract the
      // default and disabled colors from it

      // Disabled state
      states[i] = ThemeUtils.DISABLED_STATE_SET;
      colors[i] = thumbColor.getColorForState(states[i], 0);
      i++;

      states[i] = ThemeUtils.CHECKED_STATE_SET;
      colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
      i++;

      // Default enabled state
      states[i] = ThemeUtils.EMPTY_STATE_SET;
      colors[i] = thumbColor.getDefaultColor();
    } else {
      // Else we'll use an approximation using the default disabled alpha

      // Disabled state
      states[i] = ThemeUtils.DISABLED_STATE_SET;
      colors[i] = getDisabledThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
      i++;

      states[i] = ThemeUtils.CHECKED_STATE_SET;
      colors[i] = getThemeAttrColor(context, R.attr.colorControlActivated);
      i++;

      // Default enabled state
      states[i] = ThemeUtils.EMPTY_STATE_SET;
      colors[i] = getThemeAttrColor(context, R.attr.colorSwitchThumbNormal);
    }

    return new ColorStateList(states, colors);
  }

  /**
   * Tint the drawable.
   *
   * @param drawable the drawable for tint
   * @param color the color state-list
   * @param mode the porter-duff mode
   * @param state the current state
   */
  @SuppressLint("ObsoleteSdkInt")
  public static void tint(@NonNull Drawable drawable,
      @Nullable ColorStateList color, @Nullable PorterDuff.Mode mode,
      @Nullable int[] state) {

    if (canSafelyMutateDrawable(drawable) && drawable.mutate() != drawable)
    {System.out.println("Mutated drawable isn't the same instance as input.");
      return;}

    if (color == null && mode == null) drawable.clearColorFilter();
    else drawable.setColorFilter(createColorFilter(color, mode != null ? mode :
        PorterDuff.Mode.SRC_IN, state));


    // Pre-v23 there is no guarantee that a state change will invoke an
    // invalidation, so we force it ourselves
    if (Build.VERSION.SDK_INT <= 23) drawable.invalidateSelf();
  }

  /**
   * Some drawable implementations have problems with mutation.
   *
   * This method returns false if there is a known issue in the given
   * drawable's implementation.
   *
   * @param drawable source drawable
   * @return true if can, otherwise - can not
   */
  private static boolean canSafelyMutateDrawable(@NonNull Drawable drawable) {

    if (drawable instanceof DrawableContainer) {
      // If we have a DrawableContainer, let's traverse its child array
      final Drawable.ConstantState state = drawable.getConstantState();
      if (state instanceof DrawableContainer.DrawableContainerState) {
        final DrawableContainer.DrawableContainerState containerState =
            (DrawableContainer.DrawableContainerState) state;
        for (final Drawable child : containerState.getChildren()) {
          if (!canSafelyMutateDrawable(child)) {
            return false;
          }
        }
      }
    /*} else if (drawable instanceof android.support.v4.graphics.drawable.DrawableWrapper) {
      return canSafelyMutateDrawable(DrawableCompat.unwrap(drawable));*/
    } else if (drawable instanceof android.support.v7.graphics.drawable.DrawableWrapper) {
      final Drawable v7 = getWrapped((android.support.v7.graphics.drawable.DrawableWrapper) drawable);
      if (v7 == null) throw new RuntimeException("drawable == null");
      return canSafelyMutateDrawable(v7);
    } else if (drawable instanceof ScaleDrawable) {
      final Drawable scale = ((ScaleDrawable) drawable).getDrawable();
      if (scale == null) throw new RuntimeException("drawable == null");
      return canSafelyMutateDrawable(scale);
    }

    return true;
  }

  /**
   * @param wrapper a drawable wrapper
   * @return a wrapped drawable
   */
  @Nullable private static Drawable getWrapped
  (@NonNull android.support.v7.graphics.drawable.DrawableWrapper wrapper) {
    if (DRAWABLE_FIELD == null) return null;
    try {return (Drawable) DRAWABLE_FIELD.get(wrapper);}
    catch (IllegalAccessException exception) {return null;}
  }

  private static PorterDuffColorFilter createColorFilter
      (@Nullable ColorStateList tint, @Nullable PorterDuff.Mode mode,
          @Nullable final int[] state) {

    return tint == null || mode == null ? null :
        getColorFilter(tint.getColorForState(state, Color.TRANSPARENT), mode);
  }

  /**
   * @param color the color
   * @param mode the porter-duff mode
   * @return the porter duff filter
   */
  private static PorterDuffColorFilter getColorFilter
  (int color, @NonNull PorterDuff.Mode mode) {
    // First, lets see if the cache already contains the color filter
    PorterDuffColorFilter filter = COLOR_FILTER_CACHE.get(color, mode);
    if (filter == null) {
      // Cache miss, so create a color filter and add it to the cache
      filter = new PorterDuffColorFilter(color, mode);
      COLOR_FILTER_CACHE.put(color, mode, filter);
    }
    return filter;
  }

  /** Color filter LRU-Cache. */
  private static final class ColorFilterLruCache extends
      LruCache<Integer, PorterDuffColorFilter> {

    /**
     * Constructs a new {@link ColorFilterLruCache}.
     *
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     * the maximum number of entries in the cache. For all other caches,
     * this is the maximum sum of the sizes of the entries in this cache.
     */
    ColorFilterLruCache(int maxSize) {super(maxSize);}

    /**
     * @param color the color
     * @param mode the mode
     * @return cached filter or null
     */
    @Nullable PorterDuffColorFilter get(int color, @NonNull PorterDuff.Mode mode)
    {return get(genKey(color, mode));}

    /**
     * @param col color to put
     * @param mode mode to put
     * @param filter filter to put
     */
    void put(int col, PorterDuff.Mode mode, PorterDuffColorFilter filter)
    {put(genKey(col, mode), filter);}

    /**
     * @param color the color
     * @param mode the mode
     * @return the generated key
     */
    private static int genKey(int color, @NonNull PorterDuff.Mode mode) {
      int hashCode = 1; hashCode = 31 * hashCode + color;
      hashCode = 31 * hashCode + mode.hashCode();
      return hashCode;
    }
  }



  /**
   * Get reflected field of class
   *
   * @param clazz the type of object
   * @param name the name of field
   *
   * @return the field instance or null
   */
  @Nullable
  private static Field getField(@NonNull Class clazz, @NonNull String name) {
    try {final Field result = clazz.getField(name); result.setAccessible(true);
      return result;} catch (NoSuchFieldException e) {return null;}
  }


}

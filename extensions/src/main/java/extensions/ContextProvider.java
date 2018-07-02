package extensions;

import android.content.ComponentCallbacks2;
import android.content.ContentProvider;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.Window;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 24/06/2018
 */
public final class ContextProvider {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private ContextProvider() {throw new AssertionError();}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull public static <T extends Drawable> Context get(@NonNull T caller)
  {return get(Objects.requireNonNull(getViewByDrawable(caller)));}

  /**
   * @param drawable drawable
   * @return related view
   */
  @SuppressWarnings("WeakerAccess")
  @Nullable public static View getViewByDrawable(@NonNull Drawable drawable) {
    final Drawable.Callback callback = drawable.getCallback();
    if (callback != null) if (callback instanceof View) return (View) callback;
    else if (callback instanceof Drawable) return getViewByDrawable((Drawable) callback);
    return null;
  }

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull public static <T extends View> Context get(@NonNull T caller)
  {return get(caller.getContext());}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull public static <T extends Window> Context get(@NonNull T caller)
  {return get(caller.getContext());}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull public static <T extends Fragment> Context get(@NonNull T caller)
  {return requireNonNull(caller.getActivity()).getApplicationContext();}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull public static <T extends ComponentCallbacks2> Context get(@NonNull T caller) {
    return caller instanceof ContextWrapper ? get((ContextWrapper) caller) :
        caller instanceof ContentProvider ? get(requireNonNull
            (((ContentProvider) caller).getContext())) : get(new RuntimeException());
  }

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull private static <T extends ContextWrapper>
  Context get(@NonNull T caller) {return get((Context)caller);}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return context instance
   */
  @NonNull private static <T extends Context>
  Context get(@NonNull T caller){return caller.getApplicationContext();}

  /**
   * @param caller context caller
   * @param <T> type of caller
   * @return throw exception
   */
  @NonNull private static <T extends RuntimeException>
  Context get(@NonNull T caller){throw caller;}
}

/*
 * ContextProvider.java
 * extensions
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

package extensions;

import android.app.Application;
import android.app.Instrumentation;
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
   * @param instrumentation instrumentation instance
   * @param clazz application clazz
   *
   * @return context instance
   */
  @NonNull public static Context get
  (@NonNull Instrumentation instrumentation, @NonNull Class<? extends Application> clazz, boolean target) {
    final Context context = target ? instrumentation.getTargetContext() : instrumentation.getContext();
    try {final Application result =
      instrumentation.newApplication(context.getClassLoader(), clazz.getName(), context);
      instrumentation.callApplicationOnCreate(result); return result;}
      catch (InstantiationException | IllegalAccessException | ClassNotFoundException exception)
    {throw new RuntimeException(exception);}
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

/*
 * Presenter.java
 * android-arch
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

package arch;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.util.function.BiFunction;

import arch.blocks.ThreeFunction;

import static arch.Retain.get;
import static arch.Retain.put;

/**
 * Base presenter.
 *
 * @param <T> type of component (activity/fragment)
 * @param <U> type of router (some {@link Closeable} inheritor)
 * @param <V> type of view (some {@link View} inheritor)
 *
 * @author Nikitenko Gleb
 * @since 1.0, 10/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public interface Presenter <
      T extends LifecycleOwner,
      U extends Closeable,
      V extends View
    > extends Closeable {



  /**
   * @param view instance or null for bind/unbind the view
   * @param router instance or null for bind/unbind the router
   */
  void setView(@Nullable V view, @Nullable U router);

  /** @param outState saved state container */
  void save(@NonNull Bundle outState);

  /** @return true if state wasn't saved */
  boolean reset();

  /** {@inheritDoc} */
  @Override default void close() {}

  /** @param function unregister function */
  default void setUnregisterFunction
      (@NonNull Runnable function){}

  /**
   * @param tag       presenter's state-key tag
   * @param comp      component (activity/fragment)
   * @param state     saved state container
   *
   * @param presenter presenter factory
   * @param router    router factory
   * @param view      view factory
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   * @param <S> type of presenter (defined by COMPONENT, VIEW and ROUTER)
   *
   * @return the new created scope instance
   */
  @NonNull
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public static <
      T extends LifecycleOwner,
      U extends Closeable,
      V extends View,
      S extends Presenter<T, U, V>
  > S create (
      @NonNull String tag,
      @Nullable Bundle state, @NonNull T comp,
      @NonNull BiFunction<T, Bundle, S> presenter,
      @NonNull ThreeFunction<T, Bundle, S, U> router,
      @NonNull ThreeFunction<T, Bundle, S, V> view
  ) {
    S p; if (state == null || (p = get(state, tag)) == null)
      p = presenter.apply(comp, state);

    final U r = router.apply(comp, state, p);
    final V v = view.apply(comp, state, p);

    final Callbacks callbacks = new Callbacks<>(tag, p, r, v);

    if (comp instanceof Activity)
      ((Activity) comp).getApplication()
          .registerActivityLifecycleCallbacks(callbacks);
    else {
      final Lifecycle lifecycle = comp.getLifecycle();
      p.setUnregisterFunction(() -> lifecycle.removeObserver(callbacks));
      lifecycle.addObserver(callbacks);
    }

    return p;
  }

  /**
   * @param tag   presenter's state-key tag
   * @param state saved state container
   *
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void save(@NonNull String tag, @NonNull Bundle state,
      @NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view)
  {view.save(state); presenter.save(state); put(state, tag, presenter);}

  /**
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void destroy
  (@NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view) {
    view.close();
    //try {router.close();} catch (IOException e) {throw new RuntimeException(e);}
    if (presenter.reset()) presenter.close();
  }

  /**
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void start
  (@NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view)
  {view.start(); presenter.setView(view, router);}

  /**
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void stop
  (@NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view)
  {final V v = null; final U r = null; presenter.setView(v, r); view.stop();}

  /**
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void resume
  (@NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view)
  {view.resume();}

  /**
   * @param presenter presenter instance
   * @param router    router instance
   * @param view      view instance
   *
   * @param <T> type of component (activity/fragment)
   * @param <U> type of router    (some {@link Closeable} inheritor)
   * @param <V> type of view      (some {@link View} inheritor)
   */
  static <T extends LifecycleOwner, U extends Closeable, V extends View>
  void pause
  (@NonNull Presenter<T, U, V> presenter, @NonNull U router, @NonNull V view)
  {view.pause();}

  /** An Activity Lifecycle Callbacks. */
  final class Callbacks
      <T extends LifecycleOwner, U extends Closeable, V extends View>
    implements ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    /** The name of scope. */
    private final String mTag;
    /** Presenter instance. */
    private final Presenter<T, U, V> mPresenter;
    /** Router instance. */
    private final U mRouter;
    /** View instance. */
    private final V mView;

    /**
     * Constructs a new {@link Callbacks}.
     *
     * @param tag   presenter's state-key tag
     *
     * @param presenter presenter instance
     * @param router    router instance
     * @param view      view instance
     */
    Callbacks(@NonNull String tag, @NonNull Presenter<T, U, V> presenter,
        @NonNull U router, @NonNull V view)
    {mTag = tag; mPresenter = presenter; mRouter = router; mView = view;}

    /** {@inheritDoc} */
    @Override public final void onActivityCreated
    (@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    /** {@inheritDoc} */
    @Override public final void onActivityStarted(@NonNull Activity activity)
    {start(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onStart(@NonNull LifecycleOwner owner)
    {start(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onActivityResumed(@NonNull Activity activity)
    {resume(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onResume(@NonNull LifecycleOwner owner)
    {resume(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onActivityPaused(@NonNull Activity activity)
    {pause(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onPause(@NonNull LifecycleOwner owner)
    {pause(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onActivityStopped(@NonNull Activity activity)
    {stop(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onStop(@NonNull LifecycleOwner owner)
    {stop(mPresenter, mRouter, mView);}

    /** {@inheritDoc} */
    @Override public final void onActivityDestroyed(@NonNull Activity activity)
    {activity.getApplication().unregisterActivityLifecycleCallbacks(this);}

    /** {@inheritDoc} */
    @Override public final void onDestroy(@NonNull LifecycleOwner owner)
    {owner.getLifecycle().removeObserver(this);}

    /** {@inheritDoc} */
    @Override public final void onActivitySaveInstanceState
    (@NonNull Activity activity, @NonNull Bundle state)
    {save(mTag, state, mPresenter, mRouter, mView);}
  }

}

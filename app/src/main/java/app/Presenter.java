/*
 * Scope.java
 * app
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

package app;

import android.app.Activity;
import android.app.Application;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.util.Objects;

/**
 * Base scope.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 10/03/2018
 */
@SuppressWarnings("unused")
public interface Presenter<T extends View, U extends LifecycleOwner> extends Closeable {

  /**
   * @param component view-component
   * @param inState saved state container
   */
  void setup(@NonNull U component, @Nullable Bundle inState);

  /** @return true if state wasn't saved */
  boolean reset();

  /** @param view view for attach, null - detach */
  void setView(@Nullable T view);

  /** @return view instance */
  @Nullable
  T getView();

  /** @param outState saved state container */
  void save(@NonNull Bundle outState);

  /** {@inheritDoc} */
  @Override default void close() {}

  /**
   *
   * @param component application context
   * @param tag       scope's state-key tag
   * @param scope     scope factory
   * @param state     saved state container
   *
   * @param <T>       the type of VIEW
   * @param <U>       the type of PRESENTER
   *
   * @return the new created scope instance
   */
  @NonNull
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public static <T extends View, U extends LifecycleOwner, S extends Presenter<T,U>>
  S create (@NonNull String tag, @Nullable Bundle state,
      @NonNull U component, @NonNull Factory<T, U, S> scope) {
    S result; if (state == null ||
        (result = Retain.get(state, tag)) == null)
      result = scope.create(component, state);
    result.setup(component, state);

    if (component instanceof Activity)
      ((Activity) component).getApplication()
          .registerActivityLifecycleCallbacks
              (new ActivityCallbacks<>(tag, result));
    else
      component.getLifecycle()
          .addObserver(new OwnerCallbacks<>(result));

    return result;
  }

  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public static <T extends View, U extends LifecycleOwner> void save
      (@NonNull Presenter<T, U> presenter, @NonNull Bundle outState, @NonNull String tag) {
    final T view = Objects.requireNonNull(presenter.getView());
    view.save(outState); presenter.save(outState);
    Retain.put(outState, tag, presenter);
  }

  @SuppressWarnings("unchecked")
  static <T extends View, U extends LifecycleOwner>
  void start(@NonNull Presenter<T, U> presenter) {
    final T view = Objects.requireNonNull(presenter.getView());
    view.start(); presenter.setView(view);
  }

  @SuppressWarnings("unchecked")
  static <T extends View, U extends LifecycleOwner>
  void stop(@NonNull Presenter<T, U> presenter) {
    final T view = null;  presenter.setView(view);
    Objects.requireNonNull(presenter.getView()).stop();
  }

  static <T extends View, U extends LifecycleOwner> void destroy(@NonNull Presenter<T, U> presenter)
  {Objects.requireNonNull(presenter.getView()).close(); if (presenter.reset()) presenter.close();}

  static <T extends View, U extends LifecycleOwner> void resume(@NonNull Presenter<T, U> presenter)
  {Objects.requireNonNull(presenter.getView()).resume();}

  static <T extends View, U extends LifecycleOwner> void pause(@NonNull Presenter<T, U> presenter)
  {Objects.requireNonNull(presenter.getView()).pause();}

  /** An Activity Lifecycle Callbacks. */
  final class ActivityCallbacks
      <T extends View, U extends LifecycleOwner>
      implements Application.ActivityLifecycleCallbacks {

    /** The name of scope. */
    private final String mTag;
    /** Presenter instance. */
    private final Presenter<T, U> mPresenter;

    /**
     * Constructs a new {@link ActivityCallbacks}.
     *
     * @param tag the tag of presenter
     * @param presenter presenter instance
     */
    ActivityCallbacks
    (@NonNull String tag, @NonNull Presenter<T,U> presenter)
    {mTag = tag; mPresenter = presenter;}

    /** {@inheritDoc} */
    @Override public final void onActivityCreated
    (@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public final void onActivityStarted(@NonNull Activity activity) {start(mPresenter);}

    /** {@inheritDoc} */
    @Override public final void onActivityResumed(@NonNull Activity activity) {}

    /** {@inheritDoc} */
    @Override public final void onActivityPaused(@NonNull Activity activity) {}

    /** {@inheritDoc} */
    @Override public final void onActivityStopped(@NonNull Activity activity) {stop(mPresenter);}

    /** {@inheritDoc} */
    @Override public final void onActivityDestroyed(@NonNull Activity activity)
    {activity.getApplication().unregisterActivityLifecycleCallbacks(this); destroy(mPresenter);}

    /** {@inheritDoc} */
    @Override public final void onActivitySaveInstanceState
    (@NonNull Activity activity, @NonNull Bundle outState)
    {save(mPresenter, outState, mTag);}

  }

  /** Lifecycle owner callbacks. */
  final class OwnerCallbacks
      <T extends View, U extends LifecycleOwner>
      implements DefaultLifecycleObserver {

    /** Presenter instance. */
    private final Presenter<T, U> mPresenter;

    /**
     * Constructs a new {@link ActivityCallbacks}.
     *
     * @param presenter presenter instance
     */
    OwnerCallbacks(@NonNull Presenter<T,U> presenter)
    {mPresenter = presenter;}

    /** {@inheritDoc} */
    @Override public final void onCreate(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onStart(@NonNull LifecycleOwner owner) {start(mPresenter);}

    /** {@inheritDoc} */
    @Override public final void onResume(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onPause(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onStop(@NonNull LifecycleOwner owner) {stop(mPresenter);}

    /** {@inheritDoc} */
    @Override public final void onDestroy(@NonNull LifecycleOwner owner)
    {owner.getLifecycle().removeObserver(this); destroy(mPresenter);}
  }

  /** The Presenter Factory */
  @FunctionalInterface
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public interface Factory
      <T extends View, U extends LifecycleOwner, S extends Presenter<T,U>> {
    /**
     * @param component scope context
     * @param inState saved state
     *
     * @return component scope
     */
    public @NonNull S create(@NonNull U component, @Nullable Bundle inState);
  }
}

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
import android.content.Context;
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
public interface Scope<T extends View, U extends Presenter<T>> extends Closeable {

  /** @param view view for attach, null - detach */
  @SuppressWarnings("SameParameterValue")
  void setView(@Nullable T view);

  /** @return view instance */
  @Nullable
  T getView();

  /** @return presenter instance */
  @NonNull
  U getPresenter();

  /** @param outState save state container */
  default void save(@NonNull Bundle outState, @NonNull String name) {
    final T view = Objects.requireNonNull(getView());
    view.save(outState, name); getPresenter().save(outState, name);
    final Scope<T, U> scope = this; Retain.put(outState, name, scope);
  }

  /** {@inheritDoc} */
  @Override default void close() {}

  /**
   * @param view host activity
   * @param state saved state instance
   * @return the main controller instance
   */
  @NonNull
  static <T extends View, U extends Presenter<T>, S extends Scope<T, U>, V>
  S create (@NonNull Context context, @NonNull Factory<S> scope,
      @NonNull String name, @NonNull View.Factory<T, U, S, V> view,
      @NonNull V comp, @Nullable Bundle state) {
    S result; if (state == null ||
        (result = Retain.get(state, name)) == null)
      result = scope.create(context, state, name);
    result.setView(view.create(result, comp, state, name));
    result.attach(name, comp);
    return result;
  }

  @SuppressWarnings("unchecked")
  default <S> void attach(@NonNull String name, @NonNull S comp) {
    final Scope scope = this;
    if (comp instanceof Activity)
      ((Activity) comp).getApplication()
          .registerActivityLifecycleCallbacks
          (new ActivityCallbacks(name, scope));
    else
      if (comp instanceof LifecycleOwner)
        ((LifecycleOwner)comp).getLifecycle()
            .addObserver(new FragmentCallbacks(scope));
  }

  /** An Activity Lifecycle Callbacks. */
  final class ActivityCallbacks<T extends View, U extends Presenter<T>>
      implements Application.ActivityLifecycleCallbacks {

    /** The name of scope. */
    private final String mName;
    /** Scope instance. */
    private final Scope<T, U> mScope;

    /**
     * Constructs a new {@link ActivityCallbacks}.
     *
     * @param name the name of scope
     * @param scope scope instance
     */
    ActivityCallbacks
    (@NonNull String name, @NonNull Scope<T, U> scope)
    {mName = name; mScope = scope;}

    /** {@inheritDoc} */
    @Override public final void onActivityCreated
    (@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    /** {@inheritDoc} */
    @Override public final void onActivityStarted(@NonNull Activity activity)
    {Objects.requireNonNull(mScope.getView()).start();
    mScope.getPresenter().view(mScope.getView());}

    /** {@inheritDoc} */
    @Override public final void onActivityResumed(@NonNull Activity activity) {}

    /** {@inheritDoc} */
    @Override public final void onActivityPaused(@NonNull Activity activity) {}

    /** {@inheritDoc} */
    @Override public final void onActivityStopped(@NonNull Activity activity)
    {mScope.getPresenter().view(null);
    Objects.requireNonNull(mScope.getView()).stop();}

    /** {@inheritDoc} */
    @Override public final void onActivityDestroyed(@NonNull Activity activity)
    {activity.getApplication().unregisterActivityLifecycleCallbacks(this);
      final T view = Objects.requireNonNull(mScope.getView());
      final boolean isSaved = view.isSaved(); mScope.setView(null);
      view.close(); mScope.getPresenter().stop();
      if (!isSaved) {mScope.getPresenter().close(); mScope.close();}}

    /** {@inheritDoc} */
    @Override public final void onActivitySaveInstanceState
    (@NonNull Activity activity, @NonNull Bundle outState)
    {mScope.save(outState, mName);}

  }

  /** Fragment lifecycle callbacks. */
  final class FragmentCallbacks<T extends View, U extends Presenter<T>>
      implements DefaultLifecycleObserver {

    /** Scope instance. */
    private final Scope<T, U> mScope;

    /**
     * Constructs a new {@link ActivityCallbacks}.
     *
     * @param scope scope instance
     */
    FragmentCallbacks(@NonNull Scope<T, U> scope)
    {mScope = scope;}

    /** {@inheritDoc} */
    @Override public final void onCreate(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onStart(@NonNull LifecycleOwner owner)
    {Objects.requireNonNull(mScope.getView()).start();}

    /** {@inheritDoc} */
    @Override public final void onResume(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onPause(@NonNull LifecycleOwner owner) {}

    /** {@inheritDoc} */
    @Override public final void onStop(@NonNull LifecycleOwner owner)
    {Objects.requireNonNull(mScope.getView()).stop();}

    /** {@inheritDoc} */
    @Override public final void onDestroy(@NonNull LifecycleOwner owner)
    {owner.getLifecycle().removeObserver(this);
      final T view = Objects.requireNonNull(mScope.getView());
      final boolean isSaved = view.isSaved(); mScope.setView(null);
      view.close(); mScope.getPresenter().stop();
      if (!isSaved) {mScope.getPresenter().close(); mScope.close();}}
  }

  /** The Scope Factory */
  @FunctionalInterface
  interface Factory<T> {
    /**
     * @param context application context
     * @param inState saved state
     * @param name the name of scope
     *
     * @return component scope
     */
    @NonNull
    T create(@NonNull Context context, @Nullable Bundle inState,
        @NonNull String name);
  }
}

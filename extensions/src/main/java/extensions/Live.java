/*
 * Live.java
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

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static extensions.Executors.runAndWait;
import static java.util.Objects.deepEquals;
import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 05/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class Live<T> {

  /** Wrapped live state. */
  private final MutableLiveData<T> mDelegate;

  /** Constructs a new {@link Live}.*/
  private Live
  (@NonNull T initial, @Nullable Listener<T> state, @Nullable Consumer<T> consumer)
  {this(state == null ? new MutableLiveData<>() : new MutableLiveData<T>() {
    @Override protected final void onActive() {state.accept(consumer);}
    @Override protected final void onInactive() {state.accept(null);}
  }, initial);}

  /**
   * Constructs a new {@link Live}.
   *
   * @param data wrapped live state
   */
  private Live
  (@NonNull MutableLiveData<T> data, @NonNull T initial)
  {mDelegate = data; set(initial);}

  /** @param value new value */
  @AnyThread protected void set(@NonNull T value) {
    if (deepEquals(mDelegate.getValue(), value)) return;
    runAndWait(() -> mDelegate.setValue(value));
  }

  /**
   * Returns the current value.
   * Note that calling this method on a background thread does not guarantee
   * that the latest value set will be received.
   *
   * @return the current value
   */
  @NonNull public final T get()
  {return requireNonNull(mDelegate.getValue());}

  /**
   * Adds the given observer to the observers list within the lifespan of the
   * given owner. The events are dispatched on the main thread. If LiveData
   * already has state set, it will be delivered to the observer.
   * <p>
   * The observer will only receive events if the owner is in
   * {@link Lifecycle.State#STARTED} or {@link Lifecycle.State#RESUMED}
   * state (active).
   * <p>
   * If the owner moves to the {@link Lifecycle.State#DESTROYED} state,
   * the observer will automatically be removed.
   * <p>
   * When state changes while the {@code owner} is not active, it will not
   * receive any updates. If it becomes active again, it will receive the last
   * available state automatically.
   * <p>
   * LiveData keeps a strong reference to the observer and the owner as long
   * as the given LifecycleOwner is not destroyed. When it is destroyed,
   * LiveData removes references to the observer &amp; the owner.
   * <p>
   * If the given owner is already in {@link Lifecycle.State#DESTROYED} state,
   * LiveData ignores the call.
   * <p>
   * If the given owner, observer tuple is already in the list, the call is
   * ignored. If the observer is already in the list with another owner,
   * LiveData throws an {@link IllegalArgumentException}.
   *
   * @param owner    The LifecycleOwner which controls the observer
   * @param observer The observer that will receive the events
   */
  @MainThread public final void observe
  (@NonNull LifecycleOwner owner, @NonNull Observer<T> observer)
  {mDelegate.observe(owner, observer);}

  /**
   * Adds the given observer to the observers list. This call is similar to
   *  with a LifecycleOwner, which is always active.
   *  This means that the given observer will receive
   * all events and will never be automatically removed. You should manually
   * call {@link #unObserve(Observer)} to stop observing this LiveData.
   * While LiveData has one of such observers, it will be considered as active.
   * <p>
   * If the observer was already added with an owner to this LiveData, LiveData
   * throws an* {@link IllegalArgumentException}.
   *
   * @param observer The observer that will receive the events
   */
  @AnyThread public final void observe(@NonNull Observer<T> observer)
  {runAndWait(() -> mDelegate.observeForever(observer));}

  /**
   * Removes the given observer from the observers list.
   *
   * @param observer The Observer to receive events.
   */
  @AnyThread public final void unObserve(@NonNull Observer<T> observer)
  {runAndWait(() -> mDelegate.removeObserver(observer));}

  /**
   * Add source.
   *
   * @param mediator mediator live data
   * @param observer mediator observer
   */
  @AnyThread private <U> void addSource
  (@NonNull MediatorLiveData<U> mediator, @NonNull Observer<T> observer)
  {runAndWait(() -> mediator.addSource(mDelegate, observer));}

  /**
   * Add source.
   *
   * @param mediator mediator live data
   */
  @AnyThread private <U> void removeSource (@NonNull MediatorLiveData<U> mediator)
  {runAndWait(() -> mediator.removeSource(mDelegate));}

  /**
   * @param function mapper
   * @param <R> new type
   * @return new live
   */
  @AnyThread @NonNull
  public final <R> Live<R>
  map(@NonNull Function<T, R> function)
  {return map(this, function);}

  /**
   * @param function trigger
   * @param <R> new type
   * @return new live
   */
  @AnyThread @NonNull
  public final <R> Live<R>
  trigger(@NonNull Function<T, Live<R>> function)
  {return trigger(this, function);}

  /**
   * @param source source live
   * @param function map function
   * @param <T> source type
   * @param <R> result type
   * @return result live
   */
  @NonNull private static <T, R> Live<R> map
      (@NonNull Live<T> source, @NonNull Function<T, R> function) {
    return new Live.Mutable<>
        (new MediatorLiveData<R>() {
          {source.addSource(this, value -> setValue(function.apply(value)));}
          @Override public final void setValue(R value)
          {if (!deepEquals(getValue(), value)) super.setValue(value);}
        }, function.apply(source.get()));
  }

  /**
   * @param source source live
   * @param function map function
   * @param <T> source type
   * @param <R> result type
   * @return result live
   */
  @SuppressWarnings("unchecked")
  @NonNull private static <T, R> Live<R> trigger
  (@NonNull Live<T> source, @NonNull Function<T, Live<R>> function) {
    final Live<R> live; final MediatorLiveData<R> mediator;
    final Live<R> result = new Live.Mutable<> (
      mediator = new MediatorLiveData<R>() {
        @Nullable private Live<R> mChild = null; {
          source.addSource(this, t -> {
            final Live<R> newLive = function.apply(t);
            if (Objects.equals(newLive, mChild)) return;
            if (mChild != null) mChild.removeSource(this);
            if ((mChild = newLive) != null)
            {setValue(mChild.get());
            mChild.addSource(this, this::setValue);}
          });
    }
      @Override public final void setValue(R value)
      {if (!deepEquals(getValue(), value)) super.setValue(value);}},
        (live = function.apply(source.get())).get());
    live.addSource(mediator, mediator::setValue);
    return result;
  }

  /**
   * @param first     first live
   * @param second    second live
   * @param function  combine function
   *
   * @param <T>       type of first live
   * @param <U>       type of second live
   * @param <R>       type of result live
   *
   * @return          result live
   */
  @NonNull public static <T, U, R> Live<R> combine(@NonNull Live<T> first,
      @NonNull Live<U> second, @NonNull BiFunction<T, U, R> function) {
    return new Live.Mutable<>(new MediatorLiveData<R>() {
      {first.addSource(this, value -> setValue(function.apply(value, second.get())));
        second.addSource(this, value -> setValue(function.apply(first.get(), value)));}
      @Override public final void setValue(R value)
      {if (!deepEquals(getValue(), value)) {super.setValue(value);}}
    }, function.apply(first.get(), second.get()));
  }

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Live)) return false;
    final Live<?> that = (Live<?>) obj;
    return deepEquals(get(), that.get());
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return Objects.hash(get().hashCode());}

  /** {@inheritDoc} */
  @Override public final String toString()
  {return String.valueOf(mDelegate.getValue());}

  /**
   * Create Live-Object.
   *
   * @param initial initial value
   * @param <T> type of data
   *
   * @return Live Mutable
   */
  @SuppressWarnings("unchecked")
  @NonNull public static <T> Mutable<T> create(@NonNull T initial)
  {return new Mutable<>(initial, null, null);}

  /**
   * Create Live-Object.
   *
   * @param initial initial value
   * @param <T> type of data
   *
   * @return Live Mutable
   */
  @SuppressWarnings("unchecked")
  @NonNull public static <T> Mutable<T> create(@NonNull T initial, @NonNull Listener<T> state) {
    final Mutable<T>[] result = new Mutable[1];
    return result[0] = new Mutable<>(initial, state, result[0]::set);
  }

  /**
   * @author Nikitenko Gleb
   * @since 1.0, 05/06/2018
   */
  @FunctionalInterface
  public interface Observer<T> extends android.arch.lifecycle.Observer<T>, Consumer<T> {

    /** {@inheritDoc} */
    @Override default void onChanged(@Nullable T value)
    {onNonNullChanged(requireNonNull(value));}

    /** {@inheritDoc} */
    @Override default void accept(@Nullable T value)
    {onNonNullChanged(requireNonNull(value));}

    /**
     * Called when the state is changed.
     *
     * @param value The new state
     */
    @MainThread void onNonNullChanged(@NonNull T value);
  }

  /** State changes listener. */
  @FunctionalInterface
  public interface Listener<T> {

    /**
     * Calls when Live changed state
     *
     * @param mutable nonNull - activated, Null - deactivated
     */
    void accept(@Nullable Consumer<T> mutable);
  }

  /**
   * Mutable Live
   *
   * @param <T> type of data
   */
  public static final class Mutable<T> extends Live<T> {

    /**
     *  Constructs a new {@link Live.Mutable}.
     *
     *  @param initial initial data
     *  @param state state listener
     */
    private Mutable(@NonNull T initial, @Nullable Listener<T> state,
      @Nullable Consumer<T> consumer) {super(initial, state, consumer);}

    /**
     * Constructs a new {@link Live}.
     *
     * @param data wrapped live data
     * @param initial initial value
     */
    private Mutable
    (@NonNull MutableLiveData<T> data, @NonNull T initial)
    {super(data, initial);}

    /** @param value new value */
    @Override public final void
    set(@NonNull T value)
    {super.set(value);}
  }
}

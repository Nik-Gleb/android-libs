/*
 * Observable.java
 * clean
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

package clean.observables;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Objects.deepEquals;
import static java.util.Optional.ofNullable;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/07/2017
 */
@SuppressWarnings("unused")
public abstract class Observable<T>
    implements Supplier<Optional<T>>,
    Function<Optional[], T>, Closeable {

  /** Activated flags. */
  private static final boolean
      ACTIVATED = true, NOT_ACTIVATED = false;

  /**
   * The list of observers.
   * An observer can be in the list at most once and will never be null.
   */
  private final ArrayList<Runnable> mObservers = new ArrayList<>();

  /** This instance. */
  private final Observable<T> mInstance = this;

  /** This as observer for child dependencies. */
  private final Runnable mObserver = mInstance::invalidate;

  /** Is owner was activated. */
  private final AtomicBoolean mActivated =
      new AtomicBoolean(NOT_ACTIVATED);

  /** Current value. */
  private PropertyReference<T> mValue =
      new PropertyReference<>
          (mInstance::notifyChanged);


  /** Child dependencies. */
  private final List<Observable> mDependencies;

  /** The object was released. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link Observable}.
   *
   * @param dependencies child dependencies
   */
  @SuppressWarnings("unused")
  protected Observable(Observable... dependencies) {
    final Function<Optional[], T> mapper = this;
    mDependencies = asList(dependencies);
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; unregisterAll(); onClose(); mClosed = true;}

  /** Closed callback */
  @SuppressWarnings({ "EmptyMethod", "WeakerAccess" })
  protected void onClose() {}

  /**
   * Adds an observer to the list.
   * The observer cannot be null and it must not already be registered.
   *
   * @param observer the observer to set
   * @return false, if the observer is already registered
   */
  @SuppressWarnings("WeakerAccess")
  public final boolean register(Runnable observer) {
    synchronized (mObservers) {
      if (mObservers.contains(observer)) return false;
      else mObservers.add(observer); return true;
    }
  }

  /**
   * Removes a previously registered observer.
   *
   * The observer must not be null and it must already have been registered.
   *
   * @param observer the observer to reset
   *
   * @throws IllegalStateException the observer is not yet registered
   */
  @SuppressWarnings("WeakerAccess")
  public final boolean unregister(Runnable observer) {
    synchronized (mObservers) {
      final int index = mObservers.indexOf(observer);
      if (index == -1) return false;
      else mObservers.remove(index);
      return true;
    }
  }

  /** Notify all observers about changes */
  private void notifyChanged() {
    synchronized (mObservers) {
      final int start = mObservers.size() - 1;
      for (int i = start; i >= 0; i--)
        mObservers.get(i).run();
    }
  }

  /** Unregister all observers and dependencies */
  private void unregisterAll() {
    mDependencies
        .parallelStream()
        .forEach(child ->
            child.unregister(mObserver));
    synchronized (mObservers)
    {mObservers.clear();}
  }

  /** Invalidate state with dependencies. */
  @SuppressWarnings("WeakerAccess")
  protected final void invalidate()
  {mValue.setAndUpdate(request());}

  /** {@inheritDoc} */
  @Override public final Optional<T> get() {
    T value; boolean prev; do {prev = mActivated.get();
      if (prev) return ofNullable(mValue.get()); mValue.set(request());
    } while (!mActivated.compareAndSet(NOT_ACTIVATED, ACTIVATED));
    try {return get();}
    finally {
      mDependencies
          .parallelStream()
          .forEach(child ->
              child.register(mObserver));
    }
  }

  /** @return new requested and applied data */
  private T request() {
    return
        apply (
            Stream.concat (
                Stream.of
                    (ofNullable(mValue.get())),
                mDependencies
                    .parallelStream()
                    .map(Observable::get)
            ).toArray(Optional[]::new)
        );
  }

  /**
   * @param value initial value
   * @return new created Boolean-Property
   */
  @SuppressWarnings("WeakerAccess")
  protected final PropertyBoolean newProperty(boolean value)
  {return new PropertyBoolean(value, mObserver);}

  /**
   * @param value initial value
   * @return new created Integer-Property
   */
  @SuppressWarnings("WeakerAccess")
  protected final PropertyInteger newProperty(int value)
  {return new PropertyInteger(value, mObserver);}

  /**
   * @param value initial value
   * @return new created Long-Property
   */
  @SuppressWarnings("WeakerAccess")
  protected final PropertyLong newProperty(long value)
  {return new PropertyLong(value, mObserver);}

  /**
   * @param value initial value
   * @return new created Reference-Property
   */
  @SuppressWarnings("WeakerAccess")
  protected final <U> PropertyReference<U> newProperty(U value)
  {return new PropertyReference<>(value, mObserver);}

  /** @return new created Reference-Property */
  @SuppressWarnings("WeakerAccess")
  protected final <U> PropertyReference<U> newProperty()
  {return new PropertyReference<>(mObserver);}

  /**
   * Boolean CAS-Based Property.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 18/03/2018
   */
  @SuppressWarnings("WeakerAccess")
  protected static final class PropertyBoolean extends AtomicBoolean {

    /** An observer. */
    private final Runnable mObserver;

    /**
     * Constructs a new {@link PropertyBoolean}.
     *
     * @param value the initial value
     * @param observer update observer
     */
    private PropertyBoolean(boolean value, Runnable observer)
    {super(value); mObserver = observer;}

    /**
     * Constructs a new {@link PropertyBoolean}.
     *
     * @param observer update observer
     */
    private PropertyBoolean(Runnable observer)
    {mObserver = observer;}

    /** @param value new value for setup */
    public final void setAndUpdate(boolean value) {
      boolean prev; do {prev = get(); if (prev == value) return;}
      while (!compareAndSet(prev, value)); mObserver.run();
    }
  }

  /**
   * Integer CAS-Based Property.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 18/03/2018
   */
  @SuppressWarnings("WeakerAccess")
  protected static final class PropertyInteger extends AtomicInteger {

    /** An observer. */
    private final Runnable mObserver;

    /**
     * Constructs a new {@link PropertyInteger}.
     *
     * @param value the initial value
     * @param observer update observer
     */
    private PropertyInteger(int value, Runnable observer)
    {super(value); mObserver = observer;}

    /**
     * Constructs a new {@link PropertyInteger}.
     *
     * @param observer update observer
     */
    private PropertyInteger(Runnable observer)
    {mObserver = observer;}

    /** @param value new value for setup */
    public final void setAndUpdate(int value) {
      int prev; do {prev = get(); if (prev == value) return;}
      while (!compareAndSet(prev, value)); mObserver.run();
    }

  }

  /**
   * Long CAS-Based Property.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 18/03/2018
   */
  @SuppressWarnings("WeakerAccess")
  protected static final class PropertyLong extends AtomicLong {

    /** An observer. */
    private final Runnable mObserver;

    /**
     * Constructs a new {@link PropertyLong}.
     *
     * @param value the initial value
     * @param observer update observer
     */
    private PropertyLong(long value, Runnable observer)
    {super(value); mObserver = observer;}

    /**
     * Constructs a new {@link PropertyLong}.
     *
     * @param observer update observer
     */
    private PropertyLong(Runnable observer)
    {mObserver = observer;}

    /** @param value new value for setup */
    public final void setAndUpdate(long value) {
      long prev; do {prev = get(); if (prev == value) return;}
      while (!compareAndSet(prev, value)); mObserver.run();
    }

  }

  /**
   * Long CAS-Based Property.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 18/03/2018
   */
  @SuppressWarnings("WeakerAccess")
  protected static final class PropertyReference<T> extends AtomicReference<T> {

    /** An observer. */
    private final Runnable mObserver;

    /**
     * Constructs a new {@link PropertyReference}.
     *
     * @param value initial value
     * @param observer update observer
     */
    private PropertyReference(T value, Runnable observer)
    {super(value); mObserver = observer;}

    /**
     * Constructs a new {@link PropertyReference}.
     *
     * @param observer update observer
     */
    private PropertyReference(Runnable observer)
    {mObserver = observer;}

    /** @param value new value for setup */
    @SuppressWarnings("WeakerAccess")
    public final void setAndUpdate(T value) {
      T prev; do {prev = get(); if (deepEquals(prev, value)) return;}
      while (!compareAndSet(prev, value)); mObserver.run();
    }

  }
}
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

package clean;

import java.io.Closeable;
import java.util.ArrayList;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/07/2017
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public abstract class Observable<T> implements Closeable {

  /**
   * The list of observers.
   * An observer can be in the list at most once and will never be null.
   */
  private final ArrayList<OnChangedListener> mObservers = new ArrayList<>();

  /** The object was released. */
  private volatile boolean mReleased;

  /** {@inheritDoc} */
  @Override
  protected final void finalize() throws Throwable {
    try {
      if (!mReleased) {
        close();
        throw new RuntimeException(
            "\nA resource was acquired at attached stack trace but never released." +
                "\nSee java.io.Closeable for info on avoiding resource leaks."
        );
      }
    } finally {
      super.finalize();
    }
  }

  /** Check state. */
  private void checkState()
  {if (mReleased) throw new IllegalStateException("Already closed");}

  /* Remove all registered observers. */
  public void close() {
    checkState();
    synchronized (mObservers) {
      mObservers.clear();
    }
    mReleased = true;
  }

  /**
   * Adds an observer to the list.
   * The observer cannot be null and it must not already be registered.
   *
   * @param observer the observer to set
   * @return false, if the observer is already registered
   */
  public final boolean registerObserver(OnChangedListener observer) {
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
  public final boolean unregisterObserver(OnChangedListener observer) {
    synchronized (mObservers) {
      final int index = mObservers.indexOf(observer);
      if (index == -1) return false;
      else mObservers.remove(index);
      return true;
    }
  }

  /** Notify about changes. */
  protected final void notifyChanged() {
    synchronized (mObservers) {
      for (int i = mObservers.size() - 1; i >= 0; i--) {
        mObservers.get(i).onChanged();
      }
    }
  }

  /** @return content instance */
  public abstract T get() throws Throwable;

  /** @throws InterruptedException when current thread was interrupted */
  protected static void checkInterrupted() throws InterruptedException
  {if (Thread.interrupted()) throw new InterruptedException();}

  /** The data changed listener. */
  public interface OnChangedListener {
    /** The data changed callback.  */
    void onChanged();
  }

  /** The data observer. */
  public class Observer implements OnChangedListener {
    /**
     * Invokes {@link #notifyChanged()} on each observer.
     *
     * Called when the contents of the data set have changed.
     * The recipient will obtain the new contents the next time it queries the
     * model.
     */
    public void onChanged() {
      notifyChanged();
    }
  }
}
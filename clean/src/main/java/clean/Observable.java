/*
 * 	Observable.java
 * 	model
 *
 * 	Copyright (C) 2017, OmmyChat ltd. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of OmmyChat limited and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to OmmyChat limited and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from OmmyChat limited.
 */

package clean;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.util.ArrayList;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/07/2017
 */
public abstract class Observable<T, U> implements Closeable {

  /**
   * The list of observers.
   * An observer can be in the list at most once and will never be null.
   */
  private final ArrayList<OnChangedListener> mObservers = new ArrayList<>();

  /**
   * Adds an observer to the list.
   * The observer cannot be null and it must not already be registered.
   *
   * @param observer the observer to set
   * @return false, if the observer is already registered
   */
  @SuppressWarnings("UnusedReturnValue")
  public final boolean registerObserver(@NonNull OnChangedListener observer) {
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
  @SuppressWarnings("UnusedReturnValue")
  public final boolean unregisterObserver(@NonNull OnChangedListener observer) {
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

  /* Remove all registered observers. */
  public void close() {
    synchronized (mObservers) {
      mObservers.clear();
    }
  }

  /**
   * @param args the some arguments
   * @return content instance
   */
  @Nullable public abstract U getData
  (@NonNull CancellationSignal signal, @Nullable T args);


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

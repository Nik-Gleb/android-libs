/*
 * 	CancellationSignal.java
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

import android.support.annotation.Nullable;

/**
 * Provides the ability to cancel an operation in progress.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 25/08/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class CancellationSignal {

  /** The cancel listener. */
  private OnCancelListener mOnCancelListener;

  /** The cancellation flags. */
  private boolean
      mIsCanceled = false,
      mCancelInProgress = false;


  /** @return True if the operation has been canceled. */
  public final boolean isCanceled() {
    synchronized (this) {
      return mIsCanceled;
    }
  }

  /**
   * Throws {@link OperationCanceledException}
   * if the operation has been canceled.
   **/
  public final void throwIfCanceled() {
    if (isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Cancels the operation and signals the cancellation listener.
   *
   * If the operation has not yet started,
   * then it will be canceled as soon as it does.
   */
  public final void cancel() {
    final OnCancelListener listener;
    synchronized (this) {
      if (mIsCanceled) {
        return;
      }
      mIsCanceled = true;
      mCancelInProgress = true;
      listener = mOnCancelListener;
    }
    try {
      if (listener != null) {
        listener.onCancel();
      }
    } finally {
      synchronized (this) {
        mCancelInProgress = false;
        notifyAll();
      }
    }
  }

  /**
   * Sets the cancellation listener to be called when canceled.
   *
   * This method is intended to be used by the recipient of a cancellation signal
   * such as a database or a content provider to handle cancellation requests
   * while performing a long-running operation.  This method is not intended to be
   * used by applications themselves.
   *
   * If {@link CancellationSignal#cancel} has already been called, then the provided
   * listener is invoked immediately.
   *
   * This method is guaranteed that the listener will not be called after it
   * has been removed.
   *
   * @param listener The cancellation listener, or null to remove the current listener.
   */
  public void setOnCancelListener(OnCancelListener listener) {
    synchronized (this) {
      waitForCancelFinishedLocked();
      if (mOnCancelListener == listener) {
        return;
      }
      mOnCancelListener = listener;
      if (!mIsCanceled || listener == null) {
        return;
      }
    }
    listener.onCancel();
  }

  /** Wait for cancel finished. */
  private void waitForCancelFinishedLocked() {
    while (mCancelInProgress)
      try {wait();}
      catch (InterruptedException ignored) {}
  }


  /** Listens for cancellation. */
  public interface OnCancelListener {
    /** Called when {@link CancellationSignal#cancel} is invoked. */
    void onCancel();
  }

  /** An exception type that is thrown when an operation in progress is canceled. */
  public static final class OperationCanceledException extends RuntimeException {

    /** Constructs a new {@link OperationCanceledException} */
    public OperationCanceledException() {this(null);}

    /** Constructs a new {@link OperationCanceledException} */
    public OperationCanceledException(@Nullable String message)
    {super(message != null ? message : "The operation has been canceled.");}
  }
}

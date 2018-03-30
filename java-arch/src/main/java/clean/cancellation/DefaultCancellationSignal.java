/*
 * DefaultCancellationSignal.java
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

package clean.cancellation;

/**
 * Default Cancellation Signal.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class DefaultCancellationSignal implements CancellationSignal {

  /** The cancel listener. */
  private OnCancelListener mOnCancelListener;

  /** The cancellation flags. */
  private boolean
      mIsCanceled = false,
      mCancelInProgress = false;

  /** The object was released. */
  private volatile boolean mReleased;

  /** {@inheritDoc}. */
  @Override
  public final boolean isCanceled() {
    synchronized (this) {
      return mIsCanceled;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void throwIfCanceled() {
    if (isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /** {@inheritDoc} */
  @Override
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

  /** {@inheritDoc} */
  @Override
  public final void setOnCancelListener(OnCancelListener listener) {
    synchronized (this) {
      while (mCancelInProgress)
        try {wait();}
        catch (InterruptedException ignored) {}
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

  /** {@inheritDoc} */
  @Override
  public final void close() {
    checkState();
    setOnCancelListener(null);
    mReleased = true;
  }

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
}

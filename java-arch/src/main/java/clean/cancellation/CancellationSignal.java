/*
 * CancellationSignal.java
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

import java.io.Closeable;

/**
 * Abstract Cancellation Signal.
 * Provides the ability to cancel an operation in progress.
 *
 * @author Nikitenko Gleb
 * @since 2.0, 25/08/2017
 * @since 1.0, 15/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public interface CancellationSignal extends Closeable {

  /** @return True if the operation has been canceled. */
  boolean isCanceled();

  /** Throws {@link OperationCanceledException} if the operation has been canceled.*/
  void throwIfCanceled();

  /**
   * Cancels the operation and signals the cancellation listener.
   *
   * If the operation has not yet started,
   * then it will be canceled as soon as it does.
   */
  void cancel();

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
  void setOnCancelListener(OnCancelListener listener);

  /** {@inheritDoc} */
  void close();

  /** Listens for cancellation. */
  interface OnCancelListener {
    /** Called when {@link CancellationSignal#cancel} is invoked. */
    void onCancel();
  }

  /** An exception type that is thrown when an operation in progress is canceled. */
  final class OperationCanceledException extends RuntimeException {

    /** Constructs a new {@link OperationCanceledException} */
    public OperationCanceledException() {this(null);}

    /** Constructs a new {@link OperationCanceledException} */
    public OperationCanceledException(String message)
    {super(message != null ? message : "The operation has been canceled.");}
  }

}

/*
 * AsyncTask.java
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
import java.util.Objects;

/**
 * Asynchronous Task.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 25/02/2018
 */
final class AsyncTask implements Runnable, Closeable {

  /** AsyncTask ID. */
  private final int mId;

  /** I/O fields.. */
  @SuppressWarnings("WeakerAccess")
  volatile Object input, output;

  /** Worker thread. */
  volatile Thread thread = null;

  /** Worker function. */
  volatile Function function = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link AsyncTask}.
   *
   * @param id    TASK ID
   * @param data  TASK ARGS
   */
  AsyncTask(int id, Object data) {mId = id; input = data;}

  /** {@inheritDoc} */
  @SuppressWarnings("WeakerAccess")
  public final boolean apply(AsyncTask task) {
    final Object input = task.input;
    if (same(input)) return false;
    this.input = input; final Thread t;
    if ((t = this.thread) != null)
      t.interrupt();
    return true;
  }

  /** {@inheritDoc} */
  @Override public final void run() {
    final Thread t = thread;
    final Function f = function;
    if (t == null || f == null) return;
    Object input, output; boolean exit;
    do {
      input = this.input; exit = true;
      try {
        this.output = (output = f.apply(input)) == null ? Void.TYPE : output;}
      catch (Throwable throwable) {//
        final boolean interrupted = throwable instanceof InterruptedException;
        if (interrupted && (exit = same(input))) t.interrupt();
        if (exit && !interrupted) this.output = throwable;
      }
    } while (!exit);
  }

  /**
   * @param input new input
   * @return true if this new input equals already existing
   */
  private boolean same(Object input)
  {return Objects.equals(this.input, input);}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; output = input = null; mClosed = true;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object object) {
    return this == object || object instanceof AsyncTask ?
        mId == ((AsyncTask) object).mId :
        object instanceof Integer && mId == (Integer) object;
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return mId;}

  /** {@inheritDoc} */
  @Override public final String toString() {
    final StringBuilder builder =
        new StringBuilder(getClass().getSimpleName())
            .append("{")
              .append("mId=")       .append(mId)
              .append(", input=")  .append(input)
              .append(", output=") .append(output)
            .append('}');
    try {return builder.toString();}
    finally {builder.setLength(0);}
  }

}

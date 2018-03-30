/*
 * WorkerLooper.java
 * android-arch
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

package arch;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.lang.Thread;
import java.util.function.BiConsumer;

/** The worker looper. */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class WorkerLooper implements Closeable {

  /** The thread */
  private final Thread mThread;

  /** The looper. */
  private volatile Looper mLooper = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link WorkerLooper}.
   */
  public WorkerLooper(@NonNull String name, int priority,
      @Nullable BiConsumer<Looper, MessageQueue> init,
      @Nullable Runnable close) {
    mThread = new Thread(() -> {
      if (mClosed) return; Looper.prepare();
      if (init != null) init.accept
          (mLooper = Looper.myLooper(), Looper.myQueue());
      if (mClosed) {if (close != null) close.run(); return;}
      Looper.loop(); if (close != null) close.run();
    }){{setName(name); Process.setThreadPriority(priority);}};
    mThread.start();
  }

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    final Looper looper = mLooper;
    if (looper == null) return;
    Message.obtain(new Handler(looper),
        looper::quitSafely).sendToTarget();
    try {mThread.join();}
    catch (InterruptedException exception)
    {Thread.currentThread().interrupt();}
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

}
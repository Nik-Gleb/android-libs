/*
 * Thread.java
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

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;

/**
 * Async Thread.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class Thread extends java.lang.Thread implements Closeable {

  /** Global "LATCH". */
  private static final CountDownLatch LATCH =
      new CountDownLatch(/*1*/0);

  /** Lock monitor. */
  private final Object mLock = getLock();

  /** Poolable thread. */
  private final boolean mPool =
      getName() != null &&
      getName().startsWith("pool");

  /** Interruption listener. */
  private OnInterruptedListener mOnInterruptedListener = null;

  /** {@inheritDoc} */
  public Thread() {}
  /** {@inheritDoc} */
  public Thread(Runnable runnable) {super(runnable);}
  /** {@inheritDoc} */
  public Thread(ThreadGroup threadGroup, Runnable runnable)
  {super(threadGroup, runnable);}
  /** {@inheritDoc} */
  public Thread(String s) {super(s);}
  /** {@inheritDoc} */
  public Thread(ThreadGroup threadGroup, String s) {super(threadGroup, s);}
  /** {@inheritDoc} */
  public Thread(Runnable runnable, String s) {super(runnable, s);}
  /** {@inheritDoc} */
  public Thread(ThreadGroup threadGroup, Runnable runnable, String s)
  {super(threadGroup, runnable, s);}
  /** {@inheritDoc} */
  public Thread(ThreadGroup threadGroup, Runnable runnable, String s, long l)
  {super(threadGroup, runnable, s, l);}

  /** {@inheritDoc} */
  @Override public void run() {
    if (mPool) try {LATCH.await();}
    catch (InterruptedException exception)
    {interrupt();}
    super.run();
  }

  /** @param listener interruption callback. */
  public final void setOnInterruptedListener
  (OnInterruptedListener listener)
  {synchronized (mLock) {mOnInterruptedListener = listener;}}

  /** {@inheritDoc} */
  @Override public final void interrupt() {
    super.interrupt();
    synchronized (mLock) {
      if (mOnInterruptedListener != null)
        mOnInterruptedListener.onInterrupted();
    }
  }

  /** @return internal locker. */
  private Object getLock() {
    final String name = "blockerLock";
    try {final Field field = java.lang.Thread.class.getDeclaredField(name);
      field.setAccessible(true); return field.get(this);}
    catch (NoSuchFieldException | IllegalAccessException e)
    {throw new RuntimeException(e);}
  }

  /** Global "UNLOCK" all pool-threads */
  public static void unlock() {LATCH.countDown();}

  /** {@inheritDoc} */
  @Override public final void close() {
    synchronized (mLock) {
      while (isAlive()) try {mLock.wait();}
      catch (InterruptedException exception)
      {throw new RuntimeException(exception);}
    }
  }

  /** Interruption listener. */
  @FunctionalInterface
  public interface OnInterruptedListener {
    /** Calls by {@link java.lang.Thread#interrupt()} */
    void onInterrupted();
  }
}

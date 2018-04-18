/*
 * Thread.java
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

import java.lang.reflect.Field;

import arch.blocks.Module;

/**
 * Async Thread.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class ThreadModule extends Thread implements Module {

  /** Lock monitor. */
  private final Object mLock = getLock();

  /** Interruption listener. */
  private OnInterruptedListener mOnInterruptedListener = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  public ThreadModule() {}
  /** {@inheritDoc} */
  public ThreadModule(Runnable runnable) {super(runnable);}
  /** {@inheritDoc} */
  public ThreadModule(ThreadGroup threadGroup, Runnable runnable)
  {super(threadGroup, runnable);}
  /** {@inheritDoc} */
  public ThreadModule(String s) {super(s);}
  /** {@inheritDoc} */
  public ThreadModule(ThreadGroup threadGroup, String s) {super(threadGroup, s);}
  /** {@inheritDoc} */
  public ThreadModule(Runnable runnable, String s) {super(runnable, s);}
  /** {@inheritDoc} */
  public ThreadModule(ThreadGroup threadGroup, Runnable runnable, String s)
  {super(threadGroup, runnable, s);}
  /** {@inheritDoc} */
  public ThreadModule(ThreadGroup threadGroup, Runnable runnable, String s, long l)
  {super(threadGroup, runnable, s, l);}

  /** @param listener interruption callback. */
  public final void setOnInterruptedListener(OnInterruptedListener listener)
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
    try {final Field field = Thread.class.getDeclaredField(name);
      field.setAccessible(true); return field.get(this);}
    catch (NoSuchFieldException | IllegalAccessException e)
    {throw new RuntimeException(e);}
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return; synchronized (mLock) {
      while (isAlive()) try {mLock.wait();}
      catch (InterruptedException exception)
      {Thread.currentThread().interrupt();}
    } mClosed = true;
  }

  /** Interruption listener. */
  @FunctionalInterface
  public interface OnInterruptedListener {
    /** Calls by {@link Thread#interrupt()} */
    void onInterrupted();
  }
}

/*
 * ThreadFactory.java
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

import arch.blocks.Module;
import arch.blocks.Provider;

import static android.os.Message.obtain;
import static android.os.Process.setThreadPriority;
import static java.lang.Thread.currentThread;

/**
 * Main Thread Factory.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class AndroidThreadFactory implements JavaThreadFactory {

  /** This instance. */
  private final AndroidThreadFactory mInstance = this;

  /** Thread Priorities */
  private final int mProcessPriority, mThreadPriority;

  /** The name of threads. */
  private final String mName;

  /* Priorities of process. */
  /*private static final int
      PROCESS_PRIORITY = Process.THREAD_PRIORITY_URGENT_AUDIO,
      THREAD_PRIORITY = java.lang.Thread.MAX_PRIORITY;*/

  /**
   * Constructs a new {@link AndroidThreadFactory}.
   *
   * @param process process priority
   * @param thread thread priority
   */
  public AndroidThreadFactory(int process, int thread)
  {this(process, thread, null);}

  /**
   * Constructs a new {@link AndroidThreadFactory}.
   *
   * @param process process priority
   * @param thread thread priority
   * @param name thread name
   */
  public AndroidThreadFactory(int process, int thread, @Nullable String name)
  {mProcessPriority = process; mThreadPriority = thread; mName = name;}

  /**
   * @param group  the thread group. If {@code null} and there is a security
   *               manager, the group is determined by {@linkplain
   *               SecurityManager#getThreadGroup SecurityManager
   *               .getThreadGroup()}. If there is not a security manager or
   *               {@code SecurityManager.getThreadGroup()} returns
   *               {@code null}, the group is set to the current thread's
   *               thread group.
   * @param target the object whose {@code run} method is invoked when this
   *               thread is started. If {@code null}, this thread's run
   *               method is invoked.
   * @param name   the name of the new thread
   * @param stack  the desired stack size for the new thread, or zero to
   *               indicate that this parameter is to be ignored.
   *
   * @return new created thread
   */
  @NonNull @Override public final Thread newThread
  (ThreadGroup group, Runnable target, String name, long stack)
  {return newModule(group, name, stack, target);}

  /**
   * @param group  the thread group. If {@code null} and there is a security
   *               manager, the group is determined by {@linkplain
   *               SecurityManager#getThreadGroup SecurityManager
   *               .getThreadGroup()}. If there is not a security manager or
   *               {@code SecurityManager.getThreadGroup()} returns
   *               {@code null}, the group is set to the current thread's
   *               thread group.
   * @param name   the name of the new thread
   * @param stack  the desired stack size for the new thread, or zero to
   *               indicate that this parameter is to be ignored.
   * @param target the object whose {@code run} method is invoked when this
   *               thread is started. If {@code null}, this thread's run
   *               method is invoked.
   *
   * @return new created thread
   */
  @NonNull private ThreadModule newModule(ThreadGroup group, String name,
      long stack, Runnable target) {name = mName != null ? mName : name;
    final ThreadModule result = new ThreadModule(group, target, name, stack)
    {@Override public final void run() {setThreadPriority(mProcessPriority);
      super.run();}}; result.setDaemon(false); result.setPriority(mThreadPriority);
    return result;
  }

  /**
   * Wrap some module factory to looper.
   *
   * @param value source module factory
   *
   * @param <T> type of module
   *
   * @return wrapped module
   */
  @NonNull public final <T extends Module> Supplier<T> wrap(@NonNull Supplier<T> value)
  {return new LooperModule<>(mInstance, new Task<>(value));}


  /** The worker looper. */
  private static final class LooperModule<T extends Module>
      implements Provider<T> {

    /** Worker Thread. */
    private final ThreadModule mThreadModule;

    /** Worker Task. */
    private final Task<T> mTask;

    /** "CLOSE" flag-state. */
    private volatile boolean mClosed;

    /**
     * Constructs a new {@link LooperModule}
     *
     * @param factory thread factory
     * @param task module provider               .
     */
    LooperModule
    (@NonNull AndroidThreadFactory factory, @NonNull Task<T> task) {
      final ThreadGroup group = null; final String name = null; final long stack = 0L;
      mThreadModule = factory.newModule(group, name, stack, mTask = task);
    }

    /** {@inheritDoc} */
    @Override public final void close() {
      if (mClosed) return; mTask.close();
      mThreadModule.close(); mClosed = true;
    }

    /** {@inheritDoc} */
    @Override protected final void finalize() throws Throwable
    {try {close();} finally {super.finalize();}}

    /** {@inheritDoc} */
    @Override public final T get() {start(); return mTask.get();}

    /** Attempt to start thread */
    private void start()
    {if (mThreadModule.getState() == Thread.State.NEW) mThreadModule.start();}
  }

  /** Looper Task. */
  private static final class Task<T> implements Runnable, Provider<T> {

    /** Lock monitor. */
    private final Object mLock = new Object();
    /** The looper. */
    private volatile Looper mLooper = null;
    /** Provided value */
    private volatile T mValue = null;
    /** Value factory. */
    private volatile Supplier<T> mSupplier;

    /** "CLOSE" flag-state. */
    private volatile boolean mClosed;

    /**
     * Constructs a new {@link Task}
     *
     * @param supplier source supplier
     */
    Task(@NonNull Supplier<T> supplier)
    {mSupplier = supplier;}

    /** {@inheritDoc} */
    @Override public final void close() {
      if (mClosed) return;
      final Looper looper = getLooper();
      mValue = null; mLooper = null;
      obtain(new Handler(looper),
          looper::quitSafely)
          .sendToTarget();
      mClosed = true;
    }

    /** {@inheritDoc} */
    @Override protected final void finalize() throws Throwable
    {try {close();} finally {super.finalize();}}


    /** {@inheritDoc} */
    @Override public final void run() {
      if (mClosed) return; Looper.prepare();
      final Looper looper = mLooper; T value = mValue;
      final Supplier<T> factory = mSupplier;
      if (factory != null && (value == null || looper == null))
        synchronized (mLock) {
          if (mSupplier != null) mSupplier = null;
          if (mLooper == null) mLooper = Looper.myLooper();
          if (mValue == null) mValue = value = factory.get();
          mLock.notifyAll();
        }
      if (!mClosed) Looper.loop();
      if (value instanceof Closeable)
        try {((Closeable)value).close();}
        catch (IOException e)
        {throw new RuntimeException(e);}
    }

    /** @return current looper */
    private Looper getLooper() {
      if (mLooper == null)
        synchronized (mLock) {
          while (mLooper == null) {
            try {mLock.wait();}
            catch (InterruptedException e)
            {currentThread().interrupt();}
          }
        }
      return mLooper;
    }

    /** {@inheritDoc} */
    @Override @NonNull public final T get() {
      if (mValue == null)
        synchronized (mLock) {
          while (mValue == null) {
            try {mLock.wait();}
            catch (InterruptedException e)
            {currentThread().interrupt();}
          }
        }
      return mValue;
    }
  }
}

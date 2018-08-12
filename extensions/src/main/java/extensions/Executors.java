/*
 * Executors.java
 * extensions
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

package extensions;

import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;
import static android.os.Message.obtain;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static java.lang.Thread.NORM_PRIORITY;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 16/05/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Executors {

  /** Main Thread Executor. */
  public static Executor MAIN = null;

  /** Work thread name. */
  private static final String WORK_NAME = "Worker";
  /** Work thread priorities. */
  private static final int
      WORK_PROCESS = THREAD_PRIORITY_DEFAULT,
      WORK_THREAD = NORM_PRIORITY;

  /** Worker thread executor */
  public static Executor WORK = null;

  /** Only for tests */
  public static void flat()
  {MAIN = WORK = Runnable::run;}

  /** Only for work */
  public static void async()
  {MAIN = new LooperExecutor(Looper.getMainLooper());
  WORK = serial(WORK_PROCESS, WORK_THREAD, WORK_NAME);}

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Executors() {throw new AssertionError();}

  /** @return new created executor by current looper */
  @NonNull public static Executor create()
  {final Looper looper = myLooper();
  return looper != null ? new LooperExecutor(looper) : null;}

  /**
   * @param process process priority
   * @param thread thread priority
   * @param name thread name
   * @return new created parallel executor
   */
  @NonNull public static Executor parallel(int process, int thread, @NonNull String name)
  {return ThreadPool.newParallel(new ThreadFactory(process, thread, name));}

  /**
   * @param process process priority
   * @param thread thread priority
   * @param name thread name
   * @return new created serial executor
   */
  @NonNull public static Executor serial(int process, int thread, @NonNull String name)
  {return ThreadPool.newSerial(new ThreadFactory(process, thread, name));}

  /**
   * @param process process priority
   * @param thread thread priority
   * @param name thread name
   * @param module wrapped module
   * @param <T> type of module
   * @return new created async-holder
   */
  @NonNull public static <T extends Closeable> AsyncHolder<T> async
  (int process, int thread, @NonNull String name, @NonNull Supplier<T> module)
  {return AsyncHolder.fromTask(new Task<>(module, new ThreadFactory(process, thread, name)));}

  /** @param executor an executor for close */
  public static void close(@NonNull Executor executor)
  {if (executor instanceof ThreadPool) ((ThreadPool) executor).close();}

  /** Check this thread */
  public static void checkWorkerLooper() {
    final Looper looper = myLooper();
    if (looper == null || looper == getMainLooper())
      throw new IllegalStateException("Not within worker-looper");
  }

  /** @return true if this call in main looper */
  public static boolean isMainLooper()
  {return Objects.equals(myLooper(), Looper.getMainLooper());}

  /** @param looper watching looper */
  public static void waitForIdleSync(@NonNull Looper looper) {
    final Looper my = myLooper(); if (Objects.equals(my, looper))
      throw new IllegalArgumentException("Same looper");
    final MessageQueue queue = looper.getQueue(); if (queue.isIdle()) return;
    final boolean[] idle = new boolean[] {false};
    queue.addIdleHandler(() ->
    {synchronized (idle) {idle[0] = true; idle.notifyAll();} return false; });
    synchronized (idle) {while (!idle[0])
      try {idle.wait();} catch(InterruptedException e)
      {Thread.currentThread().interrupt(); break;}
    }
  }

  /** @param executor watching looper */
  public static void waitForIdleSync(@NonNull Executor executor) {
    if (executor.getClass() != LooperExecutor.class)
      throw new IllegalArgumentException("Executor not loop-based");
    waitForIdleSync(((LooperExecutor)executor).mHandler.getLooper());
  }

  /** @param task task for launch */
  @SuppressWarnings("ConstantConditions")
  public static void runAndWait(@NonNull Runnable task) {
    if (Thread.currentThread() == Looper.getMainLooper().getThread()) task.run();
    else
      try {CompletableFuture.runAsync(FrontTask.create(task), MAIN).get();}//
      catch (InterruptedException e) {currentThread().interrupt();}
      catch (ExecutionException e) {throw new RuntimeException(e.getCause());}
  }


  /** Looper Executor. */
  private static final class LooperExecutor implements Executor {

    /** My Thread Handler. */
    private final Handler mHandler;

    /**
     * Constructs a new {@link LooperExecutor}
     * @param looper looper instance
     */
    LooperExecutor(@NonNull Looper looper)
    {mHandler = new Handler(looper);}

    /** {@inheritDoc} */
    @Override public final
    void execute(@NonNull Runnable command) {
      if (command.getClass() == FrontTask.class)
        mHandler.postAtFrontOfQueue(command);
      else mHandler.post(command);
    }

    /** @return looper of this executor */
    @NonNull Looper getLooper()
    {return mHandler.getLooper();}
  }

  /** Front task runnable  */
  public interface FrontTask
      extends Runnable {

    /**
     * @param task source task
     * @return wrapped front task
     */
    @NonNull static
    FrontTask create
    (@NonNull Runnable task)
    {return task::run;}
  }


  /**
   * Async Holder for module
   * @param <T> type of module
   */
  public interface
  AsyncHolder<T extends Closeable>
      extends Supplier<T>, Runnable {
    @NonNull static <T extends Closeable>
    AsyncHolder<T> fromTask
        (@NonNull Task<T> task) {
      return new AsyncHolder<T>() {
        @Override public void run()
        {task.close();}
        @Override public T get()
        {return task.get();}
      };
    }
  }

  /** Looper Task. */
  private static final
  class Task<T extends Closeable>
      implements Runnable, Supplier<T>, Closeable {

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
    Task(@NonNull Supplier<T> supplier, @NonNull ThreadFactory factory) {
      mSupplier = supplier; final Runnable target = this; final long stack = 0L;
      final ThreadGroup group = null; final String name = null;
      (factory.newThread(group, target, name, stack)).start();
    }

    /** {@inheritDoc} */
    @Override public final void close() {
      if (mClosed) return;
      final Looper looper = getLooper();
      final Handler handler = new Handler(looper);
      final T value = mValue;
      mValue = null; mLooper = null;
      obtain(handler, () -> close(value)).sendToTarget();
      synchronized (mLock) {
        while (!mClosed) {
          try {mLock.wait();}
          catch (InterruptedException e)
          {currentThread().interrupt();}
        }
      }
    }

    /** @param value closeable */
    private static void close
    (@Nullable Object value) {
      if (value == null) return;
      if (value instanceof Closeable)
        try {((Closeable)value).close();}
        catch (IOException e)
        {throw new RuntimeException(e);}
        requireNonNull(myLooper()).quit();
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
          if (mLooper == null) mLooper = myLooper();
          if (mValue == null) mValue = factory.get();
          mLock.notifyAll();
        }
      if (!mClosed) Looper.loop(); /*close(value);*/
      synchronized (mLock) {mClosed = true; mLock.notifyAll();}
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
      if (mValue == null && !mClosed)
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

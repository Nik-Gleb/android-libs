/*
 * ThreadPool.java
 * java-arch
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

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Async Thread ThreadPool Handler..
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/02/2018
 */
@SuppressWarnings("unused")
final class ThreadPool extends ThreadPoolExecutor implements Closeable {

  /** Default time unit. */
  private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

  /** Keep Alive TimeOut. */
  private static final long TIME_OUT = 30L;

  /** Cache-mode. */
  @SuppressWarnings("FieldCanBeLocal")
  private static boolean CACHED = true;

  /** Queue capacity. */
  private static final int CAPACITY = 128;

  /** The callback. */
  private final Callback mCallback;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link ThreadPool}.
   *
   * @param callback {@link ThreadPool} callback
   * @param core core pool size
   * @param max maximum pool size
   */
  private ThreadPool(Callback callback, int core, int max) {
    super (core, max, TIME_OUT, TIME_UNIT,
        new LinkedBlockingQueue<>(CAPACITY),
        new ThreadFactory(callback));
    allowCoreThreadTimeOut(CACHED);
    mCallback = callback;
  }

  /** {@inheritDoc} */
  private ThreadPool(int core, int maximum, long keep, TimeUnit unit,
      BlockingQueue<Runnable> queue)
  {super(core, maximum, keep, unit, queue); mCallback = null;}

  /** {@inheritDoc} */
  private ThreadPool(int core, int maximum, long keep, TimeUnit unit,
      BlockingQueue<Runnable> queue, java.util.concurrent.ThreadFactory factory)
  {super(core, maximum, keep, unit, queue, factory); mCallback = null;}

  /** {@inheritDoc} */
  private ThreadPool(int core, int maximum, long keep, TimeUnit unit,
      BlockingQueue<Runnable> queue, RejectedExecutionHandler handler,
      Callback callback)
  {super(core, maximum, keep, unit, queue, handler); mCallback = null;}

  /** {@inheritDoc} */
  private ThreadPool(int core, int maximum, long keep, TimeUnit unit,
      BlockingQueue<Runnable> queue, java.util.concurrent.ThreadFactory factory,
      RejectedExecutionHandler handler)
  {super(core, maximum, keep, unit, queue, factory, handler); mCallback = null;}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; stop(); mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize()
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override protected final <T> RunnableFuture<T> newTaskFor
  (Callable<T> callable) {return mCallback.newTaskFor(callable);}

  /** {@inheritDoc} */
  @Override protected final <T> RunnableFuture<T> newTaskFor
  (Runnable runnable, T value) {return mCallback.newTaskFor(runnable, value);}

  /** {@inheritDoc} */
  protected final void beforeExecute
  (Thread thread, Runnable runnable)  {
    mCallback.beforeExecute(thread, runnable);
  }

  /** {@inheritDoc} */
  protected final void afterExecute
  (Runnable runnable, Throwable throwable)
  {mCallback.afterExecute(runnable, throwable);}

  /** {@inheritDoc} */
  protected final void terminated() {mCallback.terminated();}

  /** @return list of remaining tasks. */
  @SuppressWarnings("UnusedReturnValue")
  private List<Runnable> stop() {
    final long time = 1L; final List<Runnable> tasks = shutdownNow();
    try {if (awaitTermination(time, TIME_UNIT)) return tasks;}
    catch (InterruptedException exception)
    {Thread.currentThread().interrupt();}
    return null;
  }

  /**
   * System Thread Factory.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 22/02/2018
   */
  @SuppressWarnings("unused")
  private static final class ThreadFactory implements
      java.util.concurrent.ThreadFactory {

    /** Stack size. */
    private static final long STACK_SIZE = 0L;

    /** Class of instance. */
    private static final Class<? extends java.util.concurrent.ThreadFactory>
        CLAZZ = findClass();

    /** ThreadPool number. */
    private static final AtomicInteger POOL_NUMBER = getPoolNumber(CLAZZ);

    /** This instance. */
    private final java.util.concurrent.ThreadFactory mInstance =
        Executors.defaultThreadFactory();

    /** Thread group. */
    private final ThreadGroup group = getGroup(CLAZZ, mInstance);

    /** Thread number. */
    private final AtomicInteger threadNumber =
        getThreadNumber(CLAZZ, mInstance);

    /** Name prefix. */
    private final String namePrefix =
        getNamePrefix(CLAZZ, mInstance, POOL_NUMBER);

    /** The factory's callback. */
    private Callback mCallback;

    /**
     * Constructs a new {@link ThreadFactory}.
     *
     * @param callback the factory's callback
     */
    ThreadFactory(Callback callback) {mCallback = callback;}

    /** @return system thread factory. */
    @SuppressWarnings("unchecked")
    private static Class<? extends java.util.concurrent.ThreadFactory>
    findClass() {
      final Class<?>[] classes = Executors.class.getDeclaredClasses();
      for (final Class<?> result : classes)
        if (result.getSimpleName().equals("DefaultThreadFactory"))
          return (Class<? extends java.util.concurrent.ThreadFactory>) result;
      return null;
    }

    /**
     * Access to parent's private fields.
     *
     * @param clazz type of object
     * @param obj object instance
     * @param name name of field
     *
     * @param <T> type of field
     *
     * @return field value
     */
    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField
    (Class<? extends java.util.concurrent.ThreadFactory> clazz,
        java.util.concurrent.ThreadFactory obj, String name) {
      try {final Field field = clazz.getDeclaredField(name);
        field.setAccessible(true); return (T) field.get(obj);}
      catch (NoSuchFieldException | IllegalAccessException e) {return null; }
    }

    /**
     * Access to parent's "poolNumber" field.
     *
     * @param clazz type of object
     *
     * @return field value
     */
    private static AtomicInteger getPoolNumber
    (Class<? extends java.util.concurrent.ThreadFactory> clazz) {
      if (clazz == null) return null; final String name = "poolNumber";
      final int value = 1;
      final java.util.concurrent.ThreadFactory instance = null;
      final AtomicInteger result = getPrivateField(clazz, instance, name);
      return result != null ? result : new AtomicInteger(value);
    }

    /**
     * Access to parent's "threadNumber" field.
     *
     * @param clazz type of object
     * @param obj object instance
     *
     * @return field value
     */
    private static AtomicInteger getThreadNumber
    (Class<? extends java.util.concurrent.ThreadFactory> clazz,
        java.util.concurrent.ThreadFactory obj) {
      final String name = "threadNumber"; final int value = 1;
      final AtomicInteger result = getPrivateField(clazz, obj, name);
      return result != null ? result : new AtomicInteger(value);
    }


    /**
     * Access to parent's "group" field.
     *
     * @param clazz type of object
     * @param obj object instance
     *
     * @return field value
     */
    private static ThreadGroup getGroup
    (Class<? extends java.util.concurrent.ThreadFactory> clazz,
        java.util.concurrent.ThreadFactory obj) {
      final String name = "group";
      final ThreadGroup result = getPrivateField(clazz, obj, name);
      if (result != null) return result;
      final SecurityManager securityManager = System.getSecurityManager();
      return securityManager != null ? securityManager.getThreadGroup() :
          Thread.currentThread().getThreadGroup();
    }

    /**
     * Access to parent's "namePrefix" field.
     *
     * @param clazz type of object
     * @param obj object instance
     * @param poolNumber number of pools
     *
     * @return field value
     */
    private static String getNamePrefix
    (Class<? extends java.util.concurrent.ThreadFactory> clazz,
        java.util.concurrent.ThreadFactory obj,
        AtomicInteger poolNumber) {
      final String name = "namePrefix";
      final String result = getPrivateField(clazz, obj, name);
      return result != null ? result :
          "pool-" + poolNumber.getAndIncrement() + "-thread-";
    }

    /** {@inheritDoc} */
    @Override public final Thread newThread
    (@SuppressWarnings("NullableProblems") Runnable runnable)
    {return mCallback.newThread(group, runnable,
        namePrefix + threadNumber.getAndIncrement(), STACK_SIZE);}
  }


  /**
   * @param callback the {@link ThreadPool} callback
   * @return ThreadPool builder
   */
  @SuppressWarnings("WeakerAccess")
  static ThreadPool newSerial(Callback callback)
  {final int core = 1, max = 1; return new ThreadPool(callback, core, max);}

  /**
   * @param callback the {@link ThreadPool} callback
   * @return ThreadPool builder
   */
  @SuppressWarnings("WeakerAccess")
  static ThreadPool newParallel(Callback callback) {
    /*
     * We want at least 2 threads and at most 4 threads in the core pool,
     * preferring to have 1 less than the CPU count to avoid saturating the CPU
     * with background work.
     */
    final int availableProcessors = Runtime.getRuntime().availableProcessors(),
        core = Math.max(2, Math.min(availableProcessors - 1, 4)),
        max = availableProcessors * 2 + 1;
    return new ThreadPool(callback, core, max);
  }

  /**
   * @param factory the java thread factory
   * @return Thread Pool instance
   */
  @SuppressWarnings("WeakerAccess")
  public static ThreadPool newSerial(@NonNull JavaThreadFactory factory)
  {return newSerial(new DefaultCallback(factory));}

  /**
   * @param factory the java thread factory
   * @return Thread Pool instance
   */
  @SuppressWarnings("WeakerAccess")
  public static ThreadPool newParallel(@NonNull JavaThreadFactory factory)
  {return newParallel(new DefaultCallback(factory));}


  /** The executor callback. */
  @SuppressWarnings("EmptyMethod")
  interface Callback {

    /**
     * Returns a <tt>RunnableFuture</tt> for the given runnable and default
     * value.
     *
     * @param runnable the runnable task being wrapped
     * @param value the default value for the returned future
     *
     * @return a <tt>RunnableFuture</tt> which when run will run the
     * underlying runnable and which, as a <tt>Future</tt>, will yield
     * the given value as its result and provide for cancellation of
     * the underlying task.
     */
    /*default*/ <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value);
    //{return new FutureTask<>(runnable, value);}

    /**
     * Returns a <tt>RunnableFuture</tt> for the given callable task.
     *
     * @param callable the callable task being wrapped
     * @return a <tt>RunnableFuture</tt> which when run will apply the
     * underlying callable and which, as a <tt>Future</tt>, will yield
     * the callable's result as its result and provide for
     * cancellation of the underlying task.
     */
    /*default*/ <T> RunnableFuture<T> newTaskFor(Callable<T> callable);
    //{return new FutureTask<>(callable);}

    /**
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * @param thread the thread that will run task {@code r}
     * @param runnable the task that will be executed
     */
    void beforeExecute(Thread thread, Runnable runnable);

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     * <pre> {@code
     * class ExtendedExecutor extends ThreadPool {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null
     *         && r instanceof Future<?>
     *         && ((Future<?>)r).isDone()) {
     *       try {
     *         Object result = ((Future<?>) r).actions();
     *       } catch (CancellationException ce) {
     *         t = ce;
     *       } catch (ExecutionException ee) {
     *         t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *         // ignore/reset
     *         Thread.currentThread().interrupt();
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param runnable the runnable that has completed
     * @param throwable the exception that caused termination, or null if
     * execution completed normally
     */
    void afterExecute(Runnable runnable, Throwable throwable);

    /**
     * Method invoked when the Handler has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    void terminated();

    /**
     *
     * @param group the thread group. If {@code null} and there is a security
     *              manager, the group is determined by {@linkplain
     *              SecurityManager#getThreadGroup SecurityManager
     *              .getThreadGroup()}. If there is not a security manager or
     *              {@code SecurityManager.getThreadGroup()} returns
     *              {@code null}, the group is set to the current thread's
     *              thread group.
     *
     * @param  target the object whose {@code run} method is invoked when this
     *                thread is started. If {@code null}, this thread's run
     *                method is invoked.
     *
     * @param  name the name of the new thread
     *
     * @param  stackSize the desired stack size for the new thread, or zero to
     *                   indicate that this parameter is to be ignored.
     *
     * @return new created thread
     */
    Thread newThread
    (ThreadGroup group, Runnable target, String name, long stackSize);
  }

  /** Default callback. */
  private static final class DefaultCallback implements Callback {

    /** Thread factory. */
    private final JavaThreadFactory mFactory;

    /**
     * Constructs a new {@link DefaultCallback}.
     *
     * @param factory thread factory
     */
    DefaultCallback(@NonNull JavaThreadFactory factory)
    {mFactory = factory;}

    /** {@inheritDoc} */
    @Override public final <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
    {return new FutureTask<>(runnable, value);}

    /** {@inheritDoc} */
    @Override public final <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
    {return new FutureTask<>(callable);}

    /** {@inheritDoc} */
    @Override public final void beforeExecute(Thread thread, Runnable runnable) {}

    /** {@inheritDoc} */
    @Override public final void afterExecute(Runnable runnable, Throwable throwable) {}

    /** {@inheritDoc} */
    @Override public final void terminated() {}

    /** {@inheritDoc} */
    @Override public final Thread newThread
    (ThreadGroup group, Runnable target, String name, long stackSize)
    {return mFactory.newThread(group, target, name, stackSize);}
  }

  /**
   * For pool thread factory.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 18/04/2018
   */
  @SuppressWarnings("unused")
  @FunctionalInterface
  interface JavaThreadFactory {

    /**
     *
     * @param group the thread group. If {@code null} and there is a security
     *              manager, the group is determined by {@linkplain
     *              SecurityManager#getThreadGroup SecurityManager
     *              .getThreadGroup()}. If there is not a security manager or
     *              {@code SecurityManager.getThreadGroup()} returns
     *              {@code null}, the group is set to the current thread's
     *              thread group.
     *
     * @param  target the object whose {@code run} method is invoked when this
     *                thread is started. If {@code null}, this thread's run
     *                method is invoked.
     *
     * @param  name the name of the new thread
     *
     * @param  stack the desired stack size for the new thread, or zero to
     *                   indicate that this parameter is to be ignored.
     *
     * @return new created thread
     */
    Thread newThread(ThreadGroup group, Runnable target, String name,
        long stack);
  }

  /**
   * Java Thread.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 21/02/2018
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  static class JavaThread extends Thread implements Consumer<Runnable>, Closeable {

    /** Interruption listener. */
    private volatile Runnable mOnInterruptedListener = null;

    /** "CLOSE" flag-state. */
    private volatile boolean mClosed;

    /** {@inheritDoc} */
    public JavaThread() {}
    /** {@inheritDoc} */
    public JavaThread(Runnable runnable) {super(runnable);}
    /** {@inheritDoc} */
    public JavaThread(ThreadGroup threadGroup, Runnable runnable)
    {super(threadGroup, runnable);}
    /** {@inheritDoc} */
    public JavaThread(String s) {super(s);}
    /** {@inheritDoc} */
    public JavaThread(ThreadGroup threadGroup, String s) {super(threadGroup, s);}
    /** {@inheritDoc} */
    public JavaThread(Runnable runnable, String s) {super(runnable, s);}
    /** {@inheritDoc} */
    public JavaThread(ThreadGroup threadGroup, Runnable runnable, String s)
    {super(threadGroup, runnable, s);}
    /** {@inheritDoc} */
    public JavaThread(ThreadGroup threadGroup, Runnable runnable, String s, long l)
    {super(threadGroup, runnable, s, l);}

    /** {@inheritDoc} */
    @Override public final void interrupt() {
      super.interrupt();
      final Runnable listener = mOnInterruptedListener;
      if (listener != null) listener.run();
    }

    /** {@inheritDoc} */
    @Override protected final void finalize() throws Throwable
    {try {close();} finally {super.finalize();}}

    /** {@inheritDoc} */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override public final void close() {
      if (mClosed) return;
      synchronized (this) {
        while (isAlive()) try {wait();}
        catch (InterruptedException exception)
        {Thread.currentThread().interrupt();}
      }
      mClosed = true;
    }

    /** {@inheritDoc} */
    @Override public final void accept(Runnable listener)
    {mOnInterruptedListener = listener;}
  }

  /*
  void shutdownAndAwaitTermination(ExecutorService pool) {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();
      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }*/
}

/*
 * Threader.java
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * Main Async Threader.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 23/02/2018
 */
@SuppressWarnings("unused")
public final class Threader implements Closeable {

  /** Current active tasks. */
  private final HashMap<Integer, AsyncTask> mTasks = new HashMap<>();

  /** The main thread. */
  private final Thread mThread = Thread.currentThread();
  /** Array of functions. */
  private final Function[] mFunctions;
  /** The main thread executor. */
  private final Executor mMain, mWorker;
  /** Thread pool handler. */
  private final Handler mHandler;

  /** Saved state. */
  private final HashMap<Integer, Object> mSavedState;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link Threader}
   *
   * @param builder Threader Builder
   */
  private Threader(Builder builder) {
    mMain = builder.executor;
    mSavedState = builder.savedState;
    mFunctions = builder.functions;
    final Threader instance = this;
    mHandler = new Handler(instance,
        builder.factory, builder.functions);
    mWorker = builder.serial ?
        ThreadPool.newSerial(mHandler) :
        ThreadPool.newParallel(mHandler);
    init();
  }

  /** Start initialization. */
  private void init() {
    for(final Iterator<Map.Entry<Integer, Object>> iterator =
        mSavedState.entrySet().iterator(); iterator.hasNext();) {
      final Map.Entry<Integer, Object> entry = iterator.next();
      apply(entry.getKey(), entry.getValue()); iterator.remove();
    }
  }

  /** Handle all active actions. */
  @SuppressWarnings("WeakerAccess")
  public final void backup() {
    assertMain();
    for(final AsyncTask task:mTasks.values())
      mSavedState.put(task.hashCode(), task.input);
  }

  /**
   * Apply the action.
   *
   * @param id action id
   */
  public final void apply(int id)
  {apply(id, Void.TYPE);}

  /**
   * Apply the action.
   *
   * @param id action id
   * @param args action args
   *
   * @param <T> the type of args
   */
  @SuppressWarnings("WeakerAccess")
  public final <T> void apply(int id, T args)
  {apply(new AsyncTask(id, args));}

  /**
   * Apply the action.
   *
   * @param task instance
   * @return true if applied
   */
  final boolean apply(AsyncTask task) {
    if (mClosed) {
      task.close();
      return false;
    }
    final int key = task.hashCode();

    if (mMain == null) {
      task.thread = mThread;
      task.function = mFunctions[task.hashCode()];
      task.run(); task.thread = null;
      task.function = null;
      mFunctions[key]
          .delivery(task.output);
      task.close();
      return false;
    }

    if (isWorker())
      mMain.execute(() -> apply(task));
    else {

      if (task.output == null) {
        final AsyncTask existing = mTasks.get(key);

        if (existing != null) {
          final boolean result =
              existing.apply(task);
          task.close();
          return result;
        } else {
          AsyncTask temp = mTasks.put(key, task);

          if (temp == null)
            mWorker.execute(task);
          else {
            temp.close();
            temp = mTasks.remove(key);

            if (temp != null) {
              temp.close();
            }
            return false;
          }
        }
      } else {
        final AsyncTask existing = mTasks.remove(key);

        if (existing != null) {
          mFunctions[key]
              .delivery(existing.output);
          existing.close();
        } else {
          task.close();
          return false;
        }
      }
    }
    return true;
  }

  /** @return true if current thread is a main thread */
  private boolean isWorker()
  {return mThread != Thread.currentThread();}

  /** Check current thread */
  private void assertMain() {
    if (isWorker()) throw new IllegalStateException
        ("Shouldn't be caused not on the main-thread");
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    mClosed = true;

    mHandler.close();

    if (mWorker instanceof ThreadPool)
      ((ThreadPool) mWorker).stop();


    final Collection<AsyncTask> tasks = mTasks.values();
    for(final Iterator<AsyncTask> i = tasks.iterator();
        i.hasNext();) {i.next().close(); i.remove();}
    mTasks.clear();

  }

  /** Thread pool executor. */
  private static final class Handler
      implements ThreadPool.Callback, Closeable {

    /** Handler's callback */
    private final Threader mCallback;
    /** The thread factory. */
    private final Factory mFactory;
    /** Functions. */
    private final Function[] mFunctions;

    /** "CLOSE" flag-state. */
    private volatile boolean mClosed;

    /**
     * Constructs a new {@link Handler}.
     *
     * @param callback callback
     * @param factory thread factory
     * @param functions functions
     */
    Handler(Threader callback, Factory factory, Function[] functions)
    {mCallback = callback; mFactory = factory; mFunctions = functions;}

    /** {@inheritDoc} */
    @Override public final <T> RunnableFuture<T> newTaskFor
    (Runnable runnable, T value) {return new FutureTask<>(runnable, value);}

    //** {@inheritDoc} */
    @Override public final <T> RunnableFuture<T> newTaskFor
    (Callable<T> callable) {return new FutureTask<>(callable);}

    /** {@inheritDoc} */
    @Override public final void beforeExecute
    (Thread thread, Runnable runnable) {
      if (mClosed ||!(runnable instanceof AsyncTask)) return;
      final AsyncTask task = (AsyncTask) runnable;
      task.function = mFunctions[task.hashCode()];
      task.thread = thread;
    }

    /** {@inheritDoc} */
    @Override public final void afterExecute
    (Runnable runnable, Throwable throwable) {
      if (mClosed ||!(runnable instanceof AsyncTask)) return;
      final AsyncTask task = (AsyncTask) runnable;
      task.function = null; task.thread = null;
      mCallback.apply(task);
    }

    /** {@inheritDoc} */
    @Override public final void terminated() {}

    /** {@inheritDoc} */
    @Override public final Thread newThread
    (ThreadGroup group, Runnable target, String name, long stack)
    {return mFactory.newThread(group, target, name, stack);}

    /** {@inheritDoc} */
    @Override protected final void finalize() throws Throwable
    {try {close();} finally {super.finalize();}}

    /** {@inheritDoc} */
    @Override public final void close()
    {if (mClosed) return; mClosed = true;}
  }

  /** Thread factory. */
  public interface Factory {

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
    Thread newThread(ThreadGroup group, Runnable target, String name, long stack);
  }

  /**
   * @param executor main thread executor
   * @return Threader Builder
   */
  @SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
  public static Builder newSerial(Executor executor,  HashMap<Integer, Object>  state)
  {final boolean serial = true; return new Builder(executor, serial, state);}

  /** @return Threader Builder */
  @SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
  public static Builder newParallel(Executor executor,  HashMap<Integer, Object>  state)
  {final boolean serial = false; return new Builder(executor, serial, state);}


  /**
   * Used to add parameters to a {@link Threader}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link Threader} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static final class Builder {

    /** Parallel/Serial Mode. */
    final boolean serial;

    /** Main executor. */
    final Executor executor;

    /** Thread factory. */
    Factory factory = null;

    /** Array of functions. */
    Function[] functions = new Function[0];

    /** Saved state. */
    HashMap<Integer, Object> savedState;

    /** Init state. */
    private HashMap<Integer, Object> mInitState = new HashMap<>();

    /** Constructs a new {@link Builder} */
    Builder(Executor executor, boolean serial,  HashMap<Integer, Object>  state)
    {this.serial = serial; this.executor = executor; savedState = state;}

    /** @return this builder, to allow for chaining. */
    @SuppressWarnings("UnusedReturnValue")
    public final Builder factory(Factory factory)
    {this.factory = factory; return this;}

    /** @return this builder, to allow for chaining. */
    public final <T> Builder action(FunctionGet<T> function, FunctionResult<T> result)
    {final FunctionError error = null; return action(function, result, error);}
    /** @return this builder, to allow for chaining. */
    public final <T> Builder action
    (FunctionGet<T> function, FunctionResult<T> result, FunctionError error)
    {add(new RecordGet<>(function, result, error)); return this;}

    /** @return this builder, to allow for chaining. */
    public final <T> Builder action(FunctionSet<T> function)
    {final FunctionError error = null; return action(function, error);}
    /** @return this builder, to allow for chaining. */
    public final <T> Builder action(FunctionSet<T> function, FunctionError error)
    {add(new RecordSet<>(function, error)); return this;}

    /** @return this builder, to allow for chaining. */
    public final Builder action(FunctionVoid function)
    {final FunctionError error = null; return action(function, error);}
    /** @return this builder, to allow for chaining. */
    public final Builder action(FunctionVoid function, FunctionError error)
    {add(new RecordVoid(function, error)); return this;}

    /** @return this builder, to allow for chaining. */
    @SuppressWarnings("UnusedReturnValue")
    public final <T> Builder init(int id)
    {final Object args = null; return init(id, args);}

    /** @return this builder, to allow for chaining. */
    public final <T> Builder init(int id, T args)
    {mInitState.put(id, args); return this;}

    /** @param value new function */
    void add(Function value) {
      final int pos = 0, length = this.functions.length;
      final Function[] functions = new Function[length + 1];
      System.arraycopy(this.functions, pos, functions, pos, length);
      functions[length] = value; this.functions = functions;
    }

    /** @return true if this init first */
    public final boolean isInitial()
    {return savedState == null;}

    /** Create a {@link Threader} from this {@link Builder}. */
    @SuppressWarnings("unchecked")
    public final Threader build() {
      if (factory == null) factory = Thread::new;
      savedState = isInitial() ? mInitState : savedState;
      final Builder builder = this;
      try {return new Threader(builder);}
      finally {savedState.clear();}
    }
  }

  /** "GET" function meta. */
  private static final class RecordGet<T> extends Function {

    /** Request function. */
    final FunctionGet<T> request;

    /** Result-handler function. */
    final FunctionResult<T> result;

    /**
     * Constructs a new {@link RecordGet}.
     *
     * @param request request function
     * @param result result-handler function
     * @param error errors handler
     */
    RecordGet(FunctionGet<T> request, FunctionResult<T> result, FunctionError error)
    {super(error); this.request = request; this.result = result; }

    /** {@inheritDoc} */
    @Override final Object apply(Object args) throws Throwable
    {return request.get();}

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override final void delivery(Object result) {
      if (result == Void.TYPE)
        //throw new RuntimeException("Get can't return a null");
        result = null;
      if (result instanceof Throwable && error != null)
        error.error((Throwable) result);
      else this.result.result((T) result);
    }

  }

  /** "SET" function meta. */
  private static final class RecordSet<T> extends Function {

    /** Request function. */
    final FunctionSet<T> request;

    /**
     * Constructs a new {@link RecordSet}.
     *
     * @param request request function
     * @param error errors handler
     */
    RecordSet(FunctionSet<T> request, FunctionError error)
    {super(error); this.request = request; }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override final Object apply(Object args) throws Throwable
    {request.set((T) args); return null;}

    /** {@inheritDoc} */
    @Override final void delivery(Object result) {
      if (result instanceof Throwable && error != null)
        error.error((Throwable) result);
    }

  }

  /** "VOID" function meta. */
  private static final class RecordVoid extends Function {

    /** Request function. */
    final FunctionVoid request;

    /**
     * Constructs a new {@link RecordVoid}.
     *
     * @param request request function
     * @param error errors handler
     */
    RecordVoid(FunctionVoid request, FunctionError error)
    {super(error); this.request = request; }

    /** {@inheritDoc} */
    @Override final Object apply(Object args) throws Throwable
    {request.apply(); return null;}

    /** {@inheritDoc} */
    @Override final void delivery(Object result) {
      if (result instanceof Throwable && error != null)
        error.error((Throwable) result);
    }
  }

}

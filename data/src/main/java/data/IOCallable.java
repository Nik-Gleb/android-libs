/*
 * IOCallable.java
 * data
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

package data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RunnableFuture;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * IO Callable with {@link CompletableFuture} support.
 *
 * @param <T> the type of result
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/10/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
@FunctionalInterface
public interface IOCallable<T> extends Callable<T> {

  /** {@inheritDoc} */
  @Override @Nullable T call() throws IOException;

  /**
   * @param executor the executor to use for asynchronous execution
   *
   * @return new async future
   */
  @NonNull default CompletableFuture<T> toFuture(@NonNull Executor executor) {
    final CompletableFuture<T> res = new CompletableFuture<>();
    executor.execute(new AsyncSupply<>(res, this)); return res;
  }

  /** @return new async future */
  @NonNull default CompletableFuture<T> toFuture()
  {return toFuture(ForkJoinPool.commonPool());}

  /**
   * Internal Async Supply Task.
   *
   * @param <T> the type of request
   */
  final class AsyncSupply<T>
    extends ForkJoinTask<Void> implements RunnableFuture<Void>,
    CompletableFuture.AsynchronousCompletionTask {

    /** Completable Future Host. */
    private CompletableFuture<T> mFuture;

    /** Async Task. */
    private Callable<? extends T> mTask;

    /**
     * Constructs a new {@link AsyncSupply}.
     *
     * @param future completable future
     * @param task asynchronous task
     */
    AsyncSupply
    (@NonNull CompletableFuture<T> future, Callable<? extends T> task)
    {mFuture = future; mTask = task;}

    /** {@inheritDoc} */
    @Override public final Void getRawResult() {return null;}
    /** {@inheritDoc} */
    @Override protected final void setRawResult(Void value) {}
    /** {@inheritDoc} */
    @Override protected final boolean exec() {run(); return true;}

    /** {@inheritDoc} */
    @Override public final void run() {
      CompletableFuture<T> future; Callable<? extends T> task;
      if ((future = mFuture) != null && (task = mTask) != null) {
        mFuture = null; mTask = null;
        if (!future.isDone()) {
          try {future.complete(task.call());}
          catch (Exception exception)
          {future.completeExceptionally(exception);}
        }
      }
    }
  }


}

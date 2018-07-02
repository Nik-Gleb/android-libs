/*
 * ThreadFactory.java
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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static android.os.Process.setThreadPriority;

/**
 * Main Thread Factory.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
final class ThreadFactory implements ThreadPool.JavaThreadFactory {

  /** This instance. */
  private final ThreadFactory mInstance = this;

  /** Thread Priorities */
  private final int mProcessPriority, mThreadPriority;

  /** The name of threads. */
  private final String mName;

  /* Priorities of process. */
  /*private static final int
      PROCESS_PRIORITY = Process.THREAD_PRIORITY_URGENT_AUDIO,
      THREAD_PRIORITY = java.lang.Thread.MAX_PRIORITY;*/

  /**
   * Constructs a new {@link ThreadFactory}.
   *
   * @param process process priority
   * @param thread thread priority
   */
  public ThreadFactory(int process, int thread)
  {this(process, thread, null);}

  /**
   * Constructs a new {@link ThreadFactory}.
   *
   * @param process process priority
   * @param thread thread priority
   * @param name thread name
   */
  public ThreadFactory(int process, int thread, @Nullable String name)
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
  @NonNull private AndroidThread newModule(ThreadGroup group, String name,
      long stack, Runnable target) {name = mName != null ? mName : name;
    final AndroidThread result = new AndroidThread(group, target, name, stack);
    result.setDaemon(false); result.setPriority(mThreadPriority);
    result.setProcess(mProcessPriority); return result;
  }

  /**
   * Android Thread.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 21/02/2018
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static final class AndroidThread extends ThreadPool.JavaThread {

    /** Process priority */
    private int mProcess = THREAD_PRIORITY_DEFAULT;

    /** {@inheritDoc} */
    AndroidThread() {super();}
    /** {@inheritDoc} */
    AndroidThread(Runnable target) {super(target);}
    /** {@inheritDoc} */
    AndroidThread(ThreadGroup group, Runnable target)
    {super(group, target);}
    /** {@inheritDoc} */
    AndroidThread(String name)
    {super(name);}
    /** {@inheritDoc} */
    AndroidThread(ThreadGroup group, String name)
    {super(group, name);}
    /** {@inheritDoc} */
    AndroidThread(Runnable target, String name)
    {super(target, name);}
    /** {@inheritDoc} */
    AndroidThread(ThreadGroup group, Runnable target, String name)
    {super(group, target, name);}
    /** {@inheritDoc} */
    AndroidThread(ThreadGroup group, Runnable target, String name, long stack)
    {super(group, target, name, stack);}

    /** @param priority base process priority */
    public void setProcess(int priority)
    {mProcess = priority;}

    /** {@inheritDoc} */
    @Override public final void run()
    {setThreadPriority(mProcess); super.run();}

  }

}

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

import android.os.Process;

import arch.interactor.Threader;

/**
 * Main Thread Factory.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class ThreadFactory implements Threader.Factory {

  /** Global instance. */
  @SuppressWarnings({ "WeakerAccess", "unused" })
  public static final ThreadFactory INSTANCE = new ThreadFactory();

  /** Priorities of process. */
  private static final int
      PROCESS_PRIORITY = Process.THREAD_PRIORITY_URGENT_AUDIO,
      THREAD_PRIORITY = java.lang.Thread.MAX_PRIORITY;

  /** Default constructor. */
  private ThreadFactory() {}

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
  @Override public final java.lang.Thread newThread(ThreadGroup group,
      Runnable target, String name, long stack) {
    final Thread result = new Thread(group, target, name, stack)
    {@Override public final void run()
    {Process.setThreadPriority(PROCESS_PRIORITY); super.run();}};
    result.setDaemon(false); result.setPriority(THREAD_PRIORITY);
    return result;
  }
}

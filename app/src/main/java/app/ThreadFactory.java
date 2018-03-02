package app;

import android.os.Process;

import clean.Threader;

/**
 * Main Thread Factory.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/03/2018
 */
public final class ThreadFactory implements Threader.Factory {

  /** Global instance. */
  @SuppressWarnings("WeakerAccess")
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

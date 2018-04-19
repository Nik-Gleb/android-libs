package arch;

import javax.annotation.Nonnull;

/**
 * For pool thread factory.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 18/04/2018
 */
public interface JavaThreadFactory {

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
  @Nonnull Thread newThread(ThreadGroup group, Runnable target, String name, long stack);
}

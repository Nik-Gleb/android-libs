package clean.locks;

import com.google.common.util.concurrent.Striped;

import java.util.concurrent.locks.Lock;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/03/2018
 */
public final class MyLocks {

  private final Striped<Lock> striped  =  Striped.lazyWeakLock(2);

  public MyLocks() {
  }

  public void runAction(String key)
  {
    Lock lock =  striped.get(key);
    lock.lock();
    try
    {
      actionWithResource(key);
    }
    finally
    {
      lock.unlock();
    }
  }

  private void actionWithResource(Object obj)
  {
    // do something
  }
}

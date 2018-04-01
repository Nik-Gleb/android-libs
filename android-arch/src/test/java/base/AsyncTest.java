/*
 * AsyncTest.java
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

package base;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 09/08/2017
 */
@SuppressWarnings({ "WeakerAccess", "EmptyMethod", "RedundantThrows",
    "unused" })
@RunWith(MockitoJUnitRunner.class)
public class AsyncTest {

  /** @throws Exception by some issues */
  @Before public void setUp() throws Exception {
  }

  /** @throws Exception by some issues */
  @After public void tearDown() throws Exception {
  }

  /** @throws Exception by some issues */
  @SuppressWarnings("StatementWithEmptyBody")
  @Test public final void testInterruption() throws Exception {

    System.out.println(Thread.currentThread().getId() + " START: " + System.currentTimeMillis());
    final Thread threadExternal = new Thread(() -> {

      //noinspection unused
      /*try (final OnInterruptedListener listener = new OnInterruptedListener()) {
        final long time = System.currentTimeMillis();
        while (System.currentTimeMillis() - time < 2000)
          if (Thread.interrupted()) throw new InterruptedException();
      } catch (InterruptedException exception) {
        System.out.println(Thread.currentThread().getId() + " CATCH-0: " + System.currentTimeMillis());
        Thread.currentThread().interrupt();
        System.out.println(Thread.currentThread().getId() + " CATCH-1: " + System.currentTimeMillis());
      }*/

      System.out.println(Thread.currentThread().getId() + " FINALLY: " + System.currentTimeMillis());

    });
    threadExternal.start();
    System.out.println(Thread.currentThread().getId() + " STARTED: " + System.currentTimeMillis());

    final long delay = 1000;
    Thread.sleep(delay);

    System.out.println(Thread.currentThread().getId() + " INTERRUPT: " + System.currentTimeMillis());
    threadExternal.interrupt();
    System.out.println(Thread.currentThread().getId() + " INTERRUPTED: " + System.currentTimeMillis());

    /*

    START:        7.878 (main)
    STARTED:      7.930 (main)

    sleep (1000 ms)

    INTERRUPT:    8.931 (main)
    NOTIFY:       8.931 (main)

    notification (500 ms)

    NOTIFIED:     9.431 (main)
    INTERRUPTED:  9.431 (main)

    CATCH-0: 9.431 (worker)
    CATCH-1: 9.431 (worker)
    FINALLY: 9.431 (worker)

    */

  }

  /** @throws Exception by some issues */
  @SuppressWarnings("ConstantConditions")
  @Test public final void applyTest() throws Exception {


    final Object object = new Object();


   final Interactor interactor = new Interactor();

   /*final Threader threader =
       Threader.newParallel(null)
           .action(interactor::action2)
           .action(interactor::action1)
           .action(interactor::get, this::onResultReceived)
           .build();

   threader.apply(new AsyncT);

   threader.close();*/

    final String s = "";
  }


  @SuppressWarnings({ "SameReturnValue", "unused" })
  private static final class Interactor {

    String calc(int value)
    {return String.valueOf(value);}

    void action1(float value) {}
    void action2() {}

    long get() {return 0;}
  }

  private void onResultReceived(long value) {

  }

  private void onAction1Failed(Throwable value) {

  }

  private void onAction2Failed(Throwable value) {

  }

  private void onGetFailed(Throwable value) {

  }



  /*private static final class OnInterruptedListener
      implements Thread.OnInterruptedListener, Closeable {

    /** This instance. */
    //private final OnInterruptedListener mInstance = this;

    /* Async Thread. */
    //private final Thread mThread = getThread(mInstance);

    /* Calls by {@link java.lang.Thread#interrupt()} */
    /*@SuppressWarnings("StatementWithEmptyBody")
    @Override public final void onInterrupted() {
      System.out.println(Thread.currentThread().getId() + " NOTIFY: " + System.currentTimeMillis());
      final long time = System.currentTimeMillis();
      while (System.currentTimeMillis() - time < 500);
      System.out.println(Thread.currentThread().getId() + " NOTIFIED: " + System.currentTimeMillis());
    }*/

    /*{@inheritDoc}*/
    /*@Override public final void close()
    {if (mThread != null) mThread.setOnInterruptedListener(null);}*/

    /* @return async-thread */
    /*private static Thread getThread(Thread.OnInterruptedListener listener) {
      final java.lang.Thread origin = Thread.currentThread();
      final Thread result =  origin instanceof Thread ? (Thread) origin : null;
      if (result != null) result.setOnInterruptedListener(listener);
      return result;
    }
  }*/
}

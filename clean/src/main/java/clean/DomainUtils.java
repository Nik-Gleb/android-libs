/*
 * DomainUtils.java
 * clean
 *
 * Copyright (C) 2017, Gleb Nikitenko. All Rights Reserved.
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

import java.io.IOException;

import clean.cancellation.CancellationSignal;

/**
 * The Domain Utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/08/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class DomainUtils {

  /** The ok-object. */
  public static final Object OK = new Object();
  /** The fail-object. */
  public static final Object FAIL = null;

  /** The log cat tag. */
  private static final String TAG = "DomainUtils";

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private DomainUtils() {throw new AssertionError();}

  /**
   * Stub sleep.
   *
   * @param delay delay of timeout
   * @param signal cancellation signal
   */
  public static void stub(long delay, CancellationSignal signal) {
    final long time = System.currentTimeMillis();
    while (System.currentTimeMillis() - time < delay)
      signal.throwIfCanceled();
  }

  /**@throws IOException the test error */
  public static void fail() throws IOException
  {throw new IOException("Test Exception");}

  /**
   * @param object the response
   * @return true if positive response
   */
  public static boolean isOk(Object object) {return OK == object;}
}

/*
 * 	Utils.java
 * 	model
 *
 * 	Copyright (C) 2017, OmmyChat ltd. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of OmmyChat limited and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to OmmyChat limited and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from OmmyChat limited.
 */

package clean;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * The Main DomainUtils Class.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/08/2017
 */
public final class DomainUtils {

  /** The ok-object. */
  public static final Object OK = new Object();
  /** The fail-object. */
  public static final Object FAIL = null;

  /** The atomic sleep. */
  private static final long STUB_ATOMIC_SLEEP = 10L;
  /** The common sleep. */
  private static final long STUB_COMMON_SLEEP = 150L * 2;


  /** The log cat tag. */
  private static final String TAG = "DomainUtils";

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private DomainUtils() {throw new AssertionError();}

  /**
   * The stub sleep.
   *
   * @param signal the cancellation signal
   */
  public static void stub(@NonNull CancellationSignal signal) {

    for (int i = 0; i < STUB_COMMON_SLEEP; i++) {
      signal.throwIfCanceled();
      try {Thread.sleep(STUB_ATOMIC_SLEEP);}
      catch (InterruptedException exception)
      {exception.printStackTrace();}
    }
  }

  /**@throws IOException the test error */
  public static void fail() throws IOException
  {throw new IOException("Test Exception");}

  /**
   * @param object the response
   * @return true if positive response
   */
  public static boolean isOk(@Nullable Object object) {return OK == object;}
}

package android.support.v4.content;

import android.support.annotation.NonNull;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

/**
 * Common Loaders Utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 04/09/2017
 */
@Keep
@KeepPublicClassMembers
public final class LoadersUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private LoadersUtils() {
    throw new AssertionError();
  }

  /**
   * @param loader the loader instance
   * @return true loader doing some bg
   */
  @SuppressWarnings("unused")
  public static boolean isRunning(@NonNull AsyncTaskLoader loader)
  {return loader.mTask != null;}

}

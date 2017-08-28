package android.support.v4.app;

import android.content.Context;
import android.support.annotation.NonNull;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

/**
 * Common support tools
 *
 * @author Nikitenko Gleb
 * @since 1.0, 28/08/2017
 */
@Keep
@KeepPublicClassMembers
public final class SupportUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private SupportUtils() {
    throw new AssertionError();
  }

  /** @param fragmentManager the fragment manager for reset */
  @SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
  @NonNull public static String resetNoTransactionsBecause
  (@NonNull FragmentManager fragmentManager) {
    try { return((FragmentManagerImpl)fragmentManager).mNoTransactionsBecause;}
    finally {((FragmentManagerImpl)fragmentManager).mNoTransactionsBecause = null;}
  }

  /** @param loaderManager the loader manager for reset */
  @SuppressWarnings("UnusedReturnValue")
  @NonNull public static String resetNoTransactionsBecause
  (@NonNull LoaderManager loaderManager) {
    final LoaderManagerImpl loaderManagerImpl = ((LoaderManagerImpl)loaderManager);
    return resetNoTransactionsBecause(loaderManagerImpl.mHost.mFragmentManager);
  }

  /**
   * @param loaderManager the loader manager instance
   * @return the android application context
   */
  @SuppressWarnings("unused")
  @NonNull public static Context getContext(@NonNull LoaderManager loaderManager)
  {return ((LoaderManagerImpl)loaderManager).mHost.mContext;}

}

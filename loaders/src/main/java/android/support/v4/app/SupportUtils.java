/*
 * SupportUtils.java
 * loaders
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

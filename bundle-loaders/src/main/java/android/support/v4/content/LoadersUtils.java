/*
 * LoadersUtils.java
 * bundle-loaders
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

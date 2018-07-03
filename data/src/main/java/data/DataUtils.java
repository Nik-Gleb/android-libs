/*
 * DataUtils.java
 * data
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

package data;

import android.support.annotation.NonNull;

import java.nio.charset.StandardCharsets;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Common Tools
 *
 * @author Nikitenko Gleb
 * @since 1.0, 03/07/2018
 */
@Keep
@KeepPublicProtectedClassMembers
public final class DataUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private DataUtils() {throw new AssertionError();}


  /**
   * @param value primitive value
   *
   * @return byte array
   */
  @NonNull public static byte[] bytes(int value) {
    return new byte[] {
      (byte)(value >>> 24),
      (byte)(value >>> 16),
      (byte)(value >>> 8),
      (byte)value
    };
  }

  /**
   * @param bytes byte array
   *
   * @return byte array
   */
  @SuppressWarnings("PointlessBitwiseExpression")
  public static int toInt(@NonNull byte[] bytes) {
    return
      (bytes[0] & 0xFF) << 24 |
        (bytes[1] & 0xFF) << 16 |
        (bytes[2] & 0xFF) <<  8 |
        (bytes[3] & 0xFF) <<  0 ;
  }

  /**
   * @param value primitive value
   *
   * @return byte array
   */
  @NonNull public static byte[] bytes(@NonNull String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @param bytes byte array
   *
   * @return string
   */
  @NonNull public static String toStr(@NonNull byte[] bytes) {
    return new String(bytes, StandardCharsets.UTF_8);
  }

}

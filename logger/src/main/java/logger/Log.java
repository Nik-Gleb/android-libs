/*
 * Log.java
 * logger
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

package logger;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/** Common log-helper. */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("WeakerAccess, unused")
public final class Log {

  /** Check via Log.isLoggable */
  private static boolean sCheckTheTag = false;

  /** The log-mode. */
  @Mode
  private static int sLogMode = Mode.NONE;

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Log() {
    throw new AssertionError();
  }

  /**
   * Change current tag checking.
   *
   * @param value new flag
   */
  public static void checkTheTag(boolean value) {
    sCheckTheTag = value;
  }

  /**
   * Change current logging mode.
   *
   * @param level new log level
   *
   * @see Mode
   */
  public static void setLogLevel(@Mode int level) {
    sLogMode = level;
  }

  /**
   * Log verbose.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void v(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.VERBOSE;
    if (isTagLoggable(level, Mode.FULL, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log debug.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void d(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.DEBUG;
    if (isTagLoggable(level, Mode.NORMAL, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log info.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void i(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.INFO;
    if (isTagLoggable(level, Mode.NORMAL, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log warning.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void w(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.WARN;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log warning.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   * @param tr Error to serialize in log
   */
  public static void w(@NonNull String tag, @NonNull String msg,
      @NonNull Throwable tr) {
    @Level final int level = android.util.Log.WARN;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, getStackTraceString(msg, tr));
    }
  }

  /**
   * Log error.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void e(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.ERROR;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log error.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   * @param tr Error to serialize in log
   */
  public static void e(@NonNull String tag, @NonNull String msg,
      @NonNull Throwable tr) {
    @Level final int level = android.util.Log.ERROR;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, getStackTraceString(msg, tr));
    }
  }

  /**
   * Log assert.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   */
  public static void a(@NonNull String tag, @NonNull String msg) {
    @Level final int level = android.util.Log.ASSERT;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, msg);
    }
  }

  /**
   * Log assert.
   *
   * @param tag Tag for this log
   * @param msg Msg for this log
   * @param tr Error to serialize in log
   */
  public static void a(@NonNull String tag, @NonNull String msg,
      @NonNull Throwable tr) {
    @Level final int level = android.util.Log.ASSERT;
    if (isTagLoggable(level, Mode.BASE, tag)) {
      android.util.Log.println(level, tag, getStackTraceString(msg, tr));
    }
  }

  /**
   * @param level log level
   * @param mode log mode
   * @param tag log tag
   *
   * @return true if log allowed
   */
  private static boolean isTagLoggable(@Level int level, @Mode int mode,
      @NonNull String tag) {
    return sLogMode > Mode.NONE &&
        (sLogMode == Mode.FULL || sLogMode >= mode) &&
        (!sCheckTheTag || android.util.Log.isLoggable(tag, level));
  }

  /**
   * @param msg Msg for this log
   * @param tr Error to serialize in log
   *
   * @return concatenated string
   */
  @NonNull
  private static String getStackTraceString(@NonNull String msg,
      @NonNull Throwable tr) {
    final StringBuilder stringBuilder = new StringBuilder(msg)
        .append(System.lineSeparator())
        .append(android.util.Log.getStackTraceString(tr));
    final String result = stringBuilder.toString();
    stringBuilder.setLength(0);
    return result;
  }

  /**
   * Predefined log-level codes.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @IntDef({

            /* ===  FULL LOGGING  === */
      android.util.Log.VERBOSE,

            /* === NORMAL LOGGING === */
      android.util.Log.DEBUG,
      android.util.Log.INFO,

            /* ===  BASE LOGGING  === */
      android.util.Log.WARN,
      android.util.Log.ERROR,
      android.util.Log.ASSERT,

            /* ===   NO LOGGING   === */

  })
  @Retention(RetentionPolicy.SOURCE)
  @SuppressWarnings("WeakerAccess, unused")
  private @interface Level {

  }

  /**
   * Predefined log-mode codes.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @Keep
  @KeepPublicProtectedClassMembers
  @IntDef({ Mode.NONE, Mode.BASE, Mode.NORMAL, Mode.FULL })
  @Retention(RetentionPolicy.SOURCE)
  @SuppressWarnings("WeakerAccess, unused")
  public @interface Mode {

    int NONE = 0, BASE = 1, NORMAL = 2, FULL = 3;
  }

}

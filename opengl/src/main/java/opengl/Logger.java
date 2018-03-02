/*
 * Logger.java
 * opengl
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

package opengl;

import android.annotation.TargetApi;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Formatter;
import java.util.Locale;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.util.Log.v;
import static opengl.OpenGL.isKitKat;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 15/12/2017
 */
final class Logger {

  /** The log-cat tag. */
  private static final String TAG = "OpenGL";

  /** The "Line-Separator" character. */
  static final String LS = System.getProperty("line.separator");

  /** The logs string builder. */
  private static final StringBuilder STRING_BUILDER = new StringBuilder();

  /** Logs formatter. */
  private static final Formatter FORMATTER =
      Log.isLoggable(TAG, Log.VERBOSE) ?
      new Formatter(STRING_BUILDER) : null;

  /**
   * @param object EGL object
   * @param pattern string pattern
   */
  static <T> T log(@NonNull T object, @NonNull String pattern) {
    if (FORMATTER != null)
      synchronized (FORMATTER) {
        FORMATTER.format(pattern, getHandle(object));
        v(TAG, STRING_BUILDER.toString());
        STRING_BUILDER.setLength(0);
      }
    return object;
  }

  /** Writes the current display, context, and surface to the log. */
  @SuppressWarnings("unused")
  static void log(@NonNull String pattern) {
    final long display = getHandle(EGL14.eglGetCurrentDisplay());
    final long context = getHandle(EGL14.eglGetCurrentContext());
    final long surfaceRead = getHandle(EGL14.eglGetCurrentSurface(EGL14.EGL_READ));
    final long surfaceWrite = getHandle(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW));
    if (FORMATTER == null) return;
    synchronized (FORMATTER) {
      FORMATTER.format(pattern, display, context, surfaceRead, surfaceWrite);
      v(TAG, FORMATTER.toString()); STRING_BUILDER.setLength(0);
    }
  }

  /**
   * @param object EGL object
   * @return the EGL context handle
   */
  static <T> long getHandle(@NonNull T object) {
    if (isKitKat()) return getHandleBase(object);
    else return getHandleLollipop(object);
  }

  /**
   * @param obj EGL object
   * @return the EGL context handle
   */
  @SuppressWarnings("deprecation")
  private static <T> long getHandleBase(@NonNull T obj) {
    return
        obj instanceof EGLContext ? ((EGLContext)obj).getHandle() :
            obj instanceof EGLConfig ? ((EGLConfig)obj).getHandle() :
                obj instanceof EGLDisplay ? ((EGLDisplay)obj).getHandle() :
                    obj instanceof EGLSurface ? ((EGLSurface)obj).getHandle() :
                        -1;
  }

  /**
   * @param obj EGL object
   * @return the EGL context handle
   */
  @TargetApi(LOLLIPOP)
  private static <T> long getHandleLollipop(@NonNull T obj) {
    return
        obj instanceof EGLContext ? ((EGLContext)obj).getNativeHandle() :
            obj instanceof EGLConfig ? ((EGLConfig)obj).getNativeHandle() :
                obj instanceof EGLDisplay ? ((EGLDisplay)obj).getNativeHandle() :
                    obj instanceof EGLSurface ? ((EGLSurface)obj).getNativeHandle() :
                        -1;
  }

  @SuppressWarnings("unused")
  static void config(@NonNull EGLDisplay display, @NonNull EGLConfig config) {
    final int[] attrs = {
        EGL14.EGL_BUFFER_SIZE,
        EGL14.EGL_ALPHA_SIZE,
        EGL14.EGL_BLUE_SIZE,
        EGL14.EGL_GREEN_SIZE,
        EGL14.EGL_RED_SIZE,
        EGL14.EGL_DEPTH_SIZE,
        EGL14.EGL_STENCIL_SIZE,
        EGL14.EGL_CONFIG_CAVEAT,
        EGL14.EGL_CONFIG_ID,
        EGL14.EGL_LEVEL,
        EGL14.EGL_MAX_PBUFFER_HEIGHT,
        EGL14.EGL_MAX_PBUFFER_PIXELS,
        EGL14.EGL_MAX_PBUFFER_WIDTH,
        EGL14.EGL_NATIVE_RENDERABLE,
        EGL14.EGL_NATIVE_VISUAL_ID,
        EGL14.EGL_NATIVE_VISUAL_TYPE,
        //0x3030, // EGL14.EGL_PRESERVED_RESOURCES,
        EGL14.EGL_SAMPLES,
        EGL14.EGL_SAMPLE_BUFFERS,
        EGL14.EGL_SURFACE_TYPE,
        EGL14.EGL_TRANSPARENT_TYPE,
        EGL14.EGL_TRANSPARENT_RED_VALUE,
        EGL14.EGL_TRANSPARENT_GREEN_VALUE,
        EGL14.EGL_TRANSPARENT_BLUE_VALUE,
        EGL14.EGL_BIND_TO_TEXTURE_RGB,
        EGL14.EGL_BIND_TO_TEXTURE_RGBA,
        EGL14.EGL_MIN_SWAP_INTERVAL,
        EGL14.EGL_MAX_SWAP_INTERVAL,
        EGL14.EGL_LUMINANCE_SIZE,
        EGL14.EGL_ALPHA_MASK_SIZE,
        EGL14.EGL_COLOR_BUFFER_TYPE,
        EGL14.EGL_RENDERABLE_TYPE,
        EGL14.EGL_CONFORMANT,
        0x3142 // EGL_RECORDABLE_ANDROID
    };
    final String[] names = {
        "EGL_BUFFER_SIZE",
        "EGL_ALPHA_SIZE",
        "EGL_BLUE_SIZE",
        "EGL_GREEN_SIZE",
        "EGL_RED_SIZE",
        "EGL_DEPTH_SIZE",
        "EGL_STENCIL_SIZE",
        "EGL_CONFIG_CAVEAT",
        "EGL_CONFIG_ID",
        "EGL_LEVEL",
        "EGL_MAX_PBUFFER_HEIGHT",
        "EGL_MAX_PBUFFER_PIXELS",
        "EGL_MAX_PBUFFER_WIDTH",
        "EGL_NATIVE_RENDERABLE",
        "EGL_NATIVE_VISUAL_ID",
        "EGL_NATIVE_VISUAL_TYPE",
        //"EGL_PRESERVED_RESOURCES",
        "EGL_SAMPLES",
        "EGL_SAMPLE_BUFFERS",
        "EGL_SURFACE_TYPE",
        "EGL_TRANSPARENT_TYPE",
        "EGL_TRANSPARENT_RED_VALUE",
        "EGL_TRANSPARENT_GREEN_VALUE",
        "EGL_TRANSPARENT_BLUE_VALUE",
        "EGL_BIND_TO_TEXTURE_RGB",
        "EGL_BIND_TO_TEXTURE_RGBA",
        "EGL_MIN_SWAP_INTERVAL",
        "EGL_MAX_SWAP_INTERVAL",
        "EGL_LUMINANCE_SIZE",
        "EGL_ALPHA_MASK_SIZE",
        "EGL_COLOR_BUFFER_TYPE",
        "EGL_RENDERABLE_TYPE",
        "EGL_CONFORMANT",
        "EGL_RECORDABLE_ANDROID"
    };

    final String
        confFormat = "Configuration %d:",
        okFormat = "\t%s: %d\n",
        failFormat = "\t%s: failed\n";

    Log.v(TAG, String.format(Locale.US, confFormat, getHandle(config)));
    for (int i = 0; i < attrs.length; i++)
      if (EGL14.eglGetConfigAttrib(display, config, attrs[i], attrs, i))
        Log.v(TAG, String.format(Locale.US, okFormat , names[i], attrs[i]));
      else Log.w(TAG, String.format(Locale.US, failFormat, names[i]));

    Log.v(TAG, "\n");

  }

}

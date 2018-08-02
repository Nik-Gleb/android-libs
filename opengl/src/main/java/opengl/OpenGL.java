/*
 * OpenGL.java
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

import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;
import static opengl.Logger.LS;
import static opengl.OpenGL.GLVersion.GL2;
import static opengl.OpenGL.GLVersion.GL3;

/**
 * Common utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/12/2017
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class OpenGL {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private OpenGL() {throw new AssertionError();}

  /** @return true if current sdk-ver is KitKat */
  @SuppressWarnings("SameReturnValue")
  static boolean isKitKat() {return false;}

  /** @return true if current sdk-ver is Oreo */
  static boolean isOreo() {return SDK_INT >= O;}

  /**
   * Create a {@link EGLCore} without DEPTH and STENCIL.
   *
   * @param fmt the base color format
   *
   * @return a {@link EGLCore} instance
   */
  @NonNull public static EGLCore flat(@NonNull ColorFormat fmt)
  {return new EGLCore.Builder(fmt.red, fmt.green, fmt.blue, fmt.alpha, fmt.nId).build();}

  /**
   * Create a {@link EGLCore} with DEPTH(24) and STENCIL(8).
   *
   * @param fmt the base color format
   *
   * @return a {@link EGLCore} instance
   */
  @SuppressWarnings("WeakerAccess")
  @NonNull public static EGLCore depth(@NonNull ColorFormat fmt)
  {return new EGLCore.Builder(fmt.red, fmt.green, fmt.blue, fmt.alpha, fmt.nId).d24s8().build();}

  /** @return graph for information, then formats it all into one giant str. */
  @NonNull public static String info(@NonNull ColorFormat format) {
    final StringBuilder builder = new StringBuilder();
    final int width = 1, height = 1; boolean release = false;
    final EGLCore eglCore = flat(format);
    @SuppressWarnings("ConstantConditions")
    final EGLView surface = EGLView.offScreen(eglCore, width, height, release);
    surface.makeCurrent();
    EGLCore.info(eglCore.eglDisplay, builder); GLESUtils.info(builder);
    info(builder); surface.close(); eglCore.close();
    try {return builder.toString();}
    finally {builder.setLength(0);}
  }

  /**
   * Vendor info.
   *
   * @param builder the {@link StringBuilder} instance
   */
  private static void info(@NonNull StringBuilder builder) {
    builder
        .append(LS).append("===== System Information =====")
        .append(LS).append("mfgr      : ").append(Build.MANUFACTURER)
        .append(LS).append("brand     : ").append(Build.BRAND)
        .append(LS).append("model     : ").append(Build.MODEL)
        .append(LS).append("build     : ").append(Build.DISPLAY)
        .append(LS).append("release   : ").append(Build.VERSION.RELEASE)
        .append(LS);
  }


  /**
   * Formats the extensions string, which is a space-separated list, into a
   * series of indented values followed by newlines.
   *
   * The list is sorted.
   *
   * @param extensions the extensions string
   */
  @NonNull
  static String format(@NonNull String extensions) {
    final String space = " ";
    final StringBuilder builder = new StringBuilder();
    final String[] values = extensions.split(space);
    Arrays.sort(values);
    for (final String value : values)
      builder.append(space).append(value).append(LS);
    try {return builder.toString();}
    finally {builder.setLength(0);}
  }

  /**
   * Saves the EGL surface to a shorts.
   * <p>
   * Expects that this object's EGL surface is current.
   *
   * @param view the egl-view
   * @param format the color format
   *
   * @return the frame shorts
   */
  @NonNull
  static short[] getRGB565(@NonNull EGLView view, @NonNull ColorFormat format) {
    if (!view.isCurrent()) EGLCore.error("Expected EGL context/surface is not current");
    final int w = view.width, h = view.height, bpp = format.bytesPerPixel,
        f = format.glPixelFormat; final String message = "Can't read pixels";
    final int type = format.glPixelType, x = 0, y = 0;

    /*final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(w * h * 2);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN); view.makeCurrent();
    GLES20.glReadPixels(x, y, w, h, f, type, byteBuffer); byteBuffer.rewind();
    if (EGL14.eglGetError() != EGL14.EGL_SUCCESS) EGLCore.error(message);
    final ShortBuffer shortBuffer = byteBuffer.asShortBuffer();*/

    final ShortBuffer shortBuffer = ShortBuffer.allocate(w * h);
    view.makeCurrent(); GLES20.glReadPixels(x, y, w, h, f, type, shortBuffer);
    if (EGL14.eglGetError() != EGL14.EGL_SUCCESS) EGLCore.error(message);
    shortBuffer.rewind(); final short[] shorts = new short[shortBuffer.limit()];
    shortBuffer.get(shorts, shortBuffer.position(), shorts.length);
    try {return shorts;} finally {shortBuffer.clear();}
  }

  /**
   * Saves the EGL surface to a bytes.
   * <p>
   * Expects that this object's EGL surface is current.
   *
   * @param view the egl-view
   * @param format the color format
   *
   * @return the frame bytes
   */
  @NonNull
  public static byte[] getRGBA888(@NonNull EGLView view, @NonNull ColorFormat format) {
    if (!view.isCurrent()) EGLCore.error("Expected EGL context/surface is not current");
    final int w = view.width, h = view.height, bpp = format.bytesPerPixel,
        f = format.glPixelFormat; final String message = "Can't read pixels";
    final int type = format.glPixelType, x = 0, y = 0;
    final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(w * h * bpp);
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN); view.makeCurrent();
    GLES20.glReadPixels(x, y, w, h, f, type, byteBuffer);
    if (EGL14.eglGetError() != EGL14.EGL_SUCCESS) EGLCore.error(message);
    byteBuffer.rewind(); final byte[] result = new byte[byteBuffer.limit()];
    byteBuffer.get(result, byteBuffer.position(), result.length);
    byteBuffer.clear(); return result;
  }


  /**
   * Predefined GL-Versions.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @IntDef({GL2, GL3})
  @Retention(RetentionPolicy.SOURCE)
  @interface GLVersion {
    /** GL-2. */
    int GL2 = 2;
    /** GL-3. */
    int GL3 = 3;
  }
}

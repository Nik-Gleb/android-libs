/*
 * GLESUtils.java
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

import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;

import static opengl.Logger.LS;

/**
 * Common GLES-Specific utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/12/2017
 */
@SuppressWarnings("WeakerAccess")
public final class GLESUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private GLESUtils() {throw new AssertionError();}

  /**
   * GLES for information, then formats it all into one giant str.
   *
   * @param builder the {@link StringBuilder} instance
   */
  static void info(@NonNull StringBuilder builder) {
    builder
        .append(LS).append("===== GL Information =====")
        .append(LS).append("vendor    : ")
        .append(GLES20.glGetString(GLES20.GL_VENDOR))
        .append(LS).append("version   : ")
        .append(GLES20.glGetString(GLES20.GL_VERSION))
        .append(LS).append("renderer  : ")
        .append(GLES20.glGetString(GLES20.GL_RENDERER))
        .append(LS).append("extensions:").append(LS)
        .append(OpenGL.format(GLES20.glGetString(GLES20.GL_EXTENSIONS)));
  }

  static void checkError() {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
      Log.e("ERROR", String.valueOf(error));

  }
}

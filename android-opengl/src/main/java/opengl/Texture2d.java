/*
 * Texture2d.java
 * bundle-opengl
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

import java.nio.Buffer;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 12/01/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Texture2d {

  /** Const integers. */
  private static final int OFFSET = 0, LEVEL = 0, BORDER = 0;

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Texture2d() {throw new AssertionError();}

  /**
   *
   * @param targets array of necessary targets
   * @return array of texture names
   */
  @NonNull public static int[] createTextures
  (@NonNull boolean[] targets, @NonNull int[] temp) {
    final int[] result = new int[targets.length];
    GLES20.glGenTextures(result.length, result, OFFSET);
    for (int i = 0; i < result.length; i++) {
      makeCurrent(result, targets, i, temp); final int target = temp[1];
      GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
      GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
      GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
      GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }
    return result;
  }

  /** @param textures for close */
  public static void closeTextures(@NonNull int... textures)
  {GLES20.glDeleteTextures(textures.length, textures, OFFSET);}

  /**
   * @param names the texture names
   * @param targets the texture targets
   * @param idx the texture ids
   * @param width the horizontal size
   * @param height the vertical size
   * @param fmt the format
   * @param type the texture type
   * @param buf the buffer
   */
  public static void bindTexture(@NonNull int[] names, @NonNull boolean[] targets, int idx,
      int width, int height, int fmt, int type, @NonNull Buffer buf, @NonNull int[] temp)
  {makeCurrent(names, targets, idx, temp); GLES20.glTexImage2D(temp[1], LEVEL, fmt, width, height, BORDER, fmt, type, buf);}

  /**
   * @param external flag about external texture
   * @return gl-compatibility target
   */
  private static int target(boolean external)
  //GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
  {return external ? 0x8D65 : GLES20.GL_TEXTURE_2D;}

  /**
   * @param names the texture names
   * @param targets the texture targets
   * @param index the current index
   * @param result the results
   */
  private static void makeCurrent
  (@NonNull int[] names, @NonNull boolean[] targets, int index, @NonNull int[] result) {
    GLES20.glActiveTexture(result[0] = GLES20.GL_TEXTURE0 + index);
    GLES20.glBindTexture(result[1] = target(targets[index]), result[2] = names[index]);
  }
}

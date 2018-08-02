/*
 * Program2d.java
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
import android.opengl.Matrix;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 14/12/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Program2d {

  /** Shader str-constants. */
  private static final String
      V_POSITION = "aPosition", V_COORDINATE = "aCoordinate",
      V_MVP_MATRIX = "uMVPMatrix", V_ST_MATRIX = "uSTMatrix",
      F_COORDINATE = "vCoordinate", F_TEXTURE = "sTexture";

  /** The vertex shader. */
  private static final String VERTEX =
      "uniform mat4 " + V_MVP_MATRIX + ";\n" +
          "uniform mat4 " + V_ST_MATRIX + ";\n" +
          "attribute vec4 " + V_POSITION + ";\n" +
          "attribute vec4 " + V_COORDINATE + ";\n" +
          "varying vec2 " + F_COORDINATE + ";\n" +
          "void main() {\n" +
          "  gl_Position = " + V_MVP_MATRIX + " * " + V_POSITION + ";\n" +
          "  " + F_COORDINATE + " = (" + V_ST_MATRIX + " * " + V_COORDINATE + ").xy;\n" +
          "}\n";

  /** The 2D-Texture prefix. */
  private static final String PREFIX_2D =
      "uniform sampler2D " + F_TEXTURE + ";\n";

  /** The EXT-Texture prefix. */
  private static final String PREFIX_EXT =
      "#extension GL_OES_EGL_image_external : require\n" +
          "uniform samplerExternalOES " + F_TEXTURE + ";\n";

  /** Simple fragment shader for use with "normal" 2D textures.*/
  private static final String FRAGMENT =
      "precision mediump float;\n" +
          "varying vec2 " + F_COORDINATE + ";\n" +
          "void main() {\n" +
          "    gl_FragColor = texture2D(" + F_TEXTURE + ", " + F_COORDINATE + ");\n" +
          "}\n";

  /** Integer constants. */
  private static final int
      FLOAT_SIZE_BYTES = 4,
      VERTICES_STRIDE_BYTES =
          5 * FLOAT_SIZE_BYTES,
      VERTICES_POS_OFFSET = 0,
      VERTICES_UV_OFFSET = 3,
      VERTICES_NUMBER = 4,
      OFFSET = 0;

  /** Triangle vertices. */
  private static final FloatBuffer TRIANGLE_VERTICES = createVertices();

  /* The mvp-matrix. */
  //private static final float[] MVP_MATRIX = createIdentityMatrix();

  /** Const booleans. */
  private static final boolean NORMALIZE = false, TRANSPOSE = false;

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Program2d() {throw new AssertionError();}

  /**
   * Creates a new PROGRAM with linked shaders
   *
   * @param external use external shader
   * @return array of int attributes
   */
  @NonNull public static int[] createProgram(boolean external) {

    /*
     * 0 - Program
     * 1 - Vertex Shader
     * 2 - Fragment Shader
     * 3 - Position Handle
     * 4 - Texture Handle
     * 5 - MVP Matrix Handle
     * 6 - ST Matrix Handle
     * 7 - Texture sampler
     * 8 - Current Program
     * 9 - Temp counter
     **/

    final String prefix =
        external ? PREFIX_EXT : PREFIX_2D,
        fragment = prefix + FRAGMENT;
    final int[] result = new int[10];
    result[0] = GLES20.glCreateProgram();
    result[1] = GLES20.glCreateShader
        (GLES20.GL_VERTEX_SHADER);
    GLES20.glShaderSource(result[1], VERTEX);
    GLES20.glCompileShader(result[1]);
    result[2] = GLES20.glCreateShader
        (GLES20.GL_FRAGMENT_SHADER);
    GLES20.glShaderSource(result[2], fragment);
    GLES20.glCompileShader(result[2]);
    GLES20.glReleaseShaderCompiler();
    GLES20.glAttachShader(result[0], result[1]);
    GLES20.glAttachShader(result[0], result[2]);
    GLES20.glLinkProgram(result[0]);

    saveProgram(result);
    result[3] = GLES20.glGetAttribLocation(result[0], V_POSITION);
    result[4] = GLES20.glGetAttribLocation(result[0], V_COORDINATE);
    result[5] = GLES20.glGetUniformLocation(result[0], V_MVP_MATRIX);
    result[6] = GLES20.glGetUniformLocation(result[0], V_ST_MATRIX);
    result[7] = GLES20.glGetUniformLocation(result[0], F_TEXTURE);
    restoreProgram(result);
    // 
    return result;
  }

  /**
   * Close program.
   *
   * @param attributes the attributes
   */
  public static void closeProgram(@NonNull int[] attributes) {
    GLES20.glDetachShader(attributes[0], attributes[1]);
    GLES20.glDeleteShader(attributes[1]);
    GLES20.glDetachShader(attributes[0], attributes[2]);
    GLES20.glDeleteShader(attributes[2]);
    GLES20.glDeleteProgram(attributes[0]);
  }

  private static void restoreProgram(int[] attrs)
  {GLES20.glUseProgram(attrs[8]);}

  private static void saveProgram(int[] attrs) {
    final int current = 8, my = 0;
    GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, attrs, current);
    GLES20.glUseProgram(attrs[my]);
  }

  /**
   * Draw current state.
   *
   * @param attrs    program attributes
   * @param stMatrix transform matrix
   * @param unit     current unit
   */
  public static void draw
  (@NonNull int[] attrs, @NonNull float[] stMatrix,  @NonNull float[] mvpMatrix, int unit) {

    saveProgram(attrs);

    attrs[9] = 3;
    TRIANGLE_VERTICES.position(VERTICES_POS_OFFSET);
    GLES20.glVertexAttribPointer(attrs[3], attrs[9], GLES20.GL_FLOAT,
        NORMALIZE, VERTICES_STRIDE_BYTES, TRIANGLE_VERTICES);

    attrs[9] --;
    TRIANGLE_VERTICES.position(VERTICES_UV_OFFSET);
    GLES20.glVertexAttribPointer(attrs[4], attrs[9], GLES20.GL_FLOAT,
        NORMALIZE, VERTICES_STRIDE_BYTES, TRIANGLE_VERTICES);

    attrs[9] --;
    GLES20.glUniformMatrix4fv(attrs[5], attrs[9], TRANSPOSE, mvpMatrix, OFFSET);
    GLES20.glUniformMatrix4fv(attrs[6], attrs[9], TRANSPOSE, stMatrix, OFFSET);
    GLES20.glUniform1i(attrs[7], unit);

    GLES20.glEnableVertexAttribArray(attrs[3]);
    GLES20.glEnableVertexAttribArray(attrs[4]);
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, OFFSET, VERTICES_NUMBER);
    GLES20.glFinish();
    GLES20.glDisableVertexAttribArray(attrs[3]);
    GLES20.glDisableVertexAttribArray(attrs[4]);

    restoreProgram(attrs);
  }

  /**
   * Creates triangle vertices buffer.
   *
   * @return the triangle vertices buffer
   */
  private static FloatBuffer createVertices() {
    final float[] vertices = {
        // X, Y, Z, U, V
        -1.0f, -1.0f, 0, 0.f, 0.f,
         1.0f, -1.0f, 0, 1.f, 0.f,
        -1.0f,  1.0f, 0, 0.f, 1.f,
         1.0f,  1.0f, 0, 1.f, 1.f,
    };
    final FloatBuffer result =
        ByteBuffer.allocateDirect(vertices.length * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
    result.put(vertices).position(0);
    return result;
  }

  /** @return identity matrix. */
  @NonNull public static float[] createIdentityMatrix() {
    final float[] result = new float[16];
    Matrix.setIdentityM (result, OFFSET);
    return result;
  }
}

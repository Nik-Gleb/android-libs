/*
 * TextureRenderer.java
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

import android.graphics.Rect;
import android.opengl.GLES20;

import java.io.Closeable;

import static java.lang.Math.round;

/**
 * Texture Renderer.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/04/2018
 */
@SuppressWarnings("unused")
public final class TextureRenderer implements Closeable {

  /** Clear mask flags. */
  @SuppressWarnings("WeakerAccess")
  public static final int CLEAR_MASK =
      GLES20.GL_COLOR_BUFFER_BIT |
          GLES20.GL_DEPTH_BUFFER_BIT |
          GLES20.GL_STENCIL_BUFFER_BIT;

  /** Texture index. */
  private final int INDEX = 0;

  /** Texture targets. */
  private final boolean[] mTargets = {true};

  /** Texture matrix. */
  @SuppressWarnings("WeakerAccess")
  public final float[]
    stMatrix = Program2d.createIdentityMatrix(),
    mvpMatrix = Program2d.createIdentityMatrix();

  /** Internal attributes. */
  private final int[] mTemp = new int[3],
      mTextures = Texture2d.createTextures(mTargets, mTemp),
      mProgram = Program2d.createProgram(mTargets[INDEX]);

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    Program2d.closeProgram(mProgram);
    Texture2d.closeTextures(mTextures);
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** Draw current frame. */
  public final void draw()
  {Program2d.draw(mProgram, stMatrix, mvpMatrix, INDEX);}

  /** @return render texture id */
  public final int getTextureId() {return mTextures[INDEX];}

  /**
   * Clean scene.
   *
   * @param width horizontal size of scene
   * @param height vertical size of scene
   */
  public static void clean(int width, int height) {
    final float fZero = 0.0f; final int iZero = 0;
    GLES20.glClearColor(fZero, fZero, fZero, fZero);
    GLES20.glClearDepthf(iZero); GLES20.glClearStencil(iZero);
    GLES20.glViewport(iZero, iZero, width, height);
    GLES20.glScissor(iZero, iZero, width, height);
    GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
    GLES20.glClear(CLEAR_MASK);
  }

  /**
   * Crop output rectangles.
   *
   * @param ws    width of source
   * @param hs    height of source
   * @param wd    width of destination
   * @param hd    height of destination
   *
   * @param fit   true - for "fit-inside",
   *              false - for "center-crop"
   *
   * @param rect  target rectangle
   */
  public static void crop
  (int ws, int hs, int wd, int hd, boolean fit, Rect rect, boolean land) {

    if (land) {ws+=hs; hs=ws-hs; ws-=hs;}

    float
        rwhs = (float)ws/(float)hs,
        rhws = (float)hs/(float)ws,
        rwhd = (float)wd/(float)hd,
        rhwd = (float)hd/(float)wd;

    int t, b, l, r;

    if (fit)
      if (rwhs > rwhd) {
        t = 0; b = hd; l = round(wd * 0.5f); r = round(b * rwhs);
        l -= round(r * 0.5f); r += l;
      } else {
        l = 0; r = wd; t = round(hd * 0.5f); b = round(r * rhws);
        t -= round(b * 0.5f); b += t;
      }
    else
      if (rhws < rhwd) {
        l = 0; r = wd; t = round(hd * 0.5f); b = round(r * rhws);
        t -= round(b * 0.5f); b += t;
      } else {
        t = 0; b = hd; l = round(wd * 0.5f); r = round(b * rwhs);
        l -= round(r * 0.5f); r += l;
      }

    rect.set(l, t, r, b);
  }
}

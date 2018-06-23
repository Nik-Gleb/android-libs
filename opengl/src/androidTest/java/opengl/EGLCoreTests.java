/*
 * EGLCoreTests.java
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.Surface;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * All tests set.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 17/12/2017
 */
@RunWith(Suite.class)
@Suite.SuiteClasses ({
    //EGLCoreTests.EGLCoreOpaqueTest.class,     // Supported only on avd's
    EGLCoreTests.EGLCoreTransparentTest.class
})
public final class EGLCoreTests {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private EGLCoreTests() {throw new AssertionError();}

  /* Opaque test mode. */
  @RunWith(AndroidJUnit4.class)
  public static final class EGLCoreOpaqueTest extends EGLCoreTest
  {@Override @NonNull
  public final ColorFormat getTestColorFormat()
  {return ColorFormat.OPAQUE;}}

  /** Transparent test mode. */
  @RunWith(AndroidJUnit4.class)
  public static final class EGLCoreTransparentTest extends EGLCoreTest
  {@Override @NonNull public final ColorFormat getTestColorFormat()
  {return ColorFormat.TRANSPARENT;}}

  /** The egl-core test. */
  @SuppressWarnings("unused")
  static abstract class EGLCoreTest implements ColorFormatTest {

    /** "DRAW"-Message. */
    private static final int MSG_DRAW = 0;

    /** The GL clear-mask. */
    private static final int CLEAR_MASK =
        GLES20.GL_COLOR_BUFFER_BIT |
            GLES20.GL_DEPTH_BUFFER_BIT |
            GLES20.GL_STENCIL_BUFFER_BIT;

    /** The test callback. */
    private final TestCallback mTestCallback = new TestCallback();

    @SuppressWarnings("EmptyMethod")
    @BeforeClass
    public static void setUpClass() {}
    @SuppressWarnings("EmptyMethod")
    @AfterClass
    public static void tearDownClass() {}


    /** The log-cat tag. */
    @SuppressWarnings("unused")
    private static final String TAG = "EGLCoreTest";


    /** Application context. */
    @SuppressWarnings("unused")
    @Nullable private Context mContext = null;
    /** The current color format. */
    @Nullable private ColorFormat mColorFormat = null;
    /** Main thread handler. */
    @Nullable private Handler mHandler = null;

    @Before
    public final void setUpTest() {
      mContext = InstrumentationRegistry.getContext();
      mColorFormat = getTestColorFormat();
      mHandler = new Handler(Looper.getMainLooper(), mTestCallback);
    }

    @After
    public final void tearDownTest() {
      final Object token = null;
      Objects.requireNonNull(mHandler)
          .removeCallbacksAndMessages(token);
      mHandler = null;
      mColorFormat = null;
      mContext = null;
    }

    /**
     * Test for {@link EGLCore.Builder#d24()}
     *
     */
    //@Test
    public final void testEGLCore() {
      final int width = 0, height = 0; final boolean release = false;
      final EGLCore core = OpenGL.depth(Objects.requireNonNull(mColorFormat));
      EGLView.offScreen(core, width, height, release).close(); core.close();
    }

    /**
     * Test for {@link OpenGL#info(ColorFormat)}
     */
    //@Test
    public final void testInfo() {
      Log.d(TAG, OpenGL.info(Objects.requireNonNull(mColorFormat)));
    }

    /**
     * Test for {@link OpenGL#info(ColorFormat)}
     */
    @Test
    public final void testTexture() {
      final Context context = Objects.requireNonNull(mContext);
      final ColorFormat format = Objects.requireNonNull(mColorFormat);
      final Bitmap bmp = TestsUtils.createTestBitmap(mColorFormat.bmpConfig);
      final int w = bmp.getWidth(), h = bmp.getHeight();
      final short[] shorts = mColorFormat == ColorFormat.OPAQUE ?
          TestsUtils.bitmapShorts(bmp) : null;
      final byte[] bytes = mColorFormat == ColorFormat.OPAQUE ?
          null : TestsUtils.bitmapBytes(bmp);
      final boolean r = false; final EGLCore c = OpenGL.flat(format);
      final EGLView v = EGLView.offScreen(c, w, h, r),
          v1 = EGLView.offScreen(c, w, h, r),
          v2 = EGLView.offScreen(c, w, h, r);

      v.makeCurrent();
      final TextureRender2 render = new TextureRender2();
      render.surfaceCreated();

      final SurfaceTexture surfaceTexture = new SurfaceTexture(render.getTextureId());
      surfaceTexture.setDefaultBufferSize(w, h);
      surfaceTexture.setOnFrameAvailableListener
          (surfaceTexture1 -> TestsUtils.setResult(true), mHandler);
      final Surface surface = new Surface(surfaceTexture);

      final Object[] params = {surface, bmp};
      Message.obtain(mHandler, MSG_DRAW, params).sendToTarget();
      TestsUtils.waitAndCheck(); surfaceTexture.updateTexImage();

      v.makeCurrent();
      GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
      GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
      GLES20.glViewport(0, 0, w, h);
      GLES20.glScissor(0, 0, w, h);
      GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
      GLES20.glClear(CLEAR_MASK);
      render.drawFrame(surfaceTexture);
      v.swapBuffers(0);
      if (mColorFormat == ColorFormat.OPAQUE)
        //noinspection ConstantConditions
        TestsUtils.checkFrame(v, format, shorts, context);
      else //noinspection ConstantConditions
        TestsUtils.checkFrame(v, format, bytes, context);
      GLES20.glDisable(GLES20.GL_SCISSOR_TEST);


      if (c.getVersion() >= 3) {

        v1.makeCurrentReadFrom(v);
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
        GLES20.glViewport(0, 0, v1.width, v1.height);
        GLES20.glScissor(0, 0, v1.width, v1.height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClear(CLEAR_MASK);
        GLES30.glBlitFramebuffer(
            0, 0, v.width, v.height,
            0, 0, v1.width, v1.height,
            CLEAR_MASK, GLES30.GL_NEAREST);
        v1.swapBuffers(0); v1.makeCurrent();
        if (mColorFormat == ColorFormat.OPAQUE)
          //noinspection ConstantConditions
          TestsUtils.checkFrame(v1, format, shorts, context);
        else //noinspection ConstantConditions
          TestsUtils.checkFrame(v1, format, bytes, context);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        v2.makeCurrentReadFrom(v);
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
        GLES20.glViewport(0, 0, v2.width, v2.height);
        GLES20.glScissor(0, 0, v2.width, v2.height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClear(CLEAR_MASK);
        GLES30.glBlitFramebuffer(
            0, 0, v.width, v.height,
            0, 0, v2.width, v2.height,
            CLEAR_MASK, GLES30.GL_NEAREST);
        v2.swapBuffers(0); v2.makeCurrent();
        if (mColorFormat == ColorFormat.OPAQUE)
          //noinspection ConstantConditions
          TestsUtils.checkFrame(v2, format, shorts, context);
        else //noinspection ConstantConditions
          TestsUtils.checkFrame(v2, format, bytes, context);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

      } else {
        v1.makeCurrent();
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
        GLES20.glViewport(0, 0, v1.width, v1.height);
        GLES20.glScissor(0, 0, v1.width, v1.height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClear(CLEAR_MASK);
        render.drawFrame(surfaceTexture);
        v1.swapBuffers(0);
        if (mColorFormat == ColorFormat.OPAQUE)
          //noinspection ConstantConditions
          TestsUtils.checkFrame(v1, format, shorts, context);
        else //noinspection ConstantConditions
          TestsUtils.checkFrame(v1, format, bytes, context);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

        v2.makeCurrent();
        GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
        GLES20.glViewport(0, 0, v2.width, v2.height);
        GLES20.glScissor(0, 0, v2.width, v2.height);
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        GLES20.glClear(CLEAR_MASK);
        render.drawFrame(surfaceTexture);
        v2.swapBuffers(0);
        if (mColorFormat == ColorFormat.OPAQUE)
          //noinspection ConstantConditions
          TestsUtils.checkFrame(v2, format, shorts, context);
        else //noinspection ConstantConditions
          TestsUtils.checkFrame(v2, format, bytes, context);
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
      }

      surfaceTexture.releaseTexImage(); surface.release();
      surfaceTexture.setOnFrameAvailableListener(null);
      surfaceTexture.release(); v2.close(); v1.close();
      v.close(); c.close(); bmp.recycle();
    }

    /**
     * Test {@link Program2d} functionality.
     */
    @Test
    public final void testProgram() {

      final Context context = Objects.requireNonNull(mContext);
      final ColorFormat format = Objects.requireNonNull(mColorFormat);
      final Bitmap bmp = TestsUtils.createTestBitmap(mColorFormat.bmpConfig);
      final Bitmap bmp1 = TestsUtils.createTestBitmap1(mColorFormat.bmpConfig);
      final int w = bmp.getWidth(), h = bmp.getHeight();

      final short[] shorts = format == ColorFormat.OPAQUE ?
          TestsUtils.bitmapShorts(bmp) : null;
      final byte[] bytes = format == ColorFormat.OPAQUE ?
          null : TestsUtils.bitmapBytes(bmp);

      final short[] shorts1 = format == ColorFormat.OPAQUE ?
          TestsUtils.bitmapShorts(bmp1) : null;
      final byte[] bytes1 = format == ColorFormat.OPAQUE ?
          null : TestsUtils.bitmapBytes(bmp1);

      final Buffer buffer = format == ColorFormat.OPAQUE ?
          ShortBuffer.wrap(shorts) : ByteBuffer.wrap(bytes);
      final Buffer buffer1 = format == ColorFormat.OPAQUE ?
          ShortBuffer.wrap(shorts1) : ByteBuffer.wrap(bytes1);

      final EGLCore c = OpenGL.depth(format); final boolean r = false;
      final EGLView v = EGLView.offScreen(c, w, h, r); v.makeCurrent();

      final float[] stMatrix = Program2d.createIdentityMatrix();
      final int[] temp = new int[3]; final boolean[] targets = {true, false};
      final int[] textures = Texture2d.createTextures(targets, temp);

      final int[] program0 = Program2d.createProgram(targets[0]);
      final int[] program1 = Program2d.createProgram(targets[1]);


      Texture2d.bindTexture(textures, targets, 0, w, h,
          format.glPixelFormat, format.glPixelType, buffer, temp);
      Texture2d.bindTexture(textures, targets, 1, w, h,
          format.glPixelFormat, format.glPixelType, buffer1, temp);


      v.makeCurrent();
      //GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
      GLES20.glClearColor(0.0f,0.0f,0.0f,1.0f);
      GLES20.glClearDepthf(0); GLES20.glClearStencil(0);
      GLES20.glViewport(0, 0, w, h);
      GLES20.glScissor(0, 0, w, h);
      GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
      GLES20.glClear(CLEAR_MASK);


      Program2d.draw(program0, stMatrix, 0);
      Program2d.draw(program1, stMatrix, 1);

      v.swapBuffers(0);
      if (format == ColorFormat.OPAQUE)
        //noinspection ConstantConditions
        TestsUtils.checkFrame(v, format, shorts1, context);
      else //noinspection ConstantConditions
        TestsUtils.checkFrame(v, format, bytes1, context);
      GLES20.glDisable(GLES20.GL_SCISSOR_TEST);

      Program2d.closeProgram(program1);
      Program2d.closeProgram(program0);
      Texture2d.closeTextures(textures);


      GLESUtils.checkError(); v.close(); c.close();
    }

    /** Internal test callbacks */
    private static final class TestCallback implements Handler.Callback {
      /** {@inheritDoc} */
      @Override public final boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
          case MSG_DRAW: final Rect out = null;
            final Object[] params = Objects.requireNonNull((Object[]) msg.obj);
            final Surface surface = Objects.requireNonNull((Surface) params[0]);
            final Bitmap bitmap = Objects.requireNonNull((Bitmap) params[1]);
            final Canvas canvas = surface.lockCanvas(out);
            //final float left = 0.0f, top = 0.0f;
            final int sx = 1, sy = -1,
                px = Math.round(bitmap.getWidth() * 0.5f),
                py = Math.round(bitmap.getHeight() * 0.5f);
            final Matrix matrix = new Matrix() {{preScale(sx, sy, px, py);}};
            canvas.drawBitmap(bitmap, matrix, new Paint());
            surface.unlockCanvasAndPost(canvas);
            break;
        }
        return true;
      }
    }
  }

}

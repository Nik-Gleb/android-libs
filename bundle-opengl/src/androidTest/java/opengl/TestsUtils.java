/*
 * TestsUtils.java
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
import android.graphics.Color;
import android.support.annotation.NonNull;

import org.junit.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Common utils for testing.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 17/12/2017
 */
@SuppressWarnings({ "SameParameterValue", "unused" })
final class TestsUtils {

  /** The log-cat tag. */
  private static final String TAG = "TestsUtils";

  /** Test bitmap int colors. */
  private static final int[] BITMAP_INT_COLORS =
      {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
      //{Color.BLUE, Color.BLUE, Color.BLUE, Color.BLUE};

  /** Test bitmap int colors. */
  private static final int[] BITMAP_INT_COLORS1 =
      {Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE};
  //{Color.BLUE, Color.BLUE, Color.BLUE, Color.BLUE};


  /** The bitmap size. */
  private static final int BITMAP_SIZE = 2;
  /** The bitmap square. */
  private static final int BITMAP_SQUARE = BITMAP_SIZE * BITMAP_SIZE;


  /** Test file name */
  private static final String FILE_NAME = "frame.png";
  /** Test file access */
  private static final int FILE_MODE = Context.MODE_PRIVATE;

  /** Bitmap compress format. */
  private static final Bitmap.CompressFormat
      COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;

  /** Compress quality. */
  private static final int QUALITY = 0;

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private TestsUtils() {throw new AssertionError();}

  /**
   * Creating test file.
   *
   * @param ctx application context
   */
  static void createTestFile(@NonNull Context ctx) {
    try {Assert.assertTrue(new File(ctx.getFilesDir(), FILE_NAME).createNewFile());}
    catch (IOException exception) {Assert.fail(exception.getMessage());}
  }

  /**
   * Deleting test file.
   * @param ctx application context
   */
  static void deleteTestFile(@NonNull Context ctx)
  {Assert.assertTrue(new File(ctx.getFilesDir(), FILE_NAME).delete());}

  /**
   * Saving bitmap to test file.
   *
   * @param bitmap the source bitmap
   * @param ctx application context
   */
  private static void save(@NonNull Bitmap bitmap, @NonNull Context ctx) {
    try(final FileOutputStream output = ctx.openFileOutput(FILE_NAME, FILE_MODE))
    {Assert.assertTrue(bitmap.compress(COMPRESS_FORMAT, QUALITY, output));}
    catch (IOException exception) {Assert.fail(exception.getMessage());}
  }

  /**
   * Saving bitmap to test file.
   *
   * @param bitmap the source bitmap
   * @param ctx application context
   */
  private static void save(@NonNull Bitmap bitmap, @NonNull Context ctx,
      @NonNull String index) {
    try(final FileOutputStream output = ctx.openFileOutput(index + "-" + FILE_NAME, FILE_MODE))
    {Assert.assertTrue(bitmap.compress(COMPRESS_FORMAT, QUALITY, output));}
    catch (IOException exception) {Assert.fail(exception.getMessage());}
  }

  /**
   * @param config the bitmap config
   * @return the test bitmap
   */
  @NonNull static Bitmap createTestBitmap(@NonNull Bitmap.Config config)
  {return Bitmap.createBitmap(BITMAP_INT_COLORS, BITMAP_SIZE, BITMAP_SIZE, config);}

  /**
   * @param config the bitmap config
   * @return the test bitmap
   */
  @NonNull static Bitmap createTestBitmap1(@NonNull Bitmap.Config config)
  {return Bitmap.createBitmap(BITMAP_INT_COLORS1, BITMAP_SIZE, BITMAP_SIZE, config);}


  /**
   * @param bitmap source bitmap
   * @return the shorts
   */
  @NonNull static short[] bitmapShorts(@NonNull Bitmap bitmap) {
    final short[] result = new short[bitmap.getWidth() * bitmap.getHeight()];
    final ShortBuffer shortBuffer = ShortBuffer.wrap(result);
    bitmap.copyPixelsToBuffer(shortBuffer);
    shortBuffer.rewind(); return result;
  }


  /**
   * @param bitmap source bitmap
   * @return the bytes
   */
  @NonNull static byte[] bitmapBytes(@NonNull Bitmap bitmap) {
    final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getAllocationByteCount());
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN); bitmap.copyPixelsToBuffer(byteBuffer);
    byteBuffer.rewind(); final byte[] result = new byte[byteBuffer.limit()];
    byteBuffer.get(result, byteBuffer.position(), result.length); return result;
  }

  /**
   * Check for equals current frame
   *
   * @param view the egl-view
   * @param fmt the color format
   * @param exp the expected bytes
   */
  static void checkFrame(@NonNull EGLView view, @NonNull ColorFormat fmt,
      @NonNull short[] exp, @NonNull Context context) {
    final short[] act = OpenGL.getRGB565(view, fmt);
    if (!Arrays.equals(exp, act)) {
      final Bitmap bitmap = Bitmap.createBitmap
          (view.width, view.height, Bitmap.Config.RGB_565);
      bitmap.copyPixelsFromBuffer(ShortBuffer.wrap(act));
      save(bitmap, context); bitmap.recycle(); Assert.fail();
    } /*else {
      final Bitmap bitmap = Bitmap.createBitmap
          (view.width, view.height, Bitmap.Config.RGB_565);
      bitmap.copyPixelsFromBuffer(ShortBuffer.wrap(act));
      save(bitmap, context, view.toString()); bitmap.recycle();
    }*/
  }

  /**
   * Check for equals current frame
   *
   * @param view the egl-view
   * @param fmt the color format
   * @param exp the expected bytes
   */
  static void checkFrame(@NonNull EGLView view, @NonNull ColorFormat fmt,
      @NonNull byte[] exp, @NonNull Context context) {
    final byte[] act = OpenGL.getRGBA888(view, fmt);
    if(!Arrays.equals(exp, act)) {
      final Bitmap bitmap = Bitmap.createBitmap
          (view.width, view.height, Bitmap.Config.ARGB_8888);
      bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(act));
      save(bitmap, context); bitmap.recycle(); Assert.fail();
    }/* else {
      final Bitmap bitmap = Bitmap.createBitmap
          (view.width, view.height, Bitmap.Config.ARGB_8888);
      bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(act));
      save(bitmap, context, view.toString()); bitmap.recycle();
    }*/
  }


  /**
   * Returns a color in the RGB565 format based on a int-color instance.
   *
   * @param color the color to convert into a RGB565 color.
   * @return a color in the RGB565 format based on the specified Color.
   */
  @SuppressWarnings("UnnecessaryLocalVariable")
  private static short toRGB565(int color) {
    //noinspection PointlessBitwiseExpression
    short rgb = (short)
            (((((color >> 16) & 0xFF) & 0xf8) << 8) |
            ((((color >> 8) & 0xFF) & 0xfc) << 3) |
            (((color >> 0) & 0xFF) >> 3));
    // 0 is reserved for null -> set green (has highest resolution) to 1
    //if (rgb == 0) return 0x20;
    // If the color actually was 0x20 then set it to 0x40 (to the nearest green)
    //if (rgb == 0x20) return 0x40;
    return rgb;
  }

  /**
   * @param colors int-colors
   * @return converted rgb565-colors
   */
  private static short[] toRGB565(int[] colors) {
    final short[] result = new short[colors.length];
    for (int i = 0; i < colors.length; i++)
      result[i] = toRGB565(colors[i]);
    return result;
  }

  /* ========================== Lock Features ================================*/

  private static final Object LOCK = new Object();
  private static Boolean sResult = null;

  static void waitAndCheck() {
    synchronized (LOCK) {
      while (sResult == null) {
        try {
          LOCK.wait();}
        catch (InterruptedException ignored) {}
      }
    }
    Assert.assertTrue(sResult);
    sResult = null;
  }

  static void setResult(boolean successful) {
    synchronized (LOCK) {
      sResult = true;
      LOCK.notify();
    }
  }

}

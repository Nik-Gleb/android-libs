package opengl;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

/**
 * Color Format of Graphic.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 17/12/2017
 */
@Keep
@KeepPublicClassMembers
public enum ColorFormat {

  /** RGBA-8888 Mode. */
  TRANSPARENT(ColorMode.RGBA8888),
  /** RGB-565 Mode. */
  OPAQUE(ColorMode.RGB565);


  /** {@link PixelFormat} */
  final int pixelFormat;
  /** {@link ImageFormat} */
  final int imageFormat;
  /** {@link Bitmap.Config} */
  final Bitmap.Config bmpConfig;
  /** GL Pixel Format */
  final int glPixelFormat;

  /** Color depths */
  final int red, green, blue, alpha;
  /** The native visual id */
  final int nId;
  /** Num of bytes per pixel. */
  final int bytesPerPixel;

  /** The gl-PixelType */
  final int glPixelType;

  /**
   * Constructs a new {@link ColorFormat}
   *
   * @param mode the {@link ColorMode}
   */
  ColorFormat(@ColorMode int mode) {
    switch (mode) {
      case ColorMode.RGBA8888:
        pixelFormat = PixelFormat.RGBA_8888;
        imageFormat = SDK_INT >= M ? ImageFormat.FLEX_RGBA_8888 : 0x2A;
        bmpConfig = Bitmap.Config.ARGB_8888;
        glPixelType = GLES20.GL_UNSIGNED_BYTE;
        glPixelFormat = GLES20.GL_RGBA; nId = 1;
        red = 8; green = 8; blue = 8; alpha = 8;
        break;
      case ColorMode.RGB565:
        pixelFormat = PixelFormat.RGB_565;
        imageFormat = ImageFormat.RGB_565;
        bmpConfig = Bitmap.Config.RGB_565;
        glPixelType = GLES20.GL_UNSIGNED_SHORT_5_6_5;
        glPixelFormat = GLES20.GL_RGB; nId = 4;
        red = 5; green = 6; blue = 5; alpha = 0;
        break;
      default:
        throw new IllegalArgumentException();
    }
    bytesPerPixel = ImageFormat.getBitsPerPixel(imageFormat) / 8;
  }

  /**
   * Predefined color modes.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @IntDef({ColorMode.RGBA8888, ColorMode.RGB565})
  @Retention(RetentionPolicy.SOURCE) private @interface ColorMode
  {int RGBA8888 = 1, RGB565 = 4;}
}

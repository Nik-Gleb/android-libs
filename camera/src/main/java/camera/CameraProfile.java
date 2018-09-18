/*
 * Description.java
 * java-camera
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

package camera;

import android.annotation.SuppressLint;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.stream;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.deepEquals;
import static camera.CameraProfile.Facing.BACK;
import static camera.CameraProfile.Facing.EXTERNAL;
import static camera.CameraProfile.Facing.FRONT;
import static camera.CameraProfile.Level.LEGACY;
import static camera.CameraProfile.Rotation.ROTATION_000;

/**
 * CameraInstance CameraProfile.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 14/03/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class CameraProfile {

  /** EMPTY PROFILE. */
  public static final CameraProfile EMPTY = new CameraProfile("-1", ROTATION_000,
    EXTERNAL, LEGACY, new Size[0], new Size[0], false);

  /** Hash offset. */
  private static final int HASH_OFFSET = (int)
    (((long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE) / 3);

  /** The ID of camera device. */
  public final String id;

  /** The ROTATION of camera device. */
  public final Rotation rotation;

  /** The FACING of camera device. */
  public final Facing facing;

  /** Hardware support level */
  public final Level level;

  /** Supported sizes. */
  private final Size[] yuvSizes, jpgSizes;

  /** Hash of camera description. */
  private final int mHash;

  /**
   * Constructs a new {@link CameraProfile}.
   *
   * @param id        the id of camera device
   * @param rotation  the rotation of camera device
   * @param facing    the facing of camera device
   */
  CameraProfile(@NonNull String id, @NonNull Rotation rotation,
    @NonNull Facing facing, @NonNull Level level, @NonNull Size[] yuvSizes,
    @NonNull Size[] jpgSizes, boolean front) {
    this.id = id; this.rotation = rotation; this.facing = facing;
    this.level = level; this.yuvSizes = yuvSizes; this.jpgSizes = jpgSizes;
    final int offset = (front ?
      (facing == FRONT ? 0 : facing == BACK ? 1 : 2) :
      (facing == BACK ? 0 : facing == FRONT ? 1 : 2)) * HASH_OFFSET;
    mHash = Integer.MIN_VALUE + offset + Integer.parseInt(id);
  }

  /** {@inheritDoc} */
  @Override public final String toString() {
    if (isEmpty()) return "CameraProfile {empty}";
    else {
      final StringBuilder stringBuilder =
        new StringBuilder("CameraProfile {")
          .append("id=")          .append(id)
          .append(", facing=")    .append(facing.toString())
          .append(", rotation=")  .append(rotation.toString())
          .append(", level=")     .append(level.toString())
          .append('}');
      try {return stringBuilder.toString();}
      finally {stringBuilder.setLength(0); }
    }
  }

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj instanceof String)
      return Objects.equals(id, obj);
    else
      if (obj instanceof CameraProfile) {
        final CameraProfile that = (CameraProfile) obj;
        return
            facing == that.facing &&
              rotation == that.rotation &&
              level == that.level &&
              deepEquals(yuvSizes, that.yuvSizes) &&
              deepEquals(jpgSizes, that.jpgSizes);
      } else return false;
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return mHash;}

  /** @return true if this size is {@link #EMPTY} */
  public final boolean isEmpty() {return this == EMPTY;}

  /**
   * @param target target rect
   * @return calculated optimal size
   */
  @NonNull public Optional<Size> yuv(@NonNull Size target)
  {return chooseSize(yuvSizes, target);}

  /**
   * @param target target rect
   * @return calculated optimal size
   */
  @NonNull public Optional<Size> jpg(@NonNull Size target)
  {return chooseSize(jpgSizes, target);}


  /**
   * @param supported supported sizes
   * @param target target frame
   *
   * @return optimal size
   */
  @NonNull private static Optional<Size> chooseSize
  (@NonNull Size[] supported, @NonNull Size target) {
    // Keep up to date with rounding behavior in
    // frameworks/av/services/camera/libcameraservice/api2/CameraDeviceClient.cpp
    return stream(supported).min(comparingLong(target::euclidDistanceSquare));
  }

  /**
   * Facing.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 14/03/2018
   */
  public enum Facing {

    /** Selfie CameraInstance. */
    FRONT,
    /** Main CameraInstance. */
    BACK,
    /** External CameraInstance. */
    EXTERNAL;

    /** {@inheritDoc} */
    @Override public final String toString()
    {return this == FRONT ? "F" : this == BACK ? "B" : "X";}
  }

  /**
   * Rotation.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 14/03/2018
   */
  public enum Rotation {

    /** No Rotation. */
    ROTATION_000(0x000),
    /** 90 Rotation. */
    ROTATION_090(0x05a),
    /** 180 Rotation. */
    ROTATION_180(0x0b4),
    /** 270 Rotation. */
    ROTATION_270(0x10e);

    /** Rotation Degrees. */
    public final int degrees;

    /**
     * Constructs a new {@link Rotation}
     *
     * @param degrees rotation in degrees
     */
    Rotation(int degrees)
    {this.degrees = degrees;}

    /** {@inheritDoc} */
    @Override public final String toString() {
      return
          this == ROTATION_090 ? "→" :
              this == ROTATION_180 ? "↓" :
                  this == ROTATION_270 ? "←" :
                      this == ROTATION_000 ?"↑" :
                          null;
    }
  }

  /**
   * Hardware support levels.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 24/03/2018
   */
  public enum Level {

    /** This camera device is running in backward compatibility mode. */
    LEGACY,

    /**
     * This camera device does not have enough capabilities to qualify as a
     * <code>FULL</code> device or better.
     */
    LIMITED,

    /**
     * This camera device is capable of supporting advanced imaging
     * applications.
     */
    FULL,

    /**
     * This camera device is capable of YUV reprocessing and RAW data capture,
     * in addition to FULL-level capabilities.
     */
    LEVEL3
  }

  /**
   * Size description.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 11/09/2018
   */
  @SuppressWarnings("WeakerAccess")
  public static final class Size {

    /** NO_SIZE Constant. */
    @SuppressLint("Range")
    public static final Size EMPTY = new Size(-1, -1);

    /** Horizontal size. */
    public final int width;

    /** Vertical size. */
    public final int height;

    /** Hash code of {@link Size}. */
    private final int mHash;

    /**
     * Constructs a new {@link Size}
     *
     * @param width   horizontal size
     * @param height  vertical size
     */
    public Size(@IntRange(from = 0) int width, @IntRange(from = 0) int height)
    {this.width = width; this.height = height; mHash = height ^
      ((width << (Integer.SIZE / 2)) | (width >>> (Integer.SIZE / 2)));}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Size)) return false;
      final Size size = (Size) obj;
      return width == size.width &&
        height == size.height;
    }

    /** {@inheritDoc} */
    @Override public final int hashCode()
    {return Objects.hash(width, height, mHash);}

    /** {@inheritDoc} */
    @Override @NonNull public final String toString()
    {return "Size{" + (!isEmpty() ? width + "x" + height : "empty") + "}";}

    /** @return true if this size is {@link #EMPTY} */
    public final boolean isEmpty() {return this == EMPTY;}

    /** @return swapped size */
    @SuppressWarnings("SuspiciousNameCombination")
    @NonNull public final Size swap()
    {return new Size(height, width);}

    /** @return true for vertical ratio */
    public final boolean isVertical()
    {return height > width;}

    /**
     * @param size target size
     * @return euclid distance square
     */
    public final long euclidDistanceSquare(@NonNull Size size) {
      final long d0 = width - size.width;
      final long d1 = height - size.height;
      return d0 * d0 + d1 * d1;
    }
  }
}

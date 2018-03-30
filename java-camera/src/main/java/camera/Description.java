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

import java.util.Objects;

/**
 * Description.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 14/03/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Description implements Comparable<Description> {

  /** The ID of camera device. */
  public final String id;

  /** The ROTATION of camera device. */
  public final Rotation rotation;

  /** The FACING of camera device. */
  public final Facing facing;

  /** Hardware support level */
  public final Level level;

  /** Output sizes. */
  public final int
      yuvWidth, yuvHeight,
      jpgWidth, jpgHeight;

  /**
   * Constructs a new {@link Description}.
   *
   * @param id        the id of camera device
   * @param rotation  the rotation of camera device
   * @param facing    the facing of camera device
   */
  public Description
  (String id, Rotation rotation, Facing facing, Level level,
      int yuvWidth, int yuvHeight, int jpgWidth, int jpgHeight)
  {this.id = id; this.rotation = rotation; this.facing = facing;
  this.level = level; this.yuvWidth = yuvWidth; this.yuvHeight = yuvHeight;
  this.jpgWidth = jpgWidth; this.jpgHeight = jpgHeight;}

  /** {@inheritDoc} */
  @Override public final String toString() {
    final StringBuilder stringBuilder =
        new StringBuilder("Description {")
            .append("id=")          .append(id)
            .append(", facing=")    .append(facing.toString())
            .append(", rotation=")  .append(rotation.toString())
            .append(", level=")     .append(level.toString())
            .append(", yuvSize=")   .append(yuvWidth).append("x").append(yuvHeight)
            .append(", jpgSize=")   .append(jpgWidth).append("x").append(jpgHeight)
            .append('}');
    try {return stringBuilder.toString();}
    finally {stringBuilder.setLength(0); }
  }

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj instanceof String)
      return Objects.equals(id, obj);
    else
      if (obj instanceof Description) {
        final Description that = (Description) obj;
        return
            facing == that.facing &&
                rotation == that.rotation &&
                level == that.level &&
                yuvWidth == that.yuvWidth &&
                yuvHeight == that.yuvHeight &&
                jpgWidth == that.jpgWidth &&
                jpgHeight == that.jpgHeight;
      } else return false;
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return id.hashCode();}


  /** {@inheritDoc} */
  @Override public final int compareTo
  (@SuppressWarnings("NullableProblems") Description description)
  {final int BEFORE = -1, EQUAL = 0, AFTER = 1;
    return this.facing == description.facing ? EQUAL :
        this.facing == Facing.FRONT ? BEFORE :
            this.facing == Facing.EXTERNAL ? AFTER :
                description.facing == Facing.EXTERNAL ? BEFORE :
                    AFTER;}

  /**
   * Facing.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 14/03/2018
   */
  public enum Facing {

    /** Selfie Camera. */
    FRONT,
    /** Main Camera. */
    BACK,
    /** External Camera. */
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

}

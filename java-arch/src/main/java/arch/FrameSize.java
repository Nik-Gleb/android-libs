/*
 * FrameSize.java
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

package arch;

import java.util.Objects;

/**
 * Frame Description.
 *
 * @author Nikitemko Gleb
 * @since 1.0, 15/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class FrameSize {

  /** Empty size const */
  private static final int NO_SIZE = 0;
  /** Empty rate const */
  private static final float NO_RATE = 0.0f;

  /** Empty frame const */
  public static final FrameSize EMPTY =
      new FrameSize(NO_SIZE, NO_SIZE, NO_SIZE, NO_RATE);

  /** Properties */
  @SuppressWarnings("WeakerAccess")
  public final int width, height, format;
  /** Frame rate */
  public final float rate;

  /**
   * Constructs a new {@link FrameSize}.
   *
   * @param width the horizontal size of surface
   * @param height the vertical size of surface
   * @param format the pixel format of surface
   * @param rate the frame rate of surface
   */
  public FrameSize(int width, int height, int format, float rate)
  {this.width = width; this.height = height; this.format = format; this.rate = rate;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof FrameSize)) return false;
    final FrameSize frameSize = (FrameSize) obj;
    return
        width == frameSize.width &&
        height == frameSize.height &&
        format == frameSize.format &&
        Float.valueOf(rate).equals(frameSize.rate);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return Objects.hash(width, height, format, rate);}

  /** {@inheritDoc} */
  @Override public final String toString() {
    final StringBuilder builder =
        new StringBuilder("FrameSize{")
            .append("width=").append(width)
            .append(", height=").append(height)
            .append(", format=").append(format)
            .append(", rate=").append(rate)
            .append('}');
    try {return builder.toString();}
    finally {builder.setLength(0);}
  }
}

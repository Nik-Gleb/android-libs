/*
 * SurfaceFrame.java
 * extensions
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

package extensions;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.ImageReader;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Surface Frame.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 11/09/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class SurfaceFrame {

  /** NO_FRAME Constant. */
  @SuppressLint("Range")
  @SuppressWarnings("ConstantConditions")
  public static final SurfaceFrame
    EMPTY = new SurfaceFrame(null, -1, -1);

  /** SurfaceFrame Surface. */
  @NonNull public final Surface surface;

  /** Horizontal size. */
  public final int width;

  /** Vertical size. */
  public final int height;

  /** Hash code of this {@link SurfaceFrame}. */
  private final int mHash;

  /**
   * Constructs a new {@link SurfaceFrame}.
   *
   * @param surface output surface
   * @param width   horizontal size
   * @param height  vertical size
   */
  @SuppressWarnings({ "ConstantConditions", "NullableProblems" })
  private SurfaceFrame(@NonNull Surface surface,
    @IntRange(from = 0) int width, @IntRange(from = 0) int height)
  {this.surface = surface; this.width = width; this.height = height;
  mHash = surface != null ? surface.hashCode() : 0;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof SurfaceFrame)) return false;
    final SurfaceFrame frame = (SurfaceFrame) obj;
    return
      Objects.equals(surface, frame.surface) &&
        width == frame.width && height == frame.height;
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return mHash;}

  /** {@inheritDoc} */
  @Override public String toString() {
    return "SurfaceFrame{" + (!isEmpty() ?
      surface + ", " + width + "x" + height : "empty") + "}";
  }

  /** @return true if this size is {@link #EMPTY} */
  public final boolean isEmpty() {return this == EMPTY;}

  /**
   * @param holder   source surface holder
   * @param consumer surface frame consumer
   * @return         disposable object
   */
  @NonNull public static Runnable create
  (@NonNull SurfaceHolder holder, @NonNull Consumer<SurfaceFrame> consumer) {

    final Function<SurfaceHolder, SurfaceFrame> mapper = sfHolder -> {
      final Surface surface = holder.getSurface();
      final Rect rect = holder.getSurfaceFrame();
      return new SurfaceFrame(surface, rect.width(), rect.height());
    };

    final SurfaceFrame[] frame = new SurfaceFrame[] { SurfaceFrame.EMPTY};
    final Consumer<SurfaceFrame> inner = value -> {
      if (Objects.equals(frame[0], value)) return;
      consumer.accept(frame[0] = value);
    };

    final SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
      @Override public final void surfaceCreated(@NonNull SurfaceHolder holder)
      {inner.accept(mapper.apply(holder));}
      @Override public final void surfaceChanged
        (SurfaceHolder holder, int format, int width, int height)
      {inner.accept(mapper.apply(holder));}
      @Override public void surfaceDestroyed(SurfaceHolder holder)
      {inner.accept(SurfaceFrame.EMPTY);}
    };

    try {return () -> holder.removeCallback(callback);}
    finally {holder.addCallback(callback);}
  }

  /**
   * @param reader image reader
   * @return reader's frame
   */
  @NonNull public static SurfaceFrame create(@NonNull ImageReader reader)
  {return new SurfaceFrame(reader.getSurface(), reader.getWidth(), reader.getHeight());}
}

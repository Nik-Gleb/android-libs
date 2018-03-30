/*
 * EGLView.java
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

import android.graphics.SurfaceTexture;
import android.opengl.EGLSurface;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

import static android.opengl.EGL14.EGL_HEIGHT;
import static android.opengl.EGL14.EGL_WIDTH;
import static opengl.EGLView.WindowType.HOLDER;
import static opengl.EGLView.WindowType.OFFSCREEN;
import static opengl.EGLView.WindowType.SURFACE;
import static opengl.EGLView.WindowType.TEXTURE;

/**
 * Common base class for EGL surfaces.
 * <p>
 * There can be multiple surfaces associated with a single context.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/09/2017
 */
@Keep
@KeepPublicClassMembers
@SuppressWarnings({ "unused", "WeakerAccess", "SameParameterValue" })
public final class EGLView implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "EGLView";

  /**
   * EGLCore object we're associated with.
   * It may be associated with multiple surfaces.
   */
  @NonNull private final EGLCore mEglCore;

  /** The EGL-Surface. */
  @NonNull private final EGLSurface mEGLSurface;

  /** Surface auto-release flag. */
  private final boolean mRelease;
  /** Platform surface */
  private final Object mSurface;

  /** The horizontal size. */
  public final int width;
  /** The vertical size. */
  public final int height;

  /** The type of surface. */
  @SuppressWarnings("WeakerAccess")
  @WindowType public final int type;

  /**
   * Constructs a new {@link EGLView}.
   *
   * @param core the egl-kernel
   * @param surface the egl-surface
   * @param type the type of window
   * @param release window auto-release flag
   */
  private EGLView(@NonNull EGLCore core, @NonNull Object surface,
      @WindowType int type, boolean release) {
    mEGLSurface = (mEglCore = core).createSurface(surface);
    width = mEglCore.querySurface(mEGLSurface, EGL_WIDTH);
    height = mEglCore.querySurface(mEGLSurface, EGL_HEIGHT);
    this.type = type; mSurface = surface; mRelease = release;
  }

  /**
   * Constructs a new {@link EGLView}.
   *
   * @param core the egl-kernel
   * @param width the horizontal size
   * @param height the vertical size
   * @param type the type of window
   * @param release window auto-release flag
   */
  private EGLView(@NonNull EGLCore core, int width, int height,
      @WindowType int type, boolean release) {
    mEGLSurface = (mEglCore = core).createSurface
        (this.width = width, this.height = height);
    this.type = type; mSurface = null; mRelease = release;
  }

  /** {@inheritDoc} */
  @Override public void close() {
    mEglCore.releaseSurface(mEGLSurface);
    if (mRelease) {
      switch (type) {
        case SURFACE:
          ((Surface)mSurface).release();
          break;
        case TEXTURE:
          ((SurfaceTexture)mSurface).release();
          break;
        case HOLDER:
          (((SurfaceHolder)mSurface)).getSurface().release();
        case OFFSCREEN:
          // Nothing to do
          break;
      }
    }
  }

  /** @return true if this view is current. */
  final boolean isCurrent() {return mEglCore.isCurrent(mEGLSurface);}

  /** Makes our EGL context and surface current. */
  public final void makeCurrent() {mEglCore.makeCurrent(mEGLSurface);}

  /** Makes our EGL context and no-surface current. */
  public final void makeNothing() {mEglCore.makeNothingCurrent();}

  /**
   * Makes our EGL context and surface current for drawing.
   * Using the supplied surface for reading.
   */
  public final void makeCurrentReadFrom(@NonNull EGLView readSurface)
  {mEglCore.makeCurrent(mEGLSurface, readSurface.mEGLSurface);}

  /**
   * Calls setPresentationTime and eglSwapBuffers.
   * Use this to "publish" the current frame.
   *
   * @param nsecs presentation time stamp to EGL, in nanoseconds.
   */
  public final void swapBuffers(long nsecs) {
    if (nsecs > 0) mEglCore.setPresentationTime(mEGLSurface, nsecs);
    mEglCore.swapBuffers(mEGLSurface);
  }

  /**
   * @param surface checkable surface
   * @return true - if valid, otherwise - false
   */
  private static boolean isValid(@NonNull Surface surface)
  {return surface.isValid();}

  /**
   * @param holder checkable surface holder
   * @return true - if valid, otherwise - false
   */
  private static boolean isValid(@NonNull SurfaceHolder holder)
  {return isValid(holder.getSurface());}

  /**
   * @param texture checkable surface texture
   * @return true - if valid, otherwise - false
   */
  private static boolean isValid(@NonNull SurfaceTexture texture)
  {return !OpenGL.isOreo() || !texture.isReleased();}

  /** @return the type of hw-window. */
  @WindowType
  private static int getType(@Nullable Object obj) {
    return (obj instanceof Surface && isValid((Surface) obj)) ? SURFACE :
            (obj instanceof SurfaceHolder && isValid((SurfaceHolder) obj)) ? HOLDER :
                (obj instanceof SurfaceTexture && isValid((SurfaceTexture) obj)) ? TEXTURE :
                    EGLCore.error("Invalid surface object " + obj);
  }

  /**
   * Constructs a new {@link EGLView}.
   *
   * @param core the core-instance
   * @param width horizontal size of view
   * @param height vertical size of view
   * @param object the platform-window
   * @param release window auto-release flag
   *
   * @return new created {@link EGLView}
   */
  @NonNull private static EGLView create(@NonNull EGLCore core,
      int width, int height, @Nullable Object object, boolean release) {
    return object == null ? new EGLView(core, width, height, OFFSCREEN, release) :
        new EGLView(core, Objects.requireNonNull(object), getType(object), release);
  }

  /**
   * Constructs a new off-screen egl-surface
   *
   * @param core the core-instance
   * @param width horizontal size of view
   * @param height vertical size of view
   * @param release window auto-release flag
   *
   * @return new created {@link EGLView}
   */
  @NonNull public static EGLView offScreen
      (@NonNull EGLCore core, int width, int height, boolean release)
  {return create(core, width, height, null, release);}

  /**
   * Constructs a new hw-window egl-surface
   *
   * @param core the core-instance
   * @param surface the platform-window
   *
   * @return new created {@link EGLView}
   */
  @NonNull public static EGLView surface
  (@NonNull EGLCore core, @Nullable Surface surface, boolean release)
  {return create(core, 0, 0, surface, release);}

  /**
   * Constructs a new hw-window egl-surface
   *
   * @param core the core-instance
   * @param holder the platform-window
   * @param release window auto-release flag
   *
   * @return new created {@link EGLView}
   */
  @NonNull public static EGLView holder
  (@NonNull EGLCore core, @Nullable SurfaceHolder holder, boolean release)
  {return create(core, 0, 0, holder, release);}

  /**
   * Constructs a new hw-window egl-surface
   *
   * @param core the core-instance
   * @param texture the platform-window
   * @param release window auto-release flag
   *
   * @return new created {@link EGLView}
   */
  @NonNull public static EGLView holder
  (@NonNull EGLCore core, @Nullable SurfaceTexture texture, boolean release)
  {return create(core, 0, 0, texture, release);}

  /**
   * Predefined Window-Types.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 06/10/2016
   */
  @IntDef({ OFFSCREEN, SURFACE, HOLDER, TEXTURE})
  @Retention(RetentionPolicy.SOURCE)
  @interface WindowType {
    /** OFFSCREEN. */
    int OFFSCREEN = -1;
    /** {@link Surface}. */
    int SURFACE = 0;
    /** {@link SurfaceHolder}. */
    int HOLDER  = 1;
    /** {@link SurfaceTexture}. */
    int TEXTURE = 2;
  }
}

/*
 * EGLCore.java
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

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;

import static opengl.Logger.LS;
import static opengl.Logger.getHandle;
import static opengl.Logger.log;

/**
 * Core EGL Module..
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/09/2017
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class EGLCore implements Closeable {

  /** The egl-display. */
  @NonNull final EGLDisplay eglDisplay;
  /** The egl-config. */
  @NonNull private final EGLConfig mEGLConfig;
  /** The egl-context. */
  @NonNull private final EGLContext mEGLContext;
  /** The gl-Version. */
  private final int mGlVersion;

  /**
   * Constructs a new {@link EGLCore} by builder
   *
   * @param bldr the builder
   */
  private EGLCore(@NonNull Builder bldr) {
    final EGLResult res = createContext(eglDisplay = createDisplay(),
        bldr.eglContext, bldr.nativeId, bldr.red, bldr.green, bldr.blue,
        bldr.alpha, bldr.depth, bldr.stencil);
    mEGLContext = res.ctx; mEGLConfig = res.conf; mGlVersion = res.ver;
  }

  /** Release resources. */
  public final void close() {
    closeContext(eglDisplay, mEGLContext);
    closeDisplay(eglDisplay);
    EGL14.eglReleaseThread();
  }

  /** @return current open-gl version */
  public final int getVersion()
  {return mGlVersion;}

  /**
   * Creates a new egl-display.
   *
   * @return a new egl-display connection
   */
  @NonNull
  private static EGLDisplay createDisplay() {
    final EGLDisplay result = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    if (result == EGL14.EGL_NO_DISPLAY) return error("Unable to get display");
    else {
      final int[] v = new int[2]; final int major = 0, minor = 1;
      if (!EGL14.eglInitialize(result, v, major, v, minor))
        try {return error("Unable to initialize Display " + getHandle(result));}
        finally {closeDisplay(result);}
      else return log(result,
            "Display %d created (EGL" + v[0] + "" + v[1] + ")");
    }
  }

  /**
   * Close an egl-display connection.
   *
   * @param display egl-display connection
   */
  private static void closeDisplay(@NonNull EGLDisplay display) {
    if (EGL14.eglTerminate(display))
      log(display, "Display %d destroyed");
      else error("Unable to destroy display " + getHandle(display));
  }

  /**
   * Creates an egl-context
   *
   * @param display an EGL display connection instance
   *
   * @return a new EGL rendering context
   **/
  @NonNull
  private static EGLResult createContext(@NonNull EGLDisplay display,
      @Nullable EGLContext ctx, int f, int r, int g, int b, int a, int d, int s) {
    final int v3 = 3, v2 = 2;
    EGLResult res = createContext(display, v3, ctx, f, r, g, b, a, d, s);
    if (res == null) res = createContext(display, v2, ctx, f, r, g, b, a, d, s);
    return checkContext(display, res);
  }

  /**
   * Creates an egl-context
   *
   * @param display an EGL display connection instance
   *
   * @return a new EGL rendering context
   **/
  @Nullable
  private static EGLResult createContext
  (@NonNull EGLDisplay display, int v, @Nullable EGLContext ctx,
      int f, int r, int g, int b, int a, int d, int s) {
    final EGLConfig config = createConfig(display, f, v, r, g, b, a, d, s);
    if (config != null) {
      log(config, "Config %d created"); final int offset = 0;
    final int[] attrs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, v, EGL14.EGL_NONE};
      final EGLContext context = EGL14.eglCreateContext
          (display, config, ctx, attrs, offset);
      if (EGL14.eglGetError() == EGL14.EGL_SUCCESS) {
        log(context, "Context %d created");
        return new EGLResult(v, config, context);
      } else {
        try {return error("Can't opaque context");}
        catch (RuntimeException exception) {return null;}
      }
    } else {
      try {return error("Can't choose config");}
      catch (RuntimeException exception) {return null;}
    }
  }

  /**
   * Check egl-context initialization.
   *
   * @param display the egl-display
   * @param result the egl-result
   * @return checked egl-result
   */
  @NonNull
  private static EGLResult checkContext
      (@NonNull EGLDisplay display, @Nullable EGLResult result) {
    if (result == null) return error("Can't initialize egl");
    final int[] values = new int[1]; int offset = 0;
    return EGL14.eglQueryContext(display, result.ctx,
      EGL14.EGL_CONTEXT_CLIENT_VERSION, values, offset) ?
        result : (EGLResult) error("Can't opaque context");
  }

  /**
   * Close the egl-context.
   *
   * @param display an egl-display
   * @param context an egl-context
   */
  private static void closeContext
      (@NonNull EGLDisplay display, @NonNull EGLContext context) {
    if (EGL14.eglDestroyContext(display, context))
      log(context, "Context %d destroyed");
    else error("Unable to destroy context " + getHandle(context));
  }

  /**
   * Create a suitable EGLConfig.
   *
   * @param displ the egl-display
   * @param visualId the id of window-mode
   * @param ver Must be 2 or 3.
   */
  @Nullable
  private static EGLConfig createConfig
  (@NonNull EGLDisplay displ, int visualId, int ver, int red, int green, int blue,
      int alpha, int depth, int stencil) {

    int renderable = EGL14.EGL_OPENGL_ES2_BIT;
    if (ver >= 3) renderable |= EGLExt.EGL_OPENGL_ES3_BIT_KHR;

    final int EGL_RECORDABLE_ANDROID = 0x3142;
    final int attrsOffset = 0;
    final int[] attrs = {
        EGL14.EGL_RED_SIZE,         red,
        EGL14.EGL_GREEN_SIZE,       green,
        EGL14.EGL_BLUE_SIZE,        blue,
        EGL14.EGL_ALPHA_SIZE,       alpha,
        EGL14.EGL_DEPTH_SIZE,       depth,
        EGL14.EGL_STENCIL_SIZE,     stencil,
        EGL14.EGL_RENDERABLE_TYPE,  renderable,
        EGL_RECORDABLE_ANDROID,     EGL14.EGL_TRUE,
        EGL14.EGL_NONE
    };

    final int configsOffset = 0;
    final int[] number = new int[1];
    final int numberOffset = 0;

    EGLConfig[] configs;
    int configsSize = 0;

    boolean isFirst = false;
    do {isFirst = !isFirst;
    configs = new EGLConfig[configsSize];
      if(EGL14.eglChooseConfig(displ, attrs, attrsOffset,
          configs, configsOffset, configsSize, number, numberOffset)) {
        if ((configsSize = number[0]) <= 0 &&
            (configsSize = configs(displ, number, numberOffset)) <= 0)
          configsSize = 1;
      } else configs = null;
    } while (isFirst && configs != null);

    if (configs == null) return null;

    final int def = 0, ofs = 0;
    final int[] val = new int[1];

    for (int i = 0; i < configsSize; i++) {
      final EGLConfig config = configs[i];
      final int d = find(displ, config, EGL14.EGL_DEPTH_SIZE, def, val, ofs);
      final int s = find(displ, config, EGL14.EGL_STENCIL_SIZE, def, val, ofs);
      if ((d >= attrs[9]) && (s >= attrs[11])) {
        final int r = find(displ, config, EGL14.EGL_RED_SIZE, def, val, ofs);
        final int g = find(displ, config, EGL14.EGL_GREEN_SIZE, def, val, ofs);
        final int b = find(displ, config, EGL14.EGL_BLUE_SIZE, def, val, ofs);
        final int a = find(displ, config, EGL14.EGL_ALPHA_SIZE, def, val, ofs);
        if ((r == attrs[1]) && (g == attrs[3]) && (b == attrs[5]) && (a == attrs[7])) {
          final int id = find(displ, config, EGL14.EGL_NATIVE_VISUAL_ID, def, val, ofs);
          if (id == visualId) return config;
        }
      }
    }
    return null;
  }

  /**
   * @param disp the display
   * @param val values
   * @param ofs values offset
   *
   * @return available configs
   */
  private static int configs(@NonNull EGLDisplay disp, int[] val, int ofs) {
    final int num = 0, offset = 0; final EGLConfig[] configs = null;
    try {return EGL14.eglGetConfigs(disp, configs, offset, num, val, ofs) ? val[ofs] : 1;}
    catch (Exception exception)
    {return EGL14.eglGetConfigs(disp, new EGLConfig[num], offset, num, val, ofs) ? val[ofs] : 1;}
  }


  /**
   * @param disp the display
   * @param conf the configuration
   * @param attr the attributes
   * @param def default value
   * @param val values
   * @param ofs values offset
   *
   * @return fended attributes
   */
  private static int find
  (@NonNull EGLDisplay disp, @NonNull EGLConfig conf, int attr, int def, int[] val, int ofs)
  {return EGL14.eglGetConfigAttrib(disp, conf, attr, val, ofs) ? val[ofs] : def;}

  /**
   * Creates an EGL surface associated with a Surface.
   * <p>
   * If this is destined for MediaCodec,
   * the EGLConfig should have the "recordable" attribute.
   *
   * @param surface the platform-window
   * @return the egl-surface
   */
  @NonNull
  EGLSurface createSurface(@NonNull Object surface) {
    final int[] attrs = { EGL14.EGL_NONE}; int offset = 0;
    final EGLSurface result = EGL14.eglCreateWindowSurface
        (eglDisplay, mEGLConfig, surface, attrs, offset);
    /*if (result != null)
      EGL14.eglSurfaceAttrib(eglDisplay, result,
          EGL14.EGL_SWAP_BEHAVIOR, 0);*/
    return result != null ? log(result, "Surface %d created") :
        (EGLSurface) error("Can't opaque surface " + surface);
  }

  /**
   * Creates an EGL surface associated with an offscreen buffer.
   *
   * @param width the horizontal size
   * @param height the vertical size
   *
   * @return the egl-surface
   */
  @NonNull
  EGLSurface createSurface(int width, int height) {
    final int offset = 0; final int[] attrs =
        { EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE};
    final EGLSurface result = EGL14.eglCreatePbufferSurface
        (eglDisplay, mEGLConfig, attrs, offset);
    return result != null ? log(result, "Surface %d created") :
        (EGLSurface) error("Can't opaque surface " + width + "x" + height);
  }

  /**
   * Destroys the specified surface.
   *
   * Note the EGLSurface won't actually be
   * destroyed if it's still current in a context.
   */
  final void releaseSurface(@NonNull EGLSurface surface) {
    if (EGL14.eglDestroySurface(eglDisplay, surface))
      log(surface, "Surface %d destroyed");
    else error("Can't destroy surface " + getHandle(surface));
  }

  /**
   * Makes our EGL context current.
   * Using the supplied surface for both "draw" and "read".
   *
   * @param surface the egl-surface
   */
  final void makeCurrent(@NonNull EGLSurface surface) {
    if (!EGL14.eglMakeCurrent(eglDisplay, surface, surface,
        surface == EGL14.EGL_NO_SURFACE ? EGL14.EGL_NO_CONTEXT : mEGLContext))
      error("Can't make current surface " + getHandle(surface));
    else log(surface, "Setup current surface %d");
  }

  /**
   * Makes our EGL context current.
   * Using the supplied "draw" and "read" surfaces.
   *
   * @param draw the draw-surface
   * @param read the read-surface
   */
  @SuppressWarnings("WeakerAccess")
  final void makeCurrent(@NonNull EGLSurface draw, @NonNull EGLSurface read) {
    if (!EGL14.eglMakeCurrent(eglDisplay, draw, read, mEGLContext))
      error("Can't make current: " + getHandle(draw) + ", " + getHandle(read));
  }

  /** Makes no context current. */
  final void makeNothingCurrent() {makeCurrent(EGL14.EGL_NO_SURFACE);}

  /**
   * Calls eglSwapBuffers.
   * Use this to "publish" the current frame.
   *
   * @param surface the egl-surface
   */
  final void swapBuffers(@NonNull EGLSurface surface) {
    if (!EGL14.eglSwapBuffers(eglDisplay, surface))
      error("Can't swap buffers " + getHandle(surface));
    else log(surface, "Surface %d swapped buffers");
  }

  /**
   * Sends the presentation time stamp to EGL..
   *
   * @param surface the egl-surface
   * @param nsecs time is expressed in nanoseconds
   */
  final void setPresentationTime(@NonNull EGLSurface surface, long nsecs) {
    if (!EGLExt.eglPresentationTimeANDROID(eglDisplay, surface, nsecs))
      error("Can't set time " + getHandle(surface) + " " + nsecs);
    else log(surface, "Set presentation surface %d");
  }

  /**
   * @param surface the egl-surface
   * @return true if our context and the specified surface are current.
   **/
  final boolean isCurrent(@NonNull EGLSurface surface) {
    return mEGLContext.equals(EGL14.eglGetCurrentContext()) &&
        surface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)) &&
            surface.equals(EGL14.eglGetCurrentSurface(EGL14.EGL_READ));
  }

  /** @return current draw and read surfaces, null when no current context */
  @SuppressWarnings("unused")
  @Nullable final EGLSurface[] getCurrentSurfaces() {
    if (mEGLContext.equals(EGL14.eglGetCurrentContext())) return null;
    return new EGLSurface[] {EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),
        EGL14.eglGetCurrentSurface(EGL14.EGL_READ)};
  }

  /**
   * Performs a simple surface query.
   *
   * @param surface the egl-surface
   * @param what the what the surface
   */
  final int querySurface(@NonNull EGLSurface surface, int what) {
    final int[] value = new int[1]; final int offset = 0;
    return EGL14.eglQuerySurface(eglDisplay, surface, what, value, offset) ?
        value[0] : (int) error("Can't query surface");
  }

  /**
   * EGL for information, then formats it all into one giant str.
   *
   * @param display the {@link EGLDisplay} instance
   * @param builder the {@link StringBuilder} instance
   */
  static void info
  (@NonNull EGLDisplay display, @NonNull StringBuilder builder) {
    builder
        .append(LS).append("===== EGL Information =====")
        .append(LS).append("vendor    : ")
        .append(EGL14.eglQueryString(display, EGL14.EGL_VENDOR))
        .append(LS).append("version   : ")
        .append(EGL14.eglQueryString(display, EGL14.EGL_VERSION))
        .append(LS).append("client API: ")
        .append(EGL14.eglQueryString(display, EGL14.EGL_CLIENT_APIS))
        .append(LS).append("extensions:").append(LS)
        .append(OpenGL.format
            (EGL14.eglQueryString(display, EGL14.EGL_EXTENSIONS)));
  }


  /** @param message the error message */
  static <T> T error(@NonNull String message) {
    final int error = EGL14.eglGetError();
    throw new RuntimeException(error != EGL14.EGL_SUCCESS ?
        android.opengl.GLUtils.getEGLErrorString(error) : message);
    /*
    int error; if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS)
      throw new RuntimeException(msg + ": EGL error: 0x" + toHexString(error));
    */
  }


  /** The result of egl-initialization. */
  private static final class EGLResult {

    /** The gl-version. */
    final int ver;
    /** The egl-config. */
    @NonNull final EGLConfig conf;
    /** The egl-context. */
    @NonNull final EGLContext ctx;

    /**
     * Constructs a new {@link EGLResult}.
     *
     * @param version   gl-version
     * @param config   egl-config
     * @param context  egl-context
     */
    EGLResult (
        int version,
        @NonNull EGLConfig config,
        @NonNull EGLContext context
    ) {
      this.ver = version;
      this.conf = config;
      this.ctx = context;
    }
  }

  /**
   * Used to add parameters to a {@link EGLCore}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link EGLCore} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static final class Builder {

    /** Color depths */
    final int red, green, blue, alpha;
    /** The native visual id */
    final int nativeId;

    /** Shared context. */
    EGLContext eglContext = EGL14.EGL_NO_CONTEXT;

    /** depth & stencil */
    int depth = 0, stencil = 0;

    /** Constructs a new {@link Builder}. */
    Builder(int r, int g, int b, int a, int n)
    {red = r; green = g; blue = b; alpha = a; nativeId = n;}

    /** Create a {@link EGLCore} from this {@link Builder}. */
    @NonNull
    public EGLCore build() {
      final Builder builder = this;
      return new EGLCore(builder);
    }

    /**
     * Shared context.
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    final Builder context(@NonNull EGLContext context)
    {eglContext = context; return this;}

    /**
     * DEPTH=0 mode. (DEFAULT)
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    final Builder d0()
    {depth = 0; stencil = 0; return this;}

    /**
     * DEPTH=16 mode.
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    final Builder d16()
    {depth = 16; stencil = 0; return this;}

    /**
     * DEPTH=24 mode.
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    final Builder d24()
    {depth = 24; stencil = 0; return this;}

    /**
     * DEPTH=24 with STENCIL=8 mode.
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull
    final Builder d24s8()
    {depth = 24; stencil = 8; return this;}

  }

}

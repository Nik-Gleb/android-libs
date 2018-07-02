/*
 * CameraDeviceManager.java
 * camera
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

import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.io.Closeable;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import camera.AndroidCameraTools.CameraDeviceBuilder;
import camera.AndroidCameraTools.CaptureCallbackOptions;
import camera.AndroidCameraTools.CaptureRequestBuilder;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 26/03/2018
 */
final class CameraDeviceManager implements Closeable {

  /** Camera Device. */
  private final CameraDevice mDevice;
  /** Main handler. */
  private final Handler mHandler;

  /** Capture controller. */
  private final Consumer<Consumer<Boolean>> mController;
  /** Capture configurator. */
  private final BiConsumer<Integer, CaptureRequestBuilder> mConfigurator;
  /** State listener. */
  private final Consumer<Integer> mListener;
  /** Disposable captures callback. */
  private final CaptureCallbackOptions mOptions;
  /** Snapshot allowed. */
  private final boolean mSnapshot;

  /** Surfaces surfaces. */
  private final SurfacesProvider mProvider;

  /** Output targets. */
  private List<Surface> mSurfaces = null;
  /** Capture Session CaptureCallback. */
  private CaptureSessionCallback mSession = null;
  /** Closed state flag. */
  private boolean mClosed = false;

  CameraDeviceManager
      (@NonNull CameraDeviceBuilder builder, @NonNull CameraDevice device) {
    mDevice = device; mHandler = builder.handler; mController = builder.controller;
    mConfigurator = builder.configurator; mListener = builder.listener;
    mOptions = builder.options; mSnapshot = builder.snapshot;
    final CameraDeviceManager instance = this;
    mProvider = new SurfacesProvider(builder.surfaces, instance);
  }

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    mProvider.close();
    mClosed = true;
  }

  /** invalidate output targets */
  private void invalidate() {
    if (mSurfaces == null) {
      if (mSession != null)
        mSession.close();
      mSession = null;
    } else {
      final List<Surface> targets = mSurfaces; final Handler handler = mHandler;
      mSession = new CaptureSessionCallback(mDevice, handler, targets,
          session -> {
            final CaptureRequestManager.Builder builder =
                CaptureRequestManager.create (targets, handler);
            builder.controller    = mController;
            builder.configurator  = mConfigurator;
            builder.listener      = mListener;
            builder.options       = mOptions;
            builder.snapshot      = mSnapshot;
            return builder.build(session);
          });
    }
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** Surfaces surfaces. */
  private static final class SurfacesProvider
      implements Consumer<List<Surface>>, Closeable {

    /** Surfaces surfaces. */
    final Consumer<Consumer<List<Surface>>> surfaces;

    /** Camera Device Manager. */
    private final CameraDeviceManager mManager;

    /** Closed state flag. */
    private boolean mClosed = false;

    /**
     * Constructs a new {@link SurfacesProvider}.
     *
     * @param surfaces the surfaces provider
     * @param manager camera device manager
     */
    SurfacesProvider(@NonNull Consumer<Consumer<List<Surface>>> surfaces,
        @NonNull CameraDeviceManager manager) {
      this.surfaces = surfaces; mManager = manager;
      final Consumer<List<Surface>> instance = this;
      surfaces.accept(instance);
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ConstantConditions")
    @Override public final void close() {
      if (mClosed) return;
      final Consumer<List<Surface>> instance = null;
      surfaces.accept(instance);
      if (mManager.mSurfaces != null) {
        mManager.mSurfaces = null;
        mManager.invalidate();
      } mClosed = true;
    }

    /** {@inheritDoc} */
    @Override protected final void finalize() throws Throwable
    {try {close();} finally {super.finalize();}}

    /** {@inheritDoc} */
    @Override public final void accept(@Nullable List<Surface> surfaces)
    {mManager.mSurfaces = surfaces; mManager.invalidate();}
  }


}

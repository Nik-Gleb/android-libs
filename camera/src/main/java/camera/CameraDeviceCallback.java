/*
 * CameraDeviceCallback.java
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

import android.annotation.SuppressLint;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.io.Closeable;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Function;

import static camera.AndroidCameraTools.CameraDeviceError.NO_CAMERA_ERRORS;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 26/03/2018
 */
final class CameraDeviceCallback
    extends CameraDevice.StateCallback
    implements Closeable {

  /** Camera Device Manager Factory. */
  private final Function<CameraDevice, Closeable> mFactory;

  /** Camera Device Instance. */
  @Nullable private CameraDevice mCamera = null;

  /** Camera Device Manager. */
  @Nullable private Closeable mManager = null;

  /**
   * Constructs a new {@link CameraDeviceCallback}.
   *
   * @param factory camera device manager factory
   */
  @SuppressLint("MissingPermission")
  @RequiresPermission(android.Manifest.permission.CAMERA)
  CameraDeviceCallback(@NonNull CameraManager manger, @NonNull String cameraId,
      @NonNull Function<CameraDevice, Closeable> factory, @NonNull Handler handler) {
    mFactory = factory;
    final CameraDeviceCallback instance = this;
    try {manger.openCamera(cameraId, instance, handler);}
    catch (CameraAccessException exception)
    {throw new RuntimeException(exception);}
  }

  /** {@inheritDoc} */
  @SuppressWarnings("ConstantConditions")
  @Override public final void close() {
    if (mCamera == null) return;
    onDisconnected(mCamera);
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void onOpened
  (@NonNull CameraDevice camera)
  {mCamera = camera; mManager = mFactory.apply(camera);}

  /** {@inheritDoc} */
  @Override public final void onClosed
  (@NonNull CameraDevice camera) {mCamera = null;}

  /** {@inheritDoc} */
  @SuppressLint("WrongConstant")
  @Override public final void onDisconnected
  (@NonNull CameraDevice camera)
  {onError(camera, NO_CAMERA_ERRORS);}

  /** {@inheritDoc} */
  @Override public final void onError
  (@NonNull CameraDevice camera, int error) {
    if (mManager != null) {
      try {mManager.close();}
      catch (IOException e)
      {throw new RuntimeException(e);}
      mManager = null;
    }
    camera.close(); if (error == NO_CAMERA_ERRORS) return;
    final String msg = "Camera device has encountered a serious error: %d";
    throw new RuntimeException(String.format(Locale.US, msg, error));
  }


}

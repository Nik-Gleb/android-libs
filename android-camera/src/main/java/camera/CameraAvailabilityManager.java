/*
 * CameraAvailabilityManager.java
 * android-camera
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

import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraManager.AvailabilityCallback;
import android.os.Handler;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.util.function.BiConsumer;

/**
 * Camera Availability CaptureCallback.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 26/03/2018
 */
final class CameraAvailabilityManager extends AvailabilityCallback
    implements Closeable {

  /** Availability flags. */
  private static final boolean
  AVAILABLE = true, UNAVAILABLE = false;

  /** Camera manager */
  private final CameraManager mManager;

  /** The captureCallback. */
  private final BiConsumer<String, Boolean> mCallback;

  /** Closed state flag. */
  private boolean mClosed = false;

  /**
   * Constructs a new {@link CameraAvailabilityManager}.
   *
   * @param manager camera devices manager
   * @param callback сфддифсл
   * @param handler main handler
   */
  CameraAvailabilityManager(@NonNull CameraManager manager,
      @NonNull Handler handler, @NonNull BiConsumer<String, Boolean> callback) {
    mManager = manager; mCallback = callback;
    final CameraAvailabilityManager instance = this;
    mManager.registerAvailabilityCallback(instance, handler);
  }

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    final CameraAvailabilityManager instance = this;
    mManager.unregisterAvailabilityCallback(instance);
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void onCameraAvailable (@NonNull String cameraId)
  {super.onCameraAvailable(cameraId); mCallback.accept(cameraId, AVAILABLE);}

  /** {@inheritDoc} */
  @Override public final void onCameraUnavailable(@NonNull String cameraId)
  {mCallback.accept(cameraId, UNAVAILABLE); super.onCameraUnavailable(cameraId);}
}

/*
 * CaptureSessionCallback.java
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

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.hardware.camera2.CameraDevice;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.io.Closeable;
import java.util.List;
import java.util.function.Function;

/**
 * Capture Session CaptureCallback for proxy capture callbacks.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/03/2018
 */
final class CaptureSessionCallback extends StateCallback implements Closeable {

  /** {@link Callback}'s factory. */
  private final Function<CameraCaptureSession, Callback> mFactory;

  /** CameraInstance Capture Session. */
  @Nullable private CameraCaptureSession mSession, mTerminated = null;

  /** Capture Request Manager. */
  @Nullable private Callback mManager = null;

  CaptureSessionCallback(@NonNull CameraDevice camera, @NonNull Handler handler,
      @NonNull List<Surface> outputs,
      @NonNull Function <CameraCaptureSession, Callback> factory) {
    mFactory = factory;
    try {init(camera, handler, outputs);}
    catch (CameraAccessException e)
    {throw new RuntimeException(e);}
  }

  private void init(@NonNull CameraDevice camera, @NonNull Handler handler,
      @NonNull List<Surface> outputs) throws CameraAccessException {
    final CaptureSessionCallback instance = this;
    camera.createCaptureSession(outputs, instance, handler);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("ConstantConditions")
  @Override public final void close() {
    if (mSession == null || mTerminated != null) return;
    final boolean failed = false; exit(mSession, failed);
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /**
   * @param session session instance for exit
   * @param failed failed flag
   */
  private void exit(@NonNull CameraCaptureSession session, boolean failed) {
    mTerminated = session; mTerminated.close();
    if (mTerminated != mSession) onClosed(mTerminated); if (!failed) return;
    final String msg = "CaptureSessionCallback cannot be configured as requested";
    throw new RuntimeException(msg);
  }

  /** {@inheritDoc} */
  public final void onConfigured
  (@NonNull CameraCaptureSession session) {
    if (mSession != null) throw new IllegalStateException();
    mSession = session; mManager = mFactory.apply(session);
  }

  /** {@inheritDoc} */
  @Override public final void onClosed
  (@NonNull CameraCaptureSession session)
  {mSession = null; mTerminated = null;}

  /** {@inheritDoc} */
  @Override public final void onConfigureFailed
  (@NonNull CameraCaptureSession session)
  {final boolean failed = true; exit(session, failed);}

  /** {@inheritDoc} */
  public final void onReady
  (@NonNull CameraCaptureSession session) { super.onReady(session);
    if (mSession == null || mSession != session) return;
    if (mManager == null) throw new IllegalStateException();
    if (mTerminated == null) mManager.onDeActivated();
    else {mManager.close(); mManager = null;}
  }

  /** {@inheritDoc} */
  public final void onActive
  (@NonNull CameraCaptureSession session) {
    super.onActive(session);
    if (mSession == null || mSession != session || mManager == null)
      throw new IllegalStateException();
    mManager.onActivated();
  }

  /** {@inheritDoc} */
  public final void onCaptureQueueEmpty
  (@NonNull CameraCaptureSession session)
  {super.onCaptureQueueEmpty(session);}

  /** {@inheritDoc} */
  public final void onSurfacePrepared
  (@NonNull CameraCaptureSession session, @NonNull Surface surface)
  {super.onSurfacePrepared(session, surface);}

  /**
   * Capture Request Manager.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 26/03/2018
   */
  interface Callback extends Closeable {

    /** Calls after session was activated. */
    @SuppressWarnings("unused")
    default void onActivated() {}

    /** Calls after session was deactivated. */
    @SuppressWarnings("unused")
    default void onDeActivated() {}

    /** {@inheritDoc} */
    @Override default void close() {}
  }

}

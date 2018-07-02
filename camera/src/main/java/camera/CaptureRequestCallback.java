/*
 * CaptureRequestCallback.java
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
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.io.Closeable;

/**
 * Capture Request CaptureCallback for proxy capture callbacks.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/03/2018
 */
final class CaptureRequestCallback extends CaptureCallback implements Closeable {

  /** A configured capture session. */
  @NonNull private final CameraCaptureSession mSession;
  /** An immutable package of settings and outputs. */
  @NonNull private final CaptureRequest mRequest;
  /** "REPEATING" or "DISPOSABLE" mode. */
  private final boolean mRepeating;

  /** Callbacks holder. */
  @Nullable private final CallbacksHolder mHolder;

  /** "CLOSED"-state flag. */
  private boolean mClosed = false;

  /**
   * Constructs a new {@link CaptureCallback}.
   *
   * @param builder builder instance
   */
  CaptureRequestCallback(@NonNull Builder builder) {
    mSession    = builder.session;
    mRequest    = builder.request;
    mRepeating  = builder.repeating;
    mHolder     = builder.holder;
    try {init();}
    catch (CameraAccessException e)
    {throw new RuntimeException(e);}
  }

  /** @throws CameraAccessException if the camera device is no longer connected */
  private void init() throws CameraAccessException {
    final CaptureRequestCallback instance = mHolder != null ? this : null;
    final Handler handler = mHolder != null ? mHolder.handler : null;
    if (!mRepeating) mSession.capture(mRequest, instance, handler);
    else mSession.setRepeatingRequest(mRequest, instance, handler);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("ConstantConditions")
  @Override public final void close() {
    if (mClosed) return;
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void onCaptureStarted
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
      long timestamp, long frameNumber) {
    super.onCaptureStarted(session, request, timestamp, frameNumber);
    if (mHolder != null && (mSession != session || this.mRequest != request)) return;
    if (mHolder != null) mHolder.callback.onStarted(timestamp, frameNumber);
  }

  /** {@inheritDoc} */
  @Override public final void onCaptureProgressed
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
      @NonNull CaptureResult partialResult) {
    super.onCaptureProgressed(session, request, partialResult);
    if (mHolder != null && (mSession != session || this.mRequest != request)) return;
    if (mHolder != null) mHolder.callback.onProgressed(partialResult);
  }

  /** {@inheritDoc} */
  @Override public final void onCaptureCompleted
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
      @NonNull TotalCaptureResult result) {
    super.onCaptureCompleted(session, request, result);
    if (mHolder != null && (mSession != session || this.mRequest != request)) return;
    if (mHolder != null) mHolder.callback.onCompleted(result);
  }

  /** {@inheritDoc} */
  @Override public final void onCaptureFailed
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
      @NonNull CaptureFailure failure) {
    super.onCaptureFailed(session, request, failure);
    if (mHolder != null && (mSession != session || this.mRequest != request)) return;
    if (mHolder != null) mHolder.callback.onFailed(failure);
  }

  /** {@inheritDoc} */
  @Override public final void onCaptureSequenceCompleted
  (@NonNull CameraCaptureSession session, int sequenceId, long frameNumber)
  {super.onCaptureSequenceCompleted(session, sequenceId, frameNumber);}

  /** {@inheritDoc} */
  @Override public final void onCaptureSequenceAborted
  (@NonNull CameraCaptureSession session, int sequenceId)
  {super.onCaptureSequenceAborted(session, sequenceId);}

  /** {@inheritDoc} */
  @Override public final void onCaptureBufferLost
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
      @NonNull Surface target, long frameNumber)
  {super.onCaptureBufferLost(session, request, target, frameNumber);}

  /**
   * Create a {@link Builder} for repeatable captures.
   *
   * @param session   A configured capture session
   * @param request   An immutable package of settings and outputs
   *
   * @return a {@link CaptureRequestCallback} instance
   */
  @NonNull static CaptureRequestCallback.Builder repeating
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request)
  {final boolean repeating = true; return new Builder(session, request, repeating);}

  /**
   * Create a {@link Builder} for disposable captures.
   *
   * @param session   A configured capture session
   * @param request   An immutable package of settings and outputs
   *
   * @return a {@link CaptureRequestCallback} instance
   */
  @NonNull static CaptureRequestCallback.Builder disposable
  (@NonNull CameraCaptureSession session, @NonNull CaptureRequest request)
  {final boolean repeating = false; return new Builder(session, request, repeating);}


  /** {@link CaptureRequestCallback}'s {@link Callback}. */
  interface Callback {

    /**
     * @param timestamp the timestamp at start of capture for a regular mRequest,
     *                  or the timestamp at the input image's start of capture
     *                  for a reprocess mRequest, in nanoseconds.
     * @param frameNumber the frame number for this capture
     */
    void onStarted(long timestamp, long frameNumber);

    /**
     * @param partialResult The partial output metadata from the capture, which
     *                      includes a subset of the {@link TotalCaptureResult}
     *                      fields.
     */
    void onProgressed(@NonNull CaptureResult partialResult);

    /**
     * @param result The total output metadata from the capture, including the
     *               final capture parameters and the state of the camera system
     *               during capture.
     */
    void onCompleted(@NonNull TotalCaptureResult result);

    /**
     * @param failure The output failure from the capture, including the failure
     *                reason and the frame number.
     */
    void onFailed(@NonNull CaptureFailure failure);
  }

  /** Contains non-null {@link Callback} and {@link Handler}. */
  private static final class CallbacksHolder {

    /** {@link Callback} instance. */
    final Callback callback;

    /** {@link Handler} instance. */
    final Handler handler;

    /**
     * Constructs a new {@link CallbacksHolder}.
     *
     * @param callback {@link Callback} instance
     * @param handler {@link Handler} instance
     */
    CallbacksHolder(@NonNull Callback callback, @NonNull Handler handler)
    {this.callback = callback; this.handler = handler;}

  }

  /**
   * Used to add parameters to a {@link CaptureRequestCallback}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link CaptureRequestCallback} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  static final class Builder {

    /** A configured capture session. */
    final CameraCaptureSession session;
    /** An immutable package of settings and outputs. */
    final CaptureRequest request;
    /** "REPEATING" or "DISPOSABLE" mode. */
    final boolean repeating;

    /** Callbacks holder. */
    @Nullable CallbacksHolder holder = null;

    /**
     * Constructs a new {@link Builder}.
     *
     * @param session   A configured capture session
     * @param request   An immutable package of settings and outputs
     * @param repeating "REPEATING" or "DISPOSABLE" mode of capturing
     */
    private Builder(@NonNull CameraCaptureSession session,
        @NonNull CaptureRequest request, boolean repeating)
    {this.session = session; this.request = request; this.repeating = repeating;}

    /** Create a {@link CaptureRequestCallback} from this {@link Builder}. */
    @NonNull final CaptureRequestCallback build()
    {final Builder builder = this; return new CaptureRequestCallback(builder); }

    /**
     * @param callback {@link Callback} instance
     * @param handler {@link Handler} instance
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull final Builder callback(@NonNull Callback callback, @NonNull Handler handler)
    {holder = new CallbacksHolder(callback, handler); return this;}
  }
}

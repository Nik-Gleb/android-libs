/*
 * CaptureRequestManager.java
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
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.MediaActionSound;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import camera.AndroidCameraTools.CaptureCallbackOptions;
import camera.AndroidCameraTools.CaptureRequestBuilder;

import static android.media.MediaActionSound.SHUTTER_CLICK;
import static android.media.MediaActionSound.START_VIDEO_RECORDING;
import static android.media.MediaActionSound.STOP_VIDEO_RECORDING;
import static camera.AndroidCameraTools.CaptureRequestType.CAPTURE;
import static camera.AndroidCameraTools.CaptureRequestType.NONE;
import static camera.AndroidCameraTools.CaptureRequestType.PREVIEW;
import static camera.AndroidCameraTools.CaptureRequestType.RECORD;
import static camera.AndroidCameraTools.CaptureRequestType.SNAPSHOT;
import static camera.CaptureRequestCallback.disposable;
import static camera.CaptureRequestCallback.repeating;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 26/03/2018
 */
final class CaptureRequestManager
    implements CaptureSessionCallback.Callback {

  /** Play camera sounds. */
  private static final boolean SOUNDS = false;

  /** Media Action Sound. */
  private final MediaActionSound mSound = new MediaActionSound()
  {{load(START_VIDEO_RECORDING); load(STOP_VIDEO_RECORDING); load(SHUTTER_CLICK);}};

  /** This instance. */
  private final CaptureRequestManager mInstance = this;

  /** Request callbacks. */
  private final RequestCallback
      mPreviewCallback  = new RequestCallback(mInstance, PREVIEW),
      mRecordCallback   = new RequestCallback(mInstance, RECORD),
      mCaptureCallback  = new RequestCallback(mInstance, CAPTURE),
      mSnapshotCallback = new RequestCallback(mInstance, SNAPSHOT);

  /** Capture controller. */
  private final Consumer<Consumer<Boolean>> mController;
  /** Capture configurator. */
  private final BiConsumer<Integer, CaptureRequestBuilder> mConfigurator;
  /** State listener. */
  private final IntConsumer mListener;
  /** Disposable captures callback. */
  private final CaptureCallbackOptions mOptions;
  /** Main handler. */
  private final Handler mHandler;

  /** CameraInstance capture session. */
  private final CameraCaptureSession mSession;


  /** Capture Requests. */
  private final CaptureRequest
      mPreview, mRecord, mCapture, mSnapshot;

  /** Current states. */
  private boolean
      mClosed = false,
      mActive = false,
      mRecording = false;

  /** Current capturing captureCallback. */
  @Nullable private CaptureRequestCallback mCurrent = null;

  private CaptureRequestManager
      (@NonNull Builder builder, @NonNull CameraCaptureSession session) {
    mSession = session; mHandler = builder.handler;
    mOptions = builder.options; mListener = builder.listener;
    mController = builder.controller;
    if (mController != null) mController.accept(this::onClicked);
    mConfigurator = builder.configurator;
    final int size = builder.targets.size();
    if (size < 2) throw new IllegalArgumentException();
    final int lastIndex = size - 1;

    final List<Surface> disposable = builder.targets.subList(lastIndex, size);
    final List<Surface> repeatable = builder.targets.subList(0, lastIndex);

    final CaptureRequestManager instance = this;
    final CameraDevice device = mSession.getDevice();

    try {
      mPreview =
          new CaptureRequestOptions(PREVIEW,
              new RequestBuilder(instance, PREVIEW),
              repeatable).build(device);
      mRecord =
          new CaptureRequestOptions(RECORD,
              new RequestBuilder(instance, RECORD),
              repeatable).build(device);
      mCapture =
          new CaptureRequestOptions(CAPTURE,
              new RequestBuilder(instance, CAPTURE),
              disposable).build(device);
      mSnapshot = !builder.snapshot ? null :
          new CaptureRequestOptions(SNAPSHOT,
              new RequestBuilder(instance, SNAPSHOT),
              disposable).build(device);
    } catch (CameraAccessException exception)
    {throw new RuntimeException(exception);}

    invalidate();

  }

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mCurrent == null) return;
    if (mController != null) {
      final Consumer<Boolean> listener = null;
      mController.accept(listener);
    } mClosed = true; invalidate(); mSound.release();
  }

  /** {@inheritDoc} */
  @Override public final void onActivated()
  {if (mActive) return; mActive = true; invalidate();}

  /** {@inheritDoc} */
  @Override public final void onDeActivated()
  {if (!mActive) return; mActive = false; invalidate();}

  /** @param record true if "RECORD" was clicked, otherwise - "PREVIEW" */
  private void onClicked(boolean record)
  {if (record) onRepeatableClicked(); else onDisposableClicked();}

  /** Repeatable clicked. */
  private void onRepeatableClicked() {
    if (mClosed) return; mRecording = !mRecording;
    if (SOUNDS && !mRecording) mSound.play(STOP_VIDEO_RECORDING);
    try {mSession.stopRepeating();}
    catch (CameraAccessException e)
    {throw new RuntimeException(e);}
  }

  /** Disposable clicked. */
  private void onDisposableClicked() {
    if (mClosed) return;
    final boolean snapshot = mRecording && mSnapshot != null;
    final CaptureRequest request = snapshot ? mSnapshot : mCapture;
    final CaptureRequestCallback.Callback callback =
        snapshot ? mSnapshotCallback : mCaptureCallback;
    apply(disposable(mSession, request), mOptions, callback, mHandler).build();
    if (SOUNDS) mSound.play(SHUTTER_CLICK);
  }

  /**
   * Apply capture request options.
   *
   * @param builder capture request builder
   * @param options capture request options
   * @param callback request callback
   * @param handler request handler
   *
   * @return capture request builder
   */
  @NonNull private static CaptureRequestCallback.Builder apply
      (@NonNull CaptureRequestCallback.Builder builder,
          @Nullable CaptureCallbackOptions options,
          @NonNull CaptureRequestCallback.Callback callback,
          @NonNull Handler handler)
  {return options == null ||
      (builder.repeating && !options.repeatable) ?
      builder : builder.callback(callback, handler);}

  /** Invalidate current state. */
  private void invalidate() {
    if (!mActive) {
      if (mCurrent != null) mCurrent.close();
      mCurrent = null; mClosed = false;
      if (mListener != null) mListener.accept(NONE);
      final CaptureRequest request = mRecording ? mRecord : mPreview;
      final CaptureRequestCallback.Callback callback =
          mRecording ? mRecordCallback : mPreviewCallback;
      apply(repeating(mSession, request), mOptions, callback, mHandler).build();
    } else {
      if (mListener != null) mListener.accept(mRecording ? RECORD : PREVIEW);
      if (SOUNDS && mRecording) mSound.play(START_VIDEO_RECORDING);
    }
  }

  /**
   * @param type capture type
   * @param timestamp the timestamp at start of capture for a regular mCurrent,
   *                  or the timestamp at the input image's start of capture
   *                  for a reprocess mCurrent, in nanoseconds.
   * @param frameNumber the frame number for this capture
   */
  private void started
  (int type, long timestamp, long frameNumber)
  {if (mOptions != null) mOptions.callback.started(type, timestamp, frameNumber);}

  /**
   * @param type capture type
   * @param partialResult The partial output metadata from the capture, which
   *                      includes a subset of the {@link TotalCaptureResult}
   *                      fields.
   */
  private void progressed
  (int type, @NonNull CaptureResult partialResult)
  {if (mOptions != null) mOptions.callback.progressed(type, partialResult);}

  /**
   * @param type capture type
   * @param result The total output metadata from the capture, including the
   *               final capture parameters and the state of the camera system
   *               during capture.
   */
  private void completed
  (int type, @NonNull TotalCaptureResult result)
  {if (mOptions != null) mOptions.callback.completed(type, result);}

  /**
   * @param type capture type
   * @param failure The output failure from the capture, including the failure
   *                reason and the frame number.
   */
  private void failed
  (int type, @NonNull CaptureFailure failure)
  {if (mOptions != null) mOptions.callback.failed(type, failure);}

  /**
   * @param type capture type
   * @param builder capture builder
   */
  private void configure
  (int type, @NonNull CaptureRequestBuilder builder)
  {if (mConfigurator != null) mConfigurator.accept(type, builder);}

  /**
   * Create a {@link Builder}.
   *
   * @param targets   An output targets
   * @param handler   Main handler
   *
   * @return a {@link CaptureRequestCallback} instance
   */
  @NonNull static Builder create(@NonNull List<Surface> targets, @NonNull Handler handler)
  {return new Builder(targets,handler);}

  /** Internal Request CaptureCallback. */
  private static final class RequestCallback
      implements CaptureRequestCallback.Callback {

    /** The manager instance. */
    private final CaptureRequestManager mManager;

    /** Capture mCurrent type. */
    private final int mType;

    /**
     * Constructs a new {@link RequestBuilder}.
     *
     * @param manager capture mCurrent manager instance
     * @param type capture mCurrent type
     */
    RequestCallback
    (@NonNull CaptureRequestManager manager, int type)
    {mManager = manager; mType = type;}

    /** {@inheritDoc} */
    @Override public final void onStarted
    (long timestamp, long frameNumber)
    {mManager.started(mType, timestamp, frameNumber);}

    /** {@inheritDoc} */
    @Override public final void onProgressed
    (@NonNull CaptureResult partialResult)
    {mManager.progressed(mType, partialResult);}

    /** {@inheritDoc} */
    @Override public final void onCompleted
    (@NonNull TotalCaptureResult result)
    {mManager.completed(mType, result);}

    /** {@inheritDoc} */
    @Override public final void onFailed
    (@NonNull CaptureFailure failure)
    {mManager.failed(mType, failure);}
  }

  /** Internal Request CameraDeviceBuilder. */
  private static final class RequestBuilder
      implements Consumer<CaptureRequestBuilder> {

    /** The manager instance. */
    private final CaptureRequestManager mManager;

    /** Capture mCurrent type. */
    private final int mType;

    /**
     * Constructs a new {@link RequestBuilder}.
     *
     * @param manager capture mCurrent manager instance
     * @param type capture mCurrent type
     */
    RequestBuilder
    (@NonNull CaptureRequestManager manager,
        int type)
    {mManager = manager; mType = type;}

    /** {@inheritDoc} */
    @Override public final void accept
    (CaptureRequestBuilder builder)
    {mManager.configure(mType, builder);}
  }

  /**
   * Used to add parameters to a {@link CaptureRequestManager}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link CaptureRequestManager} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  static final class Builder {

    /** An output targets. */
    final List<Surface> targets;
    /** Main handler. */
    final Handler handler;

    /** Capture controller. */
    @Nullable Consumer<Consumer<Boolean>> controller = null;
    /** Capture configurator. */
    @Nullable BiConsumer<Integer, CaptureRequestBuilder> configurator = null;
    /** State listener. */
    @Nullable IntConsumer listener = null;
    /** Capture callback options. */
    @Nullable CaptureCallbackOptions options = null;

    /** Snapshot allowed. */
    boolean snapshot = false;

    /**
     * Constructs a new {@link Builder}.
     *
     * @param targets   An output targets
     * @param handler   Main handler
     */
    private Builder(@NonNull List<Surface> targets, @NonNull Handler handler)
    {this.targets = targets; this.handler = handler;}

    /** Create a {@link CaptureRequestManager} from this {@link Builder}. */
    @NonNull final CaptureRequestManager build(@NonNull CameraCaptureSession session)
    {final Builder builder = this; return new CaptureRequestManager(builder, session);}
  }




}

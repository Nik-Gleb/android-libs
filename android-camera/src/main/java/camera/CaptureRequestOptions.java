/*
 * CaptureRequestOptions.java
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

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.util.List;
import java.util.function.Consumer;

import camera.AndroidCameraTools.CaptureRequestBuilder;

/**
 * Capture Request Options.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 26/03/2018
 */
final class CaptureRequestOptions {

  /** The targets. */
  @SuppressWarnings("WeakerAccess")
  final List<Surface> surfaces;

  /** CaptureRequestCallback Request Tuner. */
  private final Consumer<CaptureRequestBuilder> mTuner;

  /** CaptureRequestCallback Template. */
  private final int mTemplate;

  /**
   * Constructs a new {@link CaptureRequestOptions}.
   *
   * @param surfaces the target surfaces
   * @param tuner request options tuner
   * @param template template of request
   */
  CaptureRequestOptions (int template,
      @NonNull Consumer<CaptureRequestBuilder> tuner,
      @NonNull List<Surface> surfaces)
  {this.surfaces = surfaces; mTuner = tuner; mTemplate = template;}

  /**
   * @param device camera device
   *
   * @return captureTargets request instance
   *
   * @throws CameraAccessException create request failed
   */
  @NonNull public CaptureRequest build
  (@NonNull CameraDevice device) throws CameraAccessException {
    final CaptureRequest.Builder builder =
        device.createCaptureRequest(mTemplate);
    //builder.setTag(mTemplate);
    mTuner.accept(new CaptureRequestBuilderImpl(builder));
    surfaces.forEach(builder::addTarget);return builder.build();
  }


  /** CaptureRequestCallback Request CameraDeviceBuilder Implementation. */
  private static final class CaptureRequestBuilderImpl
      implements CaptureRequestBuilder {

    /** Request CameraDeviceBuilder. */
    private final CaptureRequest.Builder mBuilder;

    /**
     * Constructs a new {@link CaptureRequestBuilderImpl}.
     *
     * @param builder request builder
     */
    CaptureRequestBuilderImpl(CaptureRequest.Builder builder)
    {mBuilder = builder;}

    /** {@inheritDoc} */
    @Override public final <T> void set
    (@NonNull CaptureRequest.Key<T> key, @Nullable T value)
    {mBuilder.set(key, value);}

    /** {@inheritDoc} */
    @Nullable @Override public final <T> T get
    (@NonNull CaptureRequest.Key<T> key)
    {return mBuilder.get(key);}
  }

}

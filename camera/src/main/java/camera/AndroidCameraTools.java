/*
 * AndroidCameraTools.java
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
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;
import android.view.Surface;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Camera Tools Helper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 28/03/2018
 */
@SuppressWarnings("WeakerAccess")
public final class AndroidCameraTools {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private AndroidCameraTools() {throw new AssertionError();}

  /**
   * @param manager android camera manager
   * @param handler main handler
   * @param callback availability callback
   *
   * @return camera availability manager
   */
  @SuppressWarnings("unused")
  @NonNull public static Closeable availability(@NonNull CameraManager manager,
      @NonNull Handler handler, @NonNull BiConsumer<String, Boolean> callback)
  {return new CameraAvailabilityManager(manager, handler, callback);}

  /**
   * CameraDeviceError codes.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 21/03/2018
   */
  @IntDef({
      CameraDeviceError.NO_CAMERA_ERRORS,
      CameraDeviceError.ERROR_CAMERA_IN_USE,
      CameraDeviceError.ERROR_MAX_CAMERAS_IN_USE,
      CameraDeviceError.ERROR_CAMERA_DISABLED,
      CameraDeviceError.ERROR_CAMERA_DEVICE,
      CameraDeviceError.ERROR_CAMERA_SERVICE
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface CameraDeviceError {

    /** Нормальное закрытие камеры.*/
    @SuppressWarnings("WeakerAccess")
    int NO_CAMERA_ERRORS = 0;

    /**
     * Устройство камеры уже используется.
     *
     * <p>
     * Может возникать при сбое в работе камеры из-за того, что камера уже
     * используется клиентом Camera API с более высоким приоритетом.
     * </p>
     */
    @SuppressWarnings("WeakerAccess")
    int ERROR_CAMERA_IN_USE = 1;

    /**
     * Устройство камеры не может быть открыто, потому что слишком много других
     * уже открытых камер.
     *
     * <p>
     * Достигнуто общесистемное ограничение на количество одновременно открытых
     * камер, и больше устройств камеры открыть невозможно, пока не будут закрыты
     * предыдущие экземпляры.
     * </p>
     */
    @SuppressWarnings("WeakerAccess")
    int ERROR_MAX_CAMERAS_IN_USE = 2;

    /**
     * Устройство камеры нельзя открыть из-за ограничений в "политике устройства".
     *
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled(android.content.ComponentName, boolean)
     */
    @SuppressWarnings("WeakerAccess")
    int ERROR_CAMERA_DISABLED = 3;

    /**
     * Устройство камеры обнаружило фатальный сбой.
     *
     * <p>Чтобы снова использовать устройство камеры, необходимо снова открыть
     * её.</p>
     */
    @SuppressWarnings("WeakerAccess")
    int ERROR_CAMERA_DEVICE = 4;

    /**
     * Устройство камеры обнаружило фатальный сбой.
     *
     * <p>Возможно потребуется отключить и перезагрузить Android-устройство,
     * для восстановления функций камеры, или имеет место быть постоянная
     * аппаратная проблема.</p>
     *
     * <p>Восстановление может быть возможно по закрытию CameraDevice и
     * CameraManager, и попытке снова запустить все ресурсы с нуля..</p>
     */
    @SuppressWarnings("WeakerAccess")
    int ERROR_CAMERA_SERVICE = 5;

  }

  /**
   * Used to add parameters to a {@link CameraDeviceManager}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link CameraDeviceBuilder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link CameraDeviceManager} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static final class CameraDeviceBuilder {

    /** Main handler. */
    final Handler handler;
    /** Surfaces provider. */
    final Consumer<Consumer<List<Surface>>> surfaces;

    /** Capture controller. */
    @Nullable public Consumer<Consumer<Boolean>> controller = null;
    /** Capture configurator. */
    @Nullable public BiConsumer<Integer, CaptureRequestBuilder> configurator = null;
    /** State listener. */
    @Nullable public Consumer<Integer> listener = null;
    /** Disposable captures callback. */
    @Nullable public CaptureCallbackOptions options = null;

    /** Snapshot allowed. */
    public boolean snapshot = false;

    /**
     * Constructs a new {@link CameraDeviceBuilder}.
     *
     * @param handler   Main handler
     * @param surfaces  Surfaces provider
     */
    private CameraDeviceBuilder
    (@NonNull Handler handler, @NonNull Consumer<Consumer<List<Surface>>> surfaces)
    {this.handler = handler; this.surfaces = surfaces;}

    /**
     * Create a {@link CameraDeviceManager} from this {@link CameraDeviceBuilder}.
     *
     * @param device camera device instance
     * @return the builder instance
     */
    @NonNull private CameraDeviceManager build(@NonNull CameraDevice device)
    {final CameraDeviceBuilder builder = this; return new CameraDeviceManager(builder, device);}

    /**
     * @param manger camera manager
     * @param cameraId camera id
     * @param builder camera device manager builder
     *
     * @return new created camera device manager
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(android.Manifest.permission.CAMERA)
    @NonNull public static Closeable create(@NonNull CameraManager manger,
        @NonNull String cameraId, @NonNull CameraDeviceBuilder builder)
    {return new CameraDeviceCallback
        (manger, cameraId, builder::build, builder.handler);}

    /**
     * @param handler main handler
     * @param surfaces surfaces provider
     *
     * @return new created camera device manager builder
     */
    @NonNull public static CameraDeviceBuilder create(@NonNull Handler handler,
        @NonNull Consumer<Consumer<List<Surface>>> surfaces)
    {return new CameraDeviceBuilder(handler, surfaces);}
  }

  /**
   * Capture Request CameraDeviceBuilder.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 23/03/2018
   */
  @SuppressWarnings("unused")
  public interface CaptureRequestBuilder {

    /**
     * Set a captureTargets request field to a value. The field definitions can be
     * found in {@link CaptureRequest}.
     *
     * <p>Setting a field to {@code null} will remove that field from the captureTargets request.
     * Unless the field is optional, removing it will likely produce an error from the camera
     * device when the request is submitted.</p>
     *
     * @param key The metadata field to write.
     * @param value The value to set the field to, which must be of a matching
     * type to the key.
     */
    <T> void set(@NonNull CaptureRequest.Key<T> key, @Nullable T value);

    /**
     * Get a captureTargets request field value. The field definitions can be
     * found in {@link CaptureRequest}.
     *
     * @throws IllegalArgumentException if the key was not valid
     *
     * @param key The metadata field to read.
     * @return The value of that key, or {@code null} if the field is not set.
     */
    @Nullable <T> T get(@NonNull CaptureRequest.Key<T> key);

  }

  /** Capture Callbacks Options. */
  public static final class CaptureCallbackOptions {

    /** Disposable captures callback. */
    final CaptureCallback callback;
    /** Repeatable callback mode. */
    final boolean repeatable;

    /**
     * Constructs a new {@link CaptureCallbackOptions}.
     *
     * @param callback disposable captures callback
     * @param repeatable repeatable callback mode
     */
    public CaptureCallbackOptions
    (@NonNull CaptureCallback callback, boolean repeatable)
    {this.callback = callback; this.repeatable = repeatable;}
  }


  /** External Capture Request Manager. */
  public interface CaptureCallback {

    /**
     * @param type capture type
     * @param timestamp the timestamp at start of capture for a regular mCurrent,
     *                  or the timestamp at the input image's start of capture
     *                  for a reprocess mCurrent, in nanoseconds.
     * @param frameNumber the frame number for this capture
     */
    @SuppressWarnings("unused") default void started
    (int type, long timestamp, long frameNumber) {}

    /**
     * @param type capture type
     * @param partialResult The partial output metadata from the capture, which
     *                      includes a subset of the {@link TotalCaptureResult}
     *                      fields.
     */
    @SuppressWarnings("unused") default void progressed
    (int type, @NonNull CaptureResult partialResult) {}

    /**
     * @param type capture type
     * @param result The total output metadata from the capture, including the
     *               final capture parameters and the state of the camera system
     *               during capture.
     */
    @SuppressWarnings("unused") default void completed
    (int type, @NonNull TotalCaptureResult result) {}

    /**
     * @param type capture type
     * @param failure The output failure from the capture, including the failure
     *                reason and the frame number.
     */
    @SuppressWarnings("unused") default void failed
    (int type, @NonNull CaptureFailure failure) {}

  }

  /**
   * Типы захват-запросов.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 24/03/2018
   */
  @IntDef({
      CaptureRequestType.NONE,
      CaptureRequestType.PREVIEW,
      CaptureRequestType.RECORD,
      CaptureRequestType.CAPTURE,
      CaptureRequestType.SNAPSHOT})
  @Retention(RetentionPolicy.SOURCE)
  public @interface CaptureRequestType {

    /** Захват приостановлен. */
    int NONE   = 0;

    /**
     * Предпочтительный режим для превью камеры.
     * <p>
     * В частности это означает, что частота кадров будет
     * приоритетнее качества их пост-обработки.
     * <p>
     * Гарантировано поддерживается на всех устройствах.
     */
    int PREVIEW   = CameraDevice.TEMPLATE_PREVIEW;

    /**
     * Предпочтительный режим для видео-записи.
     * <p>
     * В частности это означает, что частота кадров будет
     * использоваться стабильная частота кадров.
     * Пост-обработка будет установлена в "запись"-качество.
     * <p>
     * Гарантировано поддерживается на всех устройствах.
     */
    int RECORD    = CameraDevice.TEMPLATE_RECORD;

    /**
     * Предпочтительный режим для статичных изображений.
     * <p>
     * В частности это означает, что качество изображений будет
     * приоритетнее частоты кадров.
     * <p>
     * Гарантировано поддерживается на всех устройствах.
     */
    int CAPTURE   = CameraDevice.TEMPLATE_STILL_CAPTURE;

    /**
     * Предпочтительный режим для захвата изображений в процессе записи видео.
     * <p>
     * В частности это означает, максимизацию качества изображения без
     * необходимости прерывания текущей видео записи.
     * <p>
     * Гарантировано поддерживается на всех устройствах, кроме устаревших
     * ({@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL}
     * {@code == }{@link CameraMetadata#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY LEGACY})
     * и {@link CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT DEPTH_OUTPUT}
     * устройств, которые не
     * {@link CameraMetadata#REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE BACKWARD_COMPATIBLE}).
     */
    int SNAPSHOT  = CameraDevice.TEMPLATE_VIDEO_SNAPSHOT;
  }
}

package camera;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArraySet;
import android.view.Surface;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import camera.AndroidCameraTools.CameraDeviceBuilder;
import camera.AndroidCameraTools.CaptureRequestBuilder;
import camera.CameraProfile.Size;

import static android.hardware.camera2.CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL;
import static android.hardware.camera2.CameraCharacteristics.LENS_FACING;
import static android.hardware.camera2.CameraCharacteristics.SENSOR_ORIENTATION;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
import static android.hardware.camera2.CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_EXTERNAL;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;
import static camera.AndroidCameraTools.availability;
import static java.util.Arrays.sort;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingLong;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static camera.CameraProfile.Rotation.ROTATION_000;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 10/09/2018
 */
@SuppressWarnings("unused")
public abstract class CameraInstance {

  /** CameraProfile data. */
  @NonNull public final CameraProfile profile;

  /** Surfaces consumer. */
  @Nullable private final Consumer<List<Surface>> mSurfaces;

  /**
   * Constructs a new {@link CameraInstance}.
   *
   * @param profile profile data
   * @param surfaces surfaces consumer
   */
  private CameraInstance(@NonNull CameraProfile profile,
    @Nullable Consumer<List<Surface>> surfaces)
  {this.profile = profile; mSurfaces = surfaces;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof CameraInstance)) return false;
    final CameraInstance that = (CameraInstance) obj;
    return Objects.equals(profile, that.profile) &&
      Objects.equals(mSurfaces, that.mSurfaces);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return profile.hashCode();}

  /** {@inheritDoc} */
  @Override @NonNull public final String toString()
  {return profile.toString();}

  /** Get next profile. */
  public abstract void next();

  /** Get previous profile. */
  public abstract void prev();

  /** @param surfaces outputs. */
  public final void set
  (@Nullable List<Surface> surfaces)
  {if (mSurfaces != null) mSurfaces.accept(surfaces);}

  /** @return true if instance is empty */
  public boolean isEmpty()
  {return mSurfaces == null || profile.isEmpty();}

  /**
   * @param manager {@link CameraManager} instance
   *
   * @return Builder of {@link CameraInstance}' source
   */
  @NonNull public static Builder
  front(@NonNull CameraManager manager)
  {return new Builder(manager, true);}

  /**
   * @param manager {@link CameraManager} instance
   *
   * @return Builder of {@link CameraInstance}' source
   */
  @NonNull public static Builder
  back(@NonNull CameraManager manager)
  {return new Builder(manager, false);}


  /**
   * Used to add parameters to a {@link CameraInstance}.
   * <p>
   * The where methods can then be used to add parameters to the builder.
   * See the specific methods to find for which {@link Builder} type each is
   * allowed.
   * Call {@link #build} to flat the {@link CameraInstance} once all the
   * parameters have been supplied.
   */
  @SuppressWarnings({ "unused", "WeakerAccess" })
  public static final class Builder {

    /** CameraInstance manager. */
    @NonNull private final CameraManager mManager;

    /** Front first */
    private final boolean mFront;

    /** Selector function. */
    @Nullable private BiFunction<ArraySet<CameraProfile>,
      Integer, Selected> mSelector = null;

    /** Updater function. */
    @Nullable private  BiFunction<ArraySet<CameraProfile>,
      Selected, Selected> mUpdater = null;

    /** Capture controller. */
    @Nullable private Consumer<Consumer<Boolean>> mController = null;

    /** Capture configurator. */
    @Nullable private BiConsumer<Integer, CaptureRequestBuilder> mConfigurator = null;

    /** State listener. */
    @Nullable private IntConsumer mListener = null;

    /** Main handler. */
    @Nullable private Handler mHandler = null;

    /** Update throttle. */
    @IntRange(from = 0) private int mThrottle = 0;

    /**
     * Constructs a new {@link Builder}.
     *
     * @param manager camera manager
     */
    private Builder(@NonNull CameraManager manager, boolean front)
    {mManager = manager; mFront = front;}

    /**
     * @param value selector function
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder selector
    (@NonNull BiFunction<ArraySet<CameraProfile>, Integer, Selected> value)
    {mSelector = value; return this;}

    /**
     * @param value updater function
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder updater
    (@NonNull BiFunction<ArraySet<CameraProfile>, Selected, Selected> value)
    {mUpdater = value; return this;}

    /**
     * @param value main handler
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull public final
    Builder handler(@NonNull Handler value)
    {mHandler = value; return this;}

    /**
     * @param value camera controller
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder controller
    (@NonNull Consumer<Consumer<Boolean>> value)
    {mController = value; return this;}

    /**
     * @param value camera configurator
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder configurator
    (@NonNull BiConsumer<Integer, CaptureRequestBuilder> value)
    {mConfigurator = value; return this;}

    /**
     * @param value camera listener
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder listener
    (@NonNull IntConsumer value)
    {mListener = value; return this;}

    /**
     * @param value updates throttle
     *
     * @return this builder, to allow for chaining.
     */
    @NonNull public final Builder throttle
    (@IntRange(from = 12, to = 24) int value)
    {mThrottle = value; return this;}

    /**
     * Create a {@link CameraInstance}'s source  from this {@link Builder}.
     *
     * @param sink data-sink
     *
     * @return disposable source
     */
    @NonNull public final Runnable build
    (@NonNull Consumer<CameraInstance> sink) {
      mHandler = mHandler != null ?
        new Handler(mHandler.getLooper()) : new Handler();
      return build(mManager, mHandler, this::create,
        mController, mConfigurator, mListener, sink);
    }

    /** */
    @SuppressWarnings("unchecked")
    @NonNull private Runnable create(@NonNull Consumer<CameraInstance> sink) {

      final AtomicBoolean closed = new AtomicBoolean(false);
      mUpdater = mUpdater != null ? mUpdater : (profiles, selected) -> null;
      mSelector = mSelector != null ? mSelector : (profiles, integer) -> null;

      final Consumer<Selected>[] setCurrent = new Consumer[1];
      final Selected[] currents = new Selected[1];
      final ArraySet<CameraProfile>[] profiles = new ArraySet[1];

      final Consumer<Boolean> moves = forward -> {
        final Runnable task = () -> {
          if (profiles[0] != null && profiles[0].size() > 1) {
            final int index = (requireNonNull
              (currents[0]).index + (forward ? 1 : -1)) % profiles[0].size();
            setCurrent[0].accept(mSelector.apply(profiles[0],
              index < 0 ? profiles[0].size() + index : index));
          }
        };
        if (requireNonNull(mHandler).getLooper().isCurrentThread()) task.run();
        else if (!mHandler.post(task)) throw new IllegalStateException();
      };

      setCurrent[0] = current -> {
        currents[0] = current;
        final CameraProfile profile = current != null ?
          current.profile : CameraProfile.EMPTY;
        if (closed.get()) return;
        sink.accept(new CameraInstance(profile, null) {
          @Override public final void next()
          {moves.accept(true);}
          @Override public final void prev()
          {moves.accept(false);}
        });
      };

      final Consumer<Handler> notifier = target -> {
        if (mThrottle == 0) Message.obtain(target).sendToTarget();
        else target.sendMessageDelayed(Message.obtain(), mThrottle);
      };

      final Looper looper = requireNonNull(mHandler).getLooper();
      final Handler inner = new Handler(looper, msg -> {
        try {
          final ArraySet<CameraProfile> profileSet =
            stream(mManager.getCameraIdList())
              .map(id -> createProfile(mManager, id, mFront))
              .collect(toCollection(ArraySet::new));
          if (!looper.getQueue().isIdle()) notifier.accept(msg.getTarget());
          else setCurrent[0].accept(mUpdater.apply(profiles[0] =
            toArraySet(profileSet), currents[0]));
        } catch (CameraAccessException ignored) {}
        return true;
      });

      final Closeable closeable = availability(mManager, new Handler(mHandler.getLooper(),
          msg -> mHandler.postAtFrontOfQueue(msg.getCallback())), (id, available) -> {
          if (/*available && */!inner.hasMessages(0)) notifier.accept(inner);
        }
      );

      return () -> {
        if (closed.get()) return;
        try {closeable.close();}
        catch (IOException exception)
        {throw new RuntimeException(exception);}
        finally {closed.set(true);}
      };
    }

    @SuppressWarnings("unchecked")
    @NonNull private static Runnable build
      (@NonNull CameraManager manager, @NonNull Handler handler,
        @NonNull Function<Consumer<CameraInstance>, Runnable> source,
        @Nullable Consumer<Consumer<Boolean>> controller,
        @Nullable BiConsumer<Integer, CaptureRequestBuilder> configurator,
        @Nullable IntConsumer listener, @NonNull Consumer<CameraInstance> sink) {

      final BiFunction<Consumer<List<Surface>>, CameraInstance, CameraInstance>
        factory = (consumer, model) ->
          new CameraInstance(model.profile, consumer) {
            @Override public void next()
            {model.next();}
            @Override public void prev()
            {model.prev();}
          };

      final Supplier<Closeable>[] device = new Supplier[1];

      @SuppressLint("MissingPermission")
      final Consumer<CameraInstance> consumer = model -> {

        if (device[0] != null) {
          final Closeable camera = device[0].get();
          if (camera != null) {
            if (Objects.equals(camera.toString(), model.profile.id)) return;
            try {camera.close();} catch (IOException exception)
            {throw new RuntimeException(exception);}
          } device[0] = null;
        }

        if (!model.profile.isEmpty())
          try {
            final CameraDeviceBuilder builder =
              CameraDeviceBuilder.create
              (handler, surfaces -> sink.accept(factory.apply(surfaces, model)));

            if (controller != null)   builder.controller = controller;
            if (configurator != null) builder.configurator = configurator;
            if (listener != null)     builder.listener = listener;

            device[0] =
              CameraDeviceBuilder.create
              (manager, model.profile.id, builder);
          } catch (RuntimeException exception) {
            if (exception.getCause() instanceof
              CameraAccessException) device[0] = null;
          }
        else
          sink.accept(
            new CameraInstance(CameraProfile.EMPTY, null)
            {@Override public void next() {}
            @Override public void prev() {}}
          );
      };

      final Runnable dispose = source.apply(consumer);
      return () -> {
        dispose.run();
        ofNullable(device[0]).ifPresent(supplier ->
          ofNullable(supplier.get()).ifPresent(closeable -> {
            try {closeable.close();} catch (IOException exception)
            {throw new RuntimeException(exception);}
          })
        );
      };
    }

    /*
     * @param manager camera manager
     * @param id id of camera
     * @param front front camera first
     *
     * @return new created {@link CameraProfile}
     */
    @NonNull private static CameraProfile createProfile
    (@NonNull CameraManager manager, @NonNull String id, boolean front) {
      try {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);

        @NonNull final StreamConfigurationMap configs = requireNonNull
          (characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP));

        final android.util.Size[]
          camYUVSizes = configs.getOutputSizes(ImageFormat.YUV_420_888),
          //camYUVSizes = configs.getOutputSizes(SurfaceHolder.class),
          camJPGSizes = configs.getOutputSizes(ImageFormat.JPEG);

        final Size[] yuvSizes = new Size[camYUVSizes.length];
        for (int i = 0; i < yuvSizes.length; i++) {
          final android.util.Size size = camYUVSizes[i];
          yuvSizes[i] = new Size(size.getWidth(), size.getHeight());
        } sort(yuvSizes, comparingLong(o -> o.width * o.height));

        final Size[] jpgSizes = new Size[camJPGSizes.length];
        for (int i = 0; i < jpgSizes.length; i++) {
          final android.util.Size size = camJPGSizes[i];
          jpgSizes[i] = new Size(size.getWidth(), size.getHeight());
        } sort(jpgSizes, comparingLong(o -> o.width * o.height));


        final Integer facing = characteristics.get(LENS_FACING);
        final Integer orientation = characteristics.get(SENSOR_ORIENTATION);
        final Integer level = characteristics.get(INFO_SUPPORTED_HARDWARE_LEVEL);

        if (facing == null || orientation == null || level == null)
          throw new IllegalStateException();

        final CameraProfile.Rotation profileRotation =
          orientation == 0 ? ROTATION_000 :
            orientation == 90 ? CameraProfile.Rotation.ROTATION_090 :
              orientation == 180 ? CameraProfile.Rotation.ROTATION_180 :
                orientation == 270 ? CameraProfile.Rotation.ROTATION_270 :
                  null;

        final CameraProfile.Facing profileFacing =
          facing == LENS_FACING_FRONT ? CameraProfile.Facing.FRONT :
            facing == LENS_FACING_BACK ? CameraProfile.Facing.BACK :
              facing == LENS_FACING_EXTERNAL ? CameraProfile.Facing.EXTERNAL :
                null;

        final CameraProfile.Level profileLevel =
          level == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY ? CameraProfile.Level.LEGACY :
            level == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED ? CameraProfile.Level.LIMITED :
              level == INFO_SUPPORTED_HARDWARE_LEVEL_FULL ? CameraProfile.Level.FULL :
                CameraProfile.Level.LEVEL3;

        return new CameraProfile(id,
          requireNonNull(profileRotation),
          requireNonNull(profileFacing),
          profileLevel, yuvSizes, jpgSizes,
          front);

      } catch (CameraAccessException exception) {
        throw new RuntimeException(exception);
      }
    }

    /**
     * @param set set of {@link CameraProfile}'s
     *
     * @return ArraySet of profiles
     */
    @NonNull private static ArraySet<CameraProfile> toArraySet(@NonNull Set<CameraProfile> set)
    {final ArraySet<CameraProfile> result = new ArraySet<>(set.size()); result.addAll(set); return result;}

  }

  /**
   * Selected CameraInstance CameraProfile.
   *
   * @author Nikitenko Gleb
   * @since 1.0, 09/09/2018
   */
  public static final class Selected {

    /** Index of selected profile. */
    @IntRange(from = 0)
    public final int index;

    /** Instance of selected profile. */
    @NonNull public final CameraProfile profile;

    /** Hash code. */
    private final int mHash;

    /**
     * Constructs a new {@link Selected}
     *
     * @param index selected index
     * @param profile selected profile
     */
    public Selected(@IntRange(from = 0) int index, @Nullable CameraProfile profile)
    {this.index = index; this.profile = requireNonNull(profile);
      mHash = 31 * (31 + index) + profile.hashCode();}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Selected)) return false;
      final Selected state = (Selected) obj;
      return index == state.index &&
        Objects.equals(profile, state.profile);
    }

    /** {@inheritDoc} */
    @Override public final int hashCode() {return mHash;}

    /** {@inheritDoc} */
    @Override public final String toString()
    {return "State{" + "index=" + index + ", profile=" + profile + '}';}
  }
}

/*
 * Enumerator.java
 * java-camera
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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Function;

import arch.FrameSize;
import arch.blocks.Manager;
import arch.blocks.Module;
import arch.observables.Observable;
import arch.observables.Selector;

/**
 * The camera enumerator.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Enumerator extends Observable<Enumerator.CameraDevice> {

  /** Descriptions selector. */
  private final Selector<Description> mSelector;

  /** The factory of cameras. */
  private final Function<Description, CameraDevice> mMapper;

  /** Executor */
  private final Executor mExecutor;

  /** Main thread. */
  private final long mMainThread =
      Thread.currentThread().getId();

  /**
   * Constructs a new {@link Enumerator}.
   *
   * @param selector selector of description
   */
  public Enumerator(Selector<Description> selector, Manager<FrameSize> output,
      Function<Description, CameraDevice> mapper, Executor executor) {
    super(selector, output); mExecutor = executor;
    mMapper = mapper; mSelector = selector;
  }

  /** Sync camera by output */
  private void syncByOutput
  (CameraDevice camera, FrameSize frameSize) {
    if (camera == null) return;
    final CameraDevice forChangeCam = camera;
    final FrameSize frame = frameSize;
    final Runnable task = () -> {
      if (forChangeCam.getFrameSize() != null && frame == null)
        forChangeCam.stop();
      else if (forChangeCam.getFrameSize() == null && frame != null)
        forChangeCam.start(frame);
      else {
        final FrameSize a = forChangeCam.getFrameSize();
        if (!Objects.equals(a, frame))
        {forChangeCam.stop(); forChangeCam.start(frame);}
      }
    };
    if (mMainThread == Thread.currentThread().getId()) task.run();
    else mExecutor.execute(task);
  }

  /** @param camera for close async */
  private void closeCam(CameraDevice camera) {
    if (camera == null) return;
    final Runnable task = camera::close;
    if (mMainThread == Thread.currentThread().getId()) task.run();
    else mExecutor.execute(task);
  }

  /** Toggle the camera. */
  public final void toggle() {
    if (mMainThread == Thread.currentThread().getId()) mSelector.toggle();
    else mExecutor.execute(mSelector::toggle);
  }

  /** Reset current selection. */
  public final void reset() {
    if (mMainThread == Thread.currentThread().getId()) mSelector.reset();
    else mExecutor.execute(mSelector::reset);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override protected final CameraDevice apply(Optional[] optionals) {

    final CameraDevice noCamera = null;
    final CameraDevice cameraDevice =
        ((Optional<CameraDevice>)
            optionals[0]).orElse(noCamera);

    final Selector.Selection<Description> noSelection = null;
    final Selector.Selection<Description> selection =
        ((Optional<Selector.Selection<Description>>)
            optionals[1]).orElse(noSelection);

    final FrameSize noFrameSize = null;
    final FrameSize frameSize =
        ((Optional<FrameSize>)
            optionals[2]).orElse(noFrameSize);

    final Description newDescription =
        selection != null && selection.index != -1 ?
        selection.values[selection.index] : null,
        oldDescription = cameraDevice != null ?
            cameraDevice.getDescription() : null;

    boolean
        hasDescription = newDescription != null,
        wasCamera = oldDescription != null,
        keepLastCamera = hasDescription && wasCamera
            && Objects.equals(newDescription.id, oldDescription.id);

    final CameraDevice result;
    if (keepLastCamera) result = cameraDevice;
    else {
      if (wasCamera) {
        syncByOutput(cameraDevice, noFrameSize);
        closeCam(cameraDevice);
      }
      result = hasDescription ?
          mMapper.apply(newDescription) : null;
    }
    try {return result;}
    finally {syncByOutput(result, frameSize);}
  }

  /**
   * The Camera Api Common Interface
   *
   * @author Nikitenko Gleb
   * @since 1.0, 15/03/2018
   */
  public interface CameraDevice extends Module {

    /** Current frame size */
    FrameSize getFrameSize();

    /** @return camera description */
    Description getDescription();

    /** Start capturing. */
    void start(FrameSize frameSize);

    /** Stop capturing. */
    void stop();
  }
}

/*
 * Model.java
 * app
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

package app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

import clean.BaseModel;
import clean.Threader;

/**
 * Android Model.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/03/2018
 */
@SuppressWarnings("unused")
public class Model {

  /** Serial mode */
  private static final boolean SERIAL = true;

  /** STATE KEYS.. */
  private static final String STATE_ACTIONS = ":actions";

  /** "DETACH" Command. */
  private static final int WHAT_DETACH = 0;

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Model() {throw new AssertionError();}

  /** @return new created main thread handler */
  public static Handler newHandler()
  {return new Handler(Looper.myLooper(), Model::detach);}

  /**
   * Creates a new Threader Builder
   *
   * @param inState saved state instance
   * @param handler the main thread handler
   * @param factory the thread factory
   * @param unPacker arguments unpacker
   *
   * @return Threader Builder
   */
  @SuppressLint("UseSparseArrays")
  public static Threader.Builder create
  (Bundle inState, Handler handler, Threader.Factory factory,
      UnPacker unPacker, String name) {

    name += STATE_ACTIONS;
    final SparseArray<? extends Parcelable> actions = inState != null ?
            inState.getSparseParcelableArray(name) : null;

    HashMap<Integer, Object> state = null;
    if (actions != null) {
      state = new HashMap<>(actions.size());
      for (int i = 0; i < actions.size(); i++) {
        final int key = actions.keyAt(i);
        state.put(key, unPacker.unPack(key, actions.valueAt(i)));
      }
    }

    final Threader.Builder builder = SERIAL ?
          Threader.newSerial(handler::post, state) :
        Threader.newParallel(handler::post, state);
    if (factory != null) builder.factory(factory);

    return builder;
  }

  /**
   * Saving current state
   *
   * @param model model instance
   * @param outState out state container
   * @param packer arguments packer
   *
   * @param <T> type of model
   */
  public static <T extends BaseModel<?>> void save
  (T model, Bundle outState, Packer packer, String name) {
    final HashMap<Integer, Object> raw = BaseModel.state((BaseModel<?>) model);
    final SparseArray<Parcelable> packed = new SparseArray<>(raw.size());

    for (final Map.Entry<Integer, Object> entry : raw.entrySet()) {
      /*iterator.remove();*/
      final int key = entry.getKey();
      final Parcelable value = packer.pack(key, entry.getValue());
      if (value != Bundle.EMPTY) packed.put(key, value);
    }
    name += STATE_ACTIONS;
    outState.putSparseParcelableArray(name, packed);
  }

  /**
   * Release the model.
   *
   * @param handler the main thread handler
   */
  public static void release(Handler handler) {
    handler.removeMessages(WHAT_DETACH);
  }

  /**
   * Release the model.
   *
   * @param model model instance
   * @param handler the main thread handler
   *
   * @param <T> type of model
   */
  public static <T extends BaseModel<?>> void release
  (T model, Handler handler) {
    final Message msg = detachMessage(model, handler);
    handler.dispatchMessage(msg); msg.recycle();
  }

  /**
   * Start the model.
   *
   * @param model model instance
   * @param view view instance
   *
   * @param <V> type of view
   * @param <T> type of model
   */
  public static <V, T extends BaseModel<V>> void start
  (T model, V view) {BaseModel.setView(model, view);}

  /**
   * Stop the model.
   *
   * @param model model instance
   * @param handler main handler
   *
   * @param <T> type of model
   */
  public static <T extends BaseModel<?>> void stop
  (T model, Handler handler)
  {handler.sendMessageAtFrontOfQueue(detachMessage(model, handler));}

  /**
   * @param msg "DETACH"-Message
   * @return true by success
   */
  private static boolean detach(Message msg) {
    if (msg.what != WHAT_DETACH || msg.obj == null) return false;
    if (!(msg.obj instanceof BaseModel<?>)) return false;
    final BaseModel<?> model = (BaseModel<?>) msg.obj;
    BaseModel.setView(model,null); return true;
  }

  /**
   * @param model model instance
   * @param handler main thread handler
   *
   * @param <T> the type of model
   *
   * @return "DETACH" message
   */
  private static <T extends BaseModel<?>> Message detachMessage
      (T model, Handler handler)
  {return Message.obtain(handler, WHAT_DETACH, model);}


  /** The Packer */
  @FunctionalInterface
  public interface Packer {
    /**
     * @param id id of action
     * @param data raw arguments of action
     *
     * @return parcelable instance of arguments
     */
    Parcelable pack(int id, Object data);
  }


  /** The UnPacker */
  @FunctionalInterface
  public interface UnPacker {
    /**
     * @param id id of action
     * @param data parcelable arguments of action
     *
     * @return raw instance of arguments
     */
    Object unPack(int id, Parcelable data);
  }

}

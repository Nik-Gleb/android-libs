/*
 * BaseModel.java
 * clean
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

package clean;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Objects;

/**
 * Base java-model.
 *
 * @param <V> the type of view
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/02/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public abstract class BaseModel<V> implements Closeable {

  /** Threader builder. */
  private final Threader.Builder mBuilder;

  /** The View. */
  protected V view = null;

  /** The threader. */
  private Threader mThreader = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link BaseModel}.
   *
   * @param builder the builder
   */
  protected BaseModel(Threader.Builder builder)
  {mBuilder = builder;}

  /** @param view the view instance for attach/detach */
  public final void setView(V view) {
    if (this.view == view) return;
    if (view != null && mThreader == null)
    {this.view = view; mThreader = mBuilder.build();}
    else if (view == null && mThreader != null)
    {mThreader.close(); mThreader = null; this.view = null;}
    else this.view = view;
  }

  /** @return all active actions */
  public final HashMap<Integer, Object> state() {
    Objects.requireNonNull(mThreader).backup();
    return mBuilder.savedState;
  }

  /**
   * Apply the action.
   *
   * @param id action id
   */
  protected final void apply(int id)
  {mThreader.apply(id, Void.TYPE);}

  /**
   * Apply the action.
   *
   * @param id action id
   * @param args action args
   *
   * @param <U> the type of args
   */
  @SuppressWarnings("WeakerAccess")
  protected final <U> void apply(int id, U args)
  {mThreader.apply(new AsyncTask(id, args));}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; onClose(); mClosed = true;}

  /** Causes by close */
  @SuppressWarnings("WeakerAccess")
  protected void onClose()
  {if (mThreader != null) {mThreader.close(); mThreader = null;}}


}

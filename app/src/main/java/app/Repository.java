/*
 * Repository.java
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

import android.os.Bundle;

import java.io.Closeable;

/**
 * Base Android Repository.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 02/03/2018
 */
@SuppressWarnings("unused")
public abstract class Repository implements Closeable {

  /** STATE KEYS. */
  private static final String
      STATE_REPOSITORY = "repository";

  /** This instance. */
  @SuppressWarnings("WeakerAccess")
  protected final Repository instance = this;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * @param inState saved state instance
   *
   * @param <T> type of model
   *
   * @return model instance or null
   */
  public static <T extends Repository> T get(Bundle inState)
  {return inState != null ? Retain.get(inState, STATE_REPOSITORY) : null;}

  /** @param outState saved state container */
  public final void save(Bundle outState)
  {Retain.put(outState, STATE_REPOSITORY, instance);}

  /** @param isFinishing true by exit */
  public final void release(boolean isFinishing)
  {if (isFinishing) close();}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; onClosed();  mClosed = true;}

  /** Calls by exit */
  @SuppressWarnings("WeakerAccess")
  protected abstract void onClosed();
}

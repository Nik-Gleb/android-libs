/*
 * Module.java
 * extensions
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

package extensions;

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Stack;

import javax.inject.Named;

import dagger.Provides;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 25/06/2018
 */
@SuppressWarnings("unused")
@dagger.Module public class Module implements Closeable {

  /** Closeable dependencies. */
  @NonNull private final Stack mCloseables = new Stack<>();

  /** Closed state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return; mClosed = true;
    while (!mCloseables.empty()) {
      try {((Closeable)mCloseables.pop()).close();}
      catch (IOException ignored) {}
    }
  }

  /** @return closeable dependencies. */
  @SuppressWarnings("unchecked")
  @Provides @Named("stack") @NonNull
  protected final Stack closeables()
  {return mCloseables;}

  /** @return module */
  @Provides @Named("module") @NonNull protected final
  Closeable module() {return this;}
}

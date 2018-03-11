/*
 * View.java
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;

/**
 * Base view.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 10/03/2018
 */
public interface View extends Closeable {

  /** Start view. */
  default void start() {}

  /** Stop view. */
  default void stop() {}

  /** @param outState saved state container */
  void save(@NonNull Bundle outState);

  /** true if state was saved */
  boolean isSaved();

  /** {@inheritDoc} */
  @Override default void close() {}

  /**
   * The view factory.
   *
   * @param <T> the type of view
   */
  @FunctionalInterface
  @SuppressWarnings("UnnecessaryInterfaceModifier")
  public interface Factory<T extends View, U extends Presenter<?>, S extends Scope<T, U>, V> {
    /**
     * @param component source view
     * @param inState saved state
     * @return view - instance
     */
    @SuppressWarnings("UnnecessaryInterfaceModifier")
    @NonNull public T create(@NonNull S scope, @NonNull V component, @Nullable Bundle inState);
  }
}

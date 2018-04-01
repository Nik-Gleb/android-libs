/*
 * Valve.java
 * java-arch
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

package base.lifecycle;

/**
 * Data-Flow Valve.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
@SuppressWarnings("unused")
final class Valve<T> implements Jumper<T> {

  /** Consumer replacement.  */
  private final Jumper<T> mCrossover;

  /** Current consumer */
  private Consumer<T> mCurrent;

  /**
   * Construct a new {@link Valve}
   *
   * @param crossover consumer replacement
   */
  Valve(Jumper<T> crossover) {mCrossover = crossover;}

  /** {@inheritDoc} */
  @Override public final void consumer(Consumer<T> consumer) {
    if (consumer == null) consumer = mCrossover;
    if (mCurrent == consumer) return;
    final boolean pump = mCurrent == mCrossover;
    mCurrent = consumer;
    if (pump) mCrossover.consumer(mCurrent);
  }

  /** {@inheritDoc} */
  @Override public final boolean use(T item)
  {return mCurrent.use(item);}

}

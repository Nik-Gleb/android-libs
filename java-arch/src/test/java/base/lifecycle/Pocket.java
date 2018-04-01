/*
 * Pocket.java
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

import java.util.Queue;

/**
 * Pocket - is a queue-based crossover.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
@SuppressWarnings("unused")
final class Pocket<T> implements Jumper<T> {

  /** The queue of items. */
  private final Queue<T> mQueue;

  /**
   * Constructs a new {@link Pocket}
   *
   * @param queue queue of items
   */
  Pocket(Queue<T> queue) {mQueue = queue;}

  /** {@inheritDoc} */
  @Override public final boolean use(T item)
  {return mQueue.offer(item);}

  /** {@inheritDoc} */
  @Override public final void consumer(Consumer<T> consumer)
  {T item; while ((item = mQueue.poll()) != null) {consumer.use(item);}}
}

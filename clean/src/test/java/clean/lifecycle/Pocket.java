package clean.lifecycle;

import java.util.Queue;

/**
 * Pocket - is a queue-based crossover.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
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

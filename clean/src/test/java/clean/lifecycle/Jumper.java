package clean.lifecycle;

/**
 * Data-Flow Jumper.
 *
 * @param <T> the type of items
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
interface Jumper<T> extends Consumer<T> {

  /** @param consumer new target for items */
  void consumer(Consumer<T> consumer);
}

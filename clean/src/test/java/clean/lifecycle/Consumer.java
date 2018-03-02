package clean.lifecycle;

/**
 * Data-Flow Consumer.
 *
 * @param <T> the type of items
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
interface Consumer<T> {

  /**
   * @param item element
   *
   * @return true if the item was used,
   *         otherwise - false
   */
  boolean use(T item);
}
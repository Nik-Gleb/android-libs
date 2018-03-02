package clean;

/**
 * Async set function.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/02/2018
 */
public interface FunctionSet<T> {

  /**
   * Set an args, or throws an exception if unable to do so.
   *
   * @param value input data as arguments
   *
   * @throws Throwable exception, that was throws
   */
  @SuppressWarnings("unused")
  void set(T value) throws Throwable;
}

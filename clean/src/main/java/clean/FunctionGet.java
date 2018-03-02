package clean;

/**
 * Async actions function.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/02/2018
 */
public interface FunctionGet<T> {

  /**
   * Computes a result, or throws an exception if unable to do so.
   *
   * @return task output:
   *          NonNull Data Object - for functions
   *          Null/Empty Result - for void methods
   *
   * @throws Throwable exception, that was throws
   */
  @SuppressWarnings("unused")
  T get() throws Throwable;
}

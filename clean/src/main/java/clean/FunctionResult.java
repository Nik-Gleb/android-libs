package clean;

/**
 * Async result function.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/02/2018
 */
public interface FunctionResult<T> {

  /** Causes by result received */
  @SuppressWarnings("unused")
  void result(T value);
}

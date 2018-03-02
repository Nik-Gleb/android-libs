package clean;

/**
 * Async void function.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/02/2018
 */
public interface FunctionVoid {

  /**
   * Perform an action, or throws an exception if unable to do so.
   *
   * @throws Throwable exception, that was throws
   */
  void apply() throws Throwable;
}

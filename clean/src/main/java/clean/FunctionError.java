package clean;

/**
 * Async error function.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 22/02/2018
 */
public interface FunctionError {

  /** Causes when something was failed */
  @SuppressWarnings("unused")
  void error(Throwable value);
}

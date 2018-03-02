package clean;

/**
 * Base function meta.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/02/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
abstract class Function {

  /** Error-Handle Function. */
  protected final FunctionError error;

  /**
   * Constructs a new {@link Function}.
   *
   * @param error errors handler
   */
  protected Function(FunctionError error)
  {this.error = error;}

  /**
   * Asynchronous perform.
   *
   * @param args arguments of action
   *
   * @return result of action
   *
   * @throws Throwable failed error
   */
  abstract Object apply(Object args) throws Throwable;

  /** @param result action's result */
  abstract void delivery(Object result);
}

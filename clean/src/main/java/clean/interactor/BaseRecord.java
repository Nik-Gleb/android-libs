package clean.interactor;

import java.util.function.Consumer;

/**
 * Base function meta.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/02/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
abstract class BaseRecord {

  /** Error-Handle BaseRecord. */
  protected final Consumer<Exception> error;

  /**
   * Constructs a new {@link BaseRecord}.
   *
   * @param error errors handler
   */
  protected BaseRecord
  (Consumer<Exception> error)
  {this.error = error;}

  /**
   * Asynchronous perform.
   *
   * @param args arguments of action
   *
   * @return result of action
   */
  abstract Object apply(Object args);

  /** @param result action's result */
  abstract void delivery(Object result);
}

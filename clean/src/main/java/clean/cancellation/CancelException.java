package clean.cancellation;

import java.util.concurrent.CancellationException;

/**
 * Cancel Exception.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/03/2018
 */
public final class CancelException extends CancellationException {

  /** Serialization ID. */
  private static final long serialVersionUID = 5010716256764711679L;

  /** Constructs a new {@link CancelException} */
  private CancelException() {this(null);}

  /** Constructs a new {@link CancelException} */
  private CancelException(String message)
  {super(message != null ? message : "The operation has been canceled.");}

  /** Check if current thread was interrupted. */
  public static void check()
  {if (Thread.interrupted()) throw new CancelException();}

  /** Check if current thread was interrupted. */
  public static void check(String message)
  {if (Thread.interrupted()) throw new CancelException(message);}

}

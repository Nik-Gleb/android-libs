package clean;

import java.io.Closeable;

import clean.cancellation.CancellationSignal;
import clean.cancellation.CancellationSignal.OperationCanceledException;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 20/02/2018
 */
@SuppressWarnings("unused")
public interface Data<T> extends Closeable {

  /**
   * @param signal cancellation signal
   *
   * @return data object
   *
   * @throws OperationCanceledException when action was cancelled
   */
  T get(CancellationSignal signal) throws OperationCanceledException;

  /** {@inheritDoc} */
  @Override void close();
}

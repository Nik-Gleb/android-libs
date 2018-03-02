package clean.cancellation;

/**
 * Double cancellation signal.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class DoubleCancellationSignal extends CancellationSignalWrapper {

  /** External signal */
  private final CancellationSignal mCancellationSignal;

  /**
   * Constructs a new {@link DoubleCancellationSignal}
   *
   * @param signal inner cancellation signal
   */
  public DoubleCancellationSignal(CancellationSignal signal) {
    super(new DefaultCancellationSignal());
    mCancellationSignal = signal;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCanceled() {
    return super.isCanceled() || mCancellationSignal.isCanceled();
  }

  /** {@inheritDoc} */
  @Override
  public final void throwIfCanceled() {
    super.throwIfCanceled();
    mCancellationSignal.throwIfCanceled();
  }

  /** {@inheritDoc} */
  @Override
  public final void setOnCancelListener(OnCancelListener listener) {
    super.setOnCancelListener(listener);
    mCancellationSignal.setOnCancelListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void close() {
    mCancellationSignal.close();
    super.close();
  }
}

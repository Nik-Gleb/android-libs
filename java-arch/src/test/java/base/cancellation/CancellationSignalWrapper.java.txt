package clean.cancellation;

/**
 * Cancellation Signal Wrapper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/02/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public abstract class CancellationSignalWrapper implements CancellationSignal {

  /** Inner {@link CancellationSignal} */
  protected final CancellationSignal cancellationSignal;

  /** The object was released. */
  private volatile boolean mReleased;

  /**
   * Constructs a new {@link CancellationSignalWrapper}
   *
   * @param signal inner cancellation signal
   */
  public CancellationSignalWrapper(CancellationSignal signal) {
    cancellationSignal = signal;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCanceled() {
    return cancellationSignal.isCanceled();
  }

  /** {@inheritDoc} */
  @Override
  public void throwIfCanceled() {
    cancellationSignal.throwIfCanceled();
  }

  /** {@inheritDoc} */
  @Override
  public void cancel() {
    cancellationSignal.cancel();
  }

  /** {@inheritDoc} */
  @Override
  public void setOnCancelListener(OnCancelListener listener) {
    cancellationSignal.setOnCancelListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    checkState();
    cancellationSignal.close();
    mReleased = true;
  }

  /** {@inheritDoc} */
  @Override
  protected final void finalize() throws Throwable {
    try {
      if (!mReleased) {
        close();
        throw new RuntimeException(
            "\nA resource was acquired at attached stack trace but never released." +
                "\nSee java.io.Closeable for info on avoiding resource leaks."
        );
      }
    } finally {
      super.finalize();
    }
  }

  /** Check state. */
  private void checkState()
  {if (mReleased) throw new IllegalStateException("Already closed");}
}

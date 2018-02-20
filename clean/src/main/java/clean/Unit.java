package clean;

import clean.cancellation.CancellationSignal;
import clean.cancellation.CancellationSignal.OperationCanceledException;
import clean.cancellation.DefaultCancellationSignal;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 20/02/2018
 */
@SuppressWarnings("unused")
public class Unit<T> extends Observable<T> implements Manager {

  /** Log tag. */
  private final String mTag = getClass().getSimpleName();

  /** Default cancellation signal */
  static final DefaultCancellationSignal SIGNAL =
      new DefaultCancellationSignal();

  /** The default delay. */
  private static final long DELAY = 3000;

  /** Stub value. */
  @SuppressWarnings("WeakerAccess")
  protected T mStubValue = null;

  /** Default constructor. */
  @SuppressWarnings("WeakerAccess")
  protected Unit() throws OperationCanceledException
  {stub("init", SIGNAL);}

  /** {@inheritDoc} */
  @Override public T get(CancellationSignal signal)
      throws OperationCanceledException
  {stub("get", SIGNAL); return mStubValue;}

  /** {@inheritDoc} */
  @Override public void apply(Action args, CancellationSignal signal)
      throws OperationCanceledException {stub("apply", SIGNAL);}

  /** {@inheritDoc} */
  @Override public void close() throws OperationCanceledException
  {stub("close", SIGNAL);}

  /** @param signal cancellation signal */
  @SuppressWarnings("WeakerAccess")
  protected static void stub(CancellationSignal signal)
      throws OperationCanceledException {
    final long time = System.currentTimeMillis();
    while (System.currentTimeMillis() - time < DELAY)
      signal.throwIfCanceled();
  }

  /**
   * Default stub-method
   *
   * @param method method name
   * @param signal cancellation signal
   */
  private void stub(String method, CancellationSignal signal) {
    System.out.println(mTag + ": " + method + " (started)");
    try {stub(signal);} catch (OperationCanceledException e)
    {System.out.println(mTag + ": " + method + " (cancelled)"); throw e;}
    finally {System.out.println(mTag + ": " + method + " (finished)");}
  }

}

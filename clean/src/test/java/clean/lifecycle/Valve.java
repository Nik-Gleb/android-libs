package clean.lifecycle;

/**
 * Data-Flow Valve.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/02/2018
 */
@SuppressWarnings("unused")
final class Valve<T> implements Jumper<T> {

  /** Consumer replacement.  */
  private final Jumper<T> mCrossover;

  /** Current consumer */
  private Consumer<T> mCurrent;

  /**
   * Construct a new {@link Valve}
   *
   * @param crossover consumer replacement
   */
  Valve(Jumper<T> crossover) {mCrossover = crossover;}

  /** {@inheritDoc} */
  @Override public final void consumer(Consumer<T> consumer) {
    if (consumer == null) consumer = mCrossover;
    if (mCurrent == consumer) return;
    final boolean pump = mCurrent == mCrossover;
    mCurrent = consumer;
    if (pump) mCrossover.consumer(mCurrent);
  }

  /** {@inheritDoc} */
  @Override public final boolean use(T item)
  {return mCurrent.use(item);}

}

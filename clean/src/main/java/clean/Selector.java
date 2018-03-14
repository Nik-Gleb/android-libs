package clean;

import java.util.Arrays;
import java.util.Objects;

/**
 * Universal Selector.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 14/03/2018
 */
@SuppressWarnings("unused")
public final class Selector<T> extends Observable<Selector.Selection<T>> {

  /** Device list changed listener */
  private final OnChangedListener mOnChangedListener;

  /** The mValues repository. */
  private final Observable<T[]> mProvider;

  /** Cached mValues. */
  @SuppressWarnings("unchecked")
  private T[] mValues = null;

  /** Cached index. */
  private int mIndex = -1;

  /**
   * Constructs a new {@link Selector}
   *
   * @param provider devices ids
   */
  public Selector(Observable<T[]> provider) {
    (mProvider = provider).registerObserver
        (mOnChangedListener = this::onChanged);
  }

  /** {@inheritDoc} */
  @Override protected final void onClose() {
    mProvider.unregisterObserver(mOnChangedListener);
    super.onClose();
  }

  /** Calls when array of mValues was changed. */
  private void onChanged() {
    final T[] values; try {values = mProvider.get();}
    catch (Throwable ignored) {return;}
    if (Arrays.equals(mValues, values)) return;
    setValues(values); notifyChanged();
  }

  /** @param index new selected index */
  public final void select(int index) {
    if (mIndex != (index = normalize(index)))
    {mIndex = index; notifyChanged();}
  }

  /**
   * @param index source index
   * @return index result
   */
  private int normalize(int index)
  {return mValues == null || mValues.length == 0 ? -1 :
      index < -1 || index > mValues.length - 1 ? mIndex : index;}

  /** @param values new values */
  private void setValues(T[] values) {
    final T[] oldValues = mValues; mValues = values;
    if ((mIndex = normalize(mIndex)) == -1) return;
    final int last = mValues.length - 1;
    final int newIndex = find(mValues, oldValues[mIndex]);
    mIndex = newIndex != -1 ? newIndex : mIndex > last ? last : mIndex;
  }

  /**
   * @param array array of items
   * @param item item instance
   *
   * @param <T> type of items
   *
   * @return index of searched item or -1
   */
  private static <T> int find(T[] array, T item) {
    for (int i = 0; i < array.length; i++)
      if (Objects.equals(array[i], item)) return i;
    return -1;
  }

  /** {@inheritDoc} */
  @Override public final Selection<T> get() throws Throwable
  {return new Selection<>(mIndex, mValues);}


  /**
   * Selection object
   *
   * @param <T> type of object
   */
  @SuppressWarnings("WeakerAccess")
  public static final class Selection<T> {

    /** Selected index. */
    public final int index;

    /** Selected mValues. */
    public final T[] values;

    /**
     * Constructs a new {@link Selection}.
     *
     * @param index the index of selection
     * @param values the current mValues
     */
    public Selection(int index, T[] values)
    {this.index = index; this.values = values;}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Selection)) return false;
      final Selection<?> selection = (Selection<?>) obj;
      return index == selection.index && Arrays.equals(values, selection.values);
    }

    /** {@inheritDoc} */
    @Override public final int hashCode() {return Objects.hash(index, values);}

    /** {@inheritDoc} */
    @Override public final String toString() {
      final StringBuilder builder = new StringBuilder("Selection{")
          .append("index=").append(index)
          .append(", values=").append(Arrays.toString(values))
          .append('}');
      try{return builder.toString();}
      finally {builder.setLength(0);}
    }
  }
}

package clean.observables;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntUnaryOperator;

/**
 * Universal Selector.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 14/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Selector<T>
    extends Observable<Selector.Selection<T>> {

  /** Not selected index. */
  private static final int NOT_SELECTED = -1;

  /** An empty array. */
  @SuppressWarnings("unchecked")
  private final T[] mEmpty = (T[]) new Object[0];

  /** Array provider. */
  private final Observable<T[]> mProvider;

  /** Current index. */
  private PropertyInteger mIndex =
      newProperty(NOT_SELECTED);

  /**
   * Constructs a new {@link Selector}.
   *
   * @param provider values provider
   */
  public Selector(Observable<T[]> provider)
  {super(provider); mProvider = provider;}

  /** @param index new index for select */
  public final void select(int index)
  {mIndex.setAndUpdate(index);}

  /** Internal toggle camera */
  public final void toggle() {
    final int
        count = mProvider.get()
        .orElse(mEmpty).length,
        index = mIndex.get();

    if (count == 0) return;
    select (
        count == 1 && index != 0 ?
        0 : (index + 1) % count
    );
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override public final Selection<T> apply(Optional[] optionals) {

    final Selector.Selection<T> noSelection = null;
    final Selector.Selection<T> selection =
        ((Optional<Selector.Selection<T>>)
        optionals[0]).orElse(noSelection);

    final T[]
        values =
        ((Optional<T[]>)
            optionals[1])
            .orElse(mEmpty);

    final Updater updater =
        Updater.create
            (selection,
                values,
                mEmpty);

    final int index =
        mIndex.updateAndGet(updater);

    return new Selection<>(index, values);
  }
  
  /** Selection updater */
  private static final class Updater<T> implements IntUnaryOperator {

    /** Previous selection. */
    private final int mIndex, mCount;

    /** Old values. */
    private final T[] mOld, mNew;

    /**
     * Constructs a new {@link Updater}.
     *
     * @param index previous selection
     * @param old old old
     */
    private Updater(int index, T[] old, T[] aNew)
    {mIndex = index; mOld = old; mNew = aNew; mCount = mNew.length;}

    /** {@inheritDoc} */
    @Override public final int applyAsInt(int current) {
      if ((current = normalize(mIndex, current, mCount)) != NOT_SELECTED) {
        final int last = mCount - 1;
        final int newIndex = find(mNew, mOld[current]);
        current = newIndex != -1 ? newIndex : current > last ? last : current;
      }
      return current;
    }

    /**
     * @param index source index
     * @return index result
     */
    private static int normalize(int prev, int index, int count)
    {return count != 0 ? index < NOT_SELECTED || index > count - 1 ?
          prev : index : NOT_SELECTED;}

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
      return NOT_SELECTED;
    }

    /**
     * @param prev previous selection
     * @param values old values
     *
     * @return new created {@link Updater}
     */
    static <T> Updater create(Selector.Selection<T> prev, T[] values, T[] empty) {
      final T[] prevValues; final int prevIndex; if (prev != null)
      {prevValues = prev.values; prevIndex = prev.index;}
      else {prevValues = empty; prevIndex = NOT_SELECTED;}
      return new Updater<>(prevIndex, prevValues, values);
    }
  }

  /**
   * Selection object
   *
   * @param <T> type of object
   */
  @SuppressWarnings("WeakerAccess")
  public static final class Selection<T> {

    /** Selected index. */
    public final int index;

    /** Selected mOld. */
    public final T[] values;

    /**
     * Constructs a new {@link Selection}.
     *
     * @param index the index of selection
     * @param values the current mOld
     */
    public Selection(int index, T[] values)
    {this.index = index; this.values = values;}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Selection)) return false;
      final Selection<?> selection = (Selection<?>) obj;
      return index == selection.index &&
          Arrays.equals(values, selection.values);
    }

    /** {@inheritDoc} */
    @Override public final int hashCode()
    {return Objects.hash(index, values);}

    /** {@inheritDoc} */
    @Override public final String toString() {
      final StringBuilder builder =
          new StringBuilder("Selection{")
              .append("index=").append(index)
              .append(", values=").append(Arrays.toString(values))
              .append('}');
      try{return builder.toString();}
      finally {builder.setLength(0);}
    }
  }

}

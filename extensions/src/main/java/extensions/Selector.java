package extensions;

import android.support.annotation.NonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 21/06/2018
 */
@SuppressWarnings("unused")
public interface Selector<T> {

  /** @return current selection */
  @NonNull Live<Selection<T>> get();

  /** @param id identifier of item for select */
  void select(int id);

  /** @param id identifier of item for unSelect */
  void unSelect(int id);

  /** Clear all selections */
  void reset();
}

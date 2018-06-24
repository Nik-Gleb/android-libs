package extensions;

import android.support.annotation.NonNull;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Контейнер-обёртка элемента.
 *
 * Описывает выбираемый элемет.
 *
 * @param <T> тип элемента
 *
 * @author Nikitenko Gleb
 * @since 1.0, 23/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Selectable<T> {

  /** Элемент данных. */
  public final T content;

  /** Флаг "выбранности". */
  public final boolean selected;

  /**
   * Создаёт новый элемент-контейнер.
   *
   * @param content элемент данных
   * @param selected флаг "выбранности"
   */
  Selectable(T content, boolean selected) {
    this.content = Objects.requireNonNull(content);
    this.selected = selected;
  }

  /** {@inheritDoc} */
  @Override public final String toString() {
    return "Selectable{" + "selected=" + selected
        + ", state=" + content
        + '}';
  }

  /** {@inheritDoc} */
  @Override public final boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof Selectable)) return false;
    final Selectable<?> that = (Selectable<?>) object;
    return selected == that.selected &&
        Objects.equals(content, that.content);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return content.hashCode();}

  /**
   * @param <T> type of content
   *
   * @return to-content mapper
   */
  @NonNull public static <T>
  Function<Selectable<T>, T> toContent()
  {return selectable -> selectable.content;}

  /**
   * @param <T> type of content
   *
   * @return to-selected mapper
   */
  @NonNull public static <T>
  Predicate<Selectable<T>> toSelected()
  {return selectable -> selectable.selected;}
}

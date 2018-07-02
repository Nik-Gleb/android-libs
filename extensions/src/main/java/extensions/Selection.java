/*
 * Selection.java
 * extensions
 *
 * Copyright (C) 2018, Gleb Nikitenko. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package extensions;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Контейнер-обёртка выбранных элементов.
 *
 * Описывает результат пересечения множества элеметов и
 * множества выбранных идентификаторов.
 *
 * @param <T> тип элемента
 *
 * @author Никитенко Глеб
 * @since 09/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Selection<T> {

  /**
   * Элементы данных.
   *
   * Множество всех элементов,
   * обёрнутых в {@link Selectable}.
   */
  public final Set<Selectable<T>> items;

  /**
   * "Ничейные" идентификаторы.
   *
   * Множество выбранных идентификаторов, на которые
   * отсутствуют соответствующие контент-элементы.
   */
  public final Set<Integer> orphanIds;

  /** Selection. */
  private final Set<Integer> mSelection;

  /**
   * Создаёт новый контейнер выбранных данных.
   *
   * @param items контент-элементы, обёрнутые в {@link Selectable}.
   * @param orphanIds холостые идентификаторы
   */
  private Selection(@NonNull Set<Selectable<T>> items,
      @NonNull Set<Integer> orphanIds, @NonNull Set<Integer> selection) {
    this.orphanIds = Collections.unmodifiableSet(orphanIds);
    this.items = Collections.unmodifiableSet(items);
    mSelection = selection;
  }

  /**
   * Создаёт экземпляр {@link Selection}.
   *
   * Вычисляет коллизии селектированных идентификаторов и хэш-кодов элементов.
   * Данный подход подразумевает, что {@link Object#hashCode()} есть уникальный
   * идентификатор элемента множества.
   *
   * Таким образом реализация типа элемента должна обеспечить вывод уникального
   * идентификатора этого элемена через {@link Object#hashCode()}.
   *
   * @param content множество контент-элементов
   * @param selection множество идентификаторов выбранных
   *
   * @param <T> тип одного элемента данных
   *
   * @return Контейнер-обёртка выбранных элементов.
   */
  @NonNull public static <T> Selection<T> toSelection
  (@NonNull Set<T> content, @NonNull Set<Integer> selection) {
    final Set<Integer> orphanIds = new HashSet<>(Objects.requireNonNull(selection));
    return new Selection<>(Objects.requireNonNull(content).stream()
        .map(item -> new Selectable<>(item, orphanIds.remove(item.hashCode())))
        .collect(Collectors.toCollection(LinkedHashSet::new)), orphanIds, selection);
  }

  /**
   * Преобразует текущую выборку к заданному типу.
   *
   * @param content список элементов
   * @param <U> тип элемента
   *
   * @return новая выборка
   */
  @NonNull public final <U> Selection<U>
  transform(@NonNull Set<U> content)
  {return toSelection(content, mSelection);}

  /**
   * Преобразует текущую выборку к заданному типу.
   *
   * @param mapper маппер
   * @param <U> тип элемента
   *
   * @return новая выборка
   */
  @NonNull public final <U> Selection<U> transform(@NonNull Function<T, U> mapper)
  {return new Selection<>(items.stream().map(selectable ->
      new Selectable<>(mapper.apply(selectable.content), selectable.selected))
      .collect(Collectors.toCollection(LinkedHashSet::new)), orphanIds,  mSelection);}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Selection)) return false;
    final Selection<?> selection = (Selection<?>) obj;
    return Objects.equals(items, selection.items) &&
        Objects.equals(orphanIds, selection.orphanIds);
  }

  /** {@inheritDoc} */
  @Override public final int hashCode()
  {return Objects.hash(items, orphanIds);}

  /** {@inheritDoc} */
  @Override @NonNull public final String toString() {
    return "Selection{" + "items=" + items
        + ", orphanIds=" + orphanIds
        + '}';
  }
}

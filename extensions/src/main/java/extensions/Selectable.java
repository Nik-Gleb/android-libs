/*
 * Selectable.java
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

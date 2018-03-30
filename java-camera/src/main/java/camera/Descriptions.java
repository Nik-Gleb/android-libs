/*
 * Descriptions.java
 * java-camera
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

package camera;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import arch.observables.Observable;

import static java.util.Collections.synchronizedSortedSet;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 15/03/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class Descriptions extends Observable<Description[]> {

  /** The factory of descriptions. */
  private final Function<String, Description> mMapper;

  /** Descriptors set */
  private final SortedSet<Description> mSet;

  /** Open Latch. */
  public String openLatch = null;

  /** Close Latch. */
  public String closeLatch = null;

  /**
   * Constructs a new {@link Descriptions}
   *
   * @param mapper descriptions mapper
   * @param ids list of camera ids
   */
  public Descriptions(Function<String, Description> mapper, String[] ids) {
    mSet =
        synchronizedSortedSet
            (new TreeSet<>(
                Arrays
                    .stream(ids)
                    .map(mMapper = mapper)
                    .sorted()
                    .collect(Collectors.toSet())
            ));
  }

  /** @param id enabled camera id */
  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  public final void enable(String id) {
    if (Objects.equals(id, closeLatch)) {closeLatch = null; return;}
    if (mSet.stream().noneMatch(description -> description.equals(id)) &&
        mSet.add(mMapper.apply(id))) invalidate();
  }

  /** @param id disabled camera id */
  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  public final void disable(String id) {
    if (Objects.equals(id, openLatch)) {openLatch = null; return;}
    if (mSet.removeIf(description -> description.equals(id))) invalidate();}

  /** {@inheritDoc} */
  @Override protected final Description[] apply(Optional[] optionals)
  {return mSet.toArray(new Description[mSet.size()]);}
}

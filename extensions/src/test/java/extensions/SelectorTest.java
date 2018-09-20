/*
 * SelectorTest.java
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

import android.support.v4.util.ArraySet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static extensions.Selected.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Sample Java Test
 *
 * @author Nikitenko Gleb
 * @since 1.0, 09/08/2017
 */
@SuppressWarnings({ "WeakerAccess", "EmptyMethod" })
@ExtendWith(MockitoExtension.class)
public class SelectorTest {

  /** Constructs a new {@link SelectorTest} */
  public SelectorTest() {}

  /** {@inheritDoc} */
  @BeforeEach public final void setUp() {}

  /** {@inheritDoc} */
  @SuppressWarnings("ConstantConditions")
  @AfterEach public final void tearDown() {}

  @Test public final void testSelectNullArray()
  {assertNull(select(null, -1));}

  @Test public final void testSelectOutOfBoundNegative()
  {assertNull(select(array(), -1));}

  @Test public final void testSelectOutOfBoundLength()
  {final ArraySet<Integer> items = array();
  assertNull(select(items, items.size()));}

  @Test public final void testSelectOutOfBoundPositive()
  {final ArraySet<Integer> items = array();
  assertNull(select(items, items.size() + 1));}

  @Test public final void testSelectNormalIndex()
  {assertEquals(new Selected<>(1, 5), select(array(3, 5), 1));}

  @Test public final void testNullItemsInitial()
  {assertNull(Selected.create(null));}

  @Test public final void testEmptyItemsInitial()
  {assertNull(Selected.create(array()));}

  @Test public final void testNonEmptyItemsInitial() {
    assertEquals(new Selected<>(0, 3), Selected.create(array(3, 5)));
    assertEquals(new Selected<>(0, 7), Selected.create(array(7, 9)));
  }

  @Test public final void testNullItemsNoInitial()
  {assertNull(new Selected<>(2, 7).items(null));}

  @Test public final void testEmptyItemsNoInitial()
  {assertNull(new Selected<>(2, 7).items(array()));}

  @Test public final void testNonEmptyItemsNoInitialUnchanged() {
    final Selected<Integer> expected = new Selected<>(2, 7);
    assertEquals(expected, expected.items(array(9, 4, 7, 2, 10)));
    assertEquals(expected, expected.items(array(1, 2, 7, 8, 12)));
  }

  @Test public final void testNonEmptyItemsNoInitialSameItem() {
    Selected<Integer> expected = new Selected<>(2, 7);
    assertEquals(expected, expected.items(array(9, 4, 7, 2, 10)));
    expected = new Selected<>(3, 7);
    assertEquals(expected, expected.items(array(1, 2, 7, 6, 12)));
  }

  @Test public final void testNonEmptyItemsNoInitialReselect()
  {assertEquals(new Selected<>(2, 9), new Selected<>(2, 7).items(array(9, 4, 2, 10)));}

  @Test public final void testNonEmptyItemsNoInitialLost()
  {assertEquals(new Selected<>(1, 9), new Selected<>(2, 7).items(array(9, 4)));}


  /**
   * @param items array of items
   *
   * @return array set of items
   */
  @SuppressWarnings("unchecked")
  private static <T> ArraySet<T> array(T... items) {
    final ArraySet<T> result = new ArraySet<>(items.length);
    try {return result;} finally {result.addAll(Arrays.asList(items));}
  }

}
/*
 * CleanTest.java
 * clean
 *
 * Copyright (C) 2017, Gleb Nikitenko. All Rights Reserved.
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

package clean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import clean.cancellation.CancellationSignal.OperationCanceledException;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 09/08/2017
 */
@SuppressWarnings({ "WeakerAccess", "EmptyMethod" })
@RunWith(MockitoJUnitRunner.class)
public class CleanTest {

  /** The Mock Unit. */
  private MockUnit mMockUnit = null;

  /** @throws Exception by some issues */
  @Before public void setUp() throws Exception {
    mMockUnit = new MockUnit();
  }

  /** @throws Exception by some issues */
  @After public void tearDown() throws Exception {
    mMockUnit.close();
    mMockUnit = null;
  }

  /** @throws Exception by some issues */
  @Test public final void getTest() throws Exception {
    mMockUnit.get(Unit.SIGNAL);
  }

  /** @throws Exception by some issues */
  @Test public final void applyTest() throws Exception {
    final int id = 1; final String args = "";
    mMockUnit.apply(Manager.Action.create(id, args), Unit.SIGNAL);
  }


  /** The mock unit. */
  private static final class MockUnit extends Unit<String> {

    /** The test value. */
    private static final String TEST_VALUE = "Hello, World";

    /** {@inheritDoc} */
    private MockUnit() throws OperationCanceledException
    {super(); mStubValue = TEST_VALUE;}

  }

}
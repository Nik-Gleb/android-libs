/*
 * WidgetsTest.java
 * data
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

package widgets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;

import data.DataResource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 23/06/2018
 */
@ExtendWith(MockitoExtension.class)
final class WidgetsTest {

  /** Map mock. */
  private final Map mMap = mock(Map.class);

  /* Calculator mock. */
  //@InjectMocks private final MyClass mCalculator = new MyClass(mMap);

  /** Setup test. */
  @BeforeEach final void setUp() {
    System.out.println("MyClassTest.setUp");
    
  }

  /** Reset test. */
  @AfterEach final void tearDown() {
    System.out.println("MyClassTest.tearDown");
  }

  @Test final void sum() {
    //when(mMap.size()).thenReturn(3);

    //System.out.println("MyClassTest.sum " + mCalculator.sum(1, 2));

    //System.out.println("MyClassTest.sum " + mCalculator.getCode(new Bundle()));

    final byte[] bytes = DataResource.toBytes(-
      326);
    System.out.println("WidgetsTest.sum " + Arrays.toString(bytes));
    System.out.println("WidgetsTest.sum " + DataResource.fromBytes(bytes));
  }
}
/*
 * CPToolsTest.java
 * cp-tools
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

package repository;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicClassMembers;

/**
 * BaseTest Tests.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/06/2017
 */
@Keep
@KeepPublicClassMembers
@SuppressWarnings("EmptyMethod")
@RunWith(AndroidJUnit4.class)
public final class BaseTest {

  /** @throws Exception by any fails */
  @Before
  public final void setUp () throws Exception {}

  /** @throws Exception by any fails */
  @After
  public final void tearDown () throws Exception {}

  /**
   * Test for
   *
   * @throws Exception by some fails
   */
  @Test
  public final void testMain () throws Exception {

    final Selection selection = Selection.create()
        .where("a", "1", "2")
        .where("b", "3", "4")
        .build();

    Log.d("TAG", "testMain: " + selection.getSelection());
    Log.d("TAG", Arrays.toString(selection.getSelectionArgs()));
  }

}
/*
 * 	ExampleInstrumentedTest.java
 * 	uploader
 *
 * 	Copyright (C) 2017, E-Toolkit ltd. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of E-Toolkit limited and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to E-Toolkit limited and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from E-Toolkit limited.
 */

package com.etoolkit.photoeditor.uploader;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
  @Test
  public void useAppContext() throws Exception {
    // Context of the app under test.
    Context appContext = InstrumentationRegistry.getTargetContext();

    assertEquals("com.etoolkit.photoeditor.loveuploader.test",
        appContext.getPackageName());
  }
}

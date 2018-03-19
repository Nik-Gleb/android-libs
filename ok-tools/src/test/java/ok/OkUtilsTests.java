/*
 * OkUtilsTests.java
 * ok-tools
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

package ok;

import org.junit.Test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static ok.OkUtils.perform;

/**
 * OkHttpUtils Tests.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/06/2017
 */
public final class OkUtilsTests {


  /** Testing performs. */
  @Test public final void performTest() throws Exception {

    final String name = "test.txt";

    final File file = new File("test.txt");
    if (!file.createNewFile())
      new PrintWriter(name).close();
    file.deleteOnExit();

    final long timeOut = 1800;
    final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    final OkHttpClient okHttpClient =
        new OkHttpClient.Builder()
            .connectTimeout (timeOut, timeUnit)
            .build();

    final HttpUrl httpUrl =
        new HttpUrl.Builder()
            .scheme("https")
            .host("httpbin.org")
            .addPathSegment("delay")
            .addPathSegment("2")
            .build();

    final Request request =
        new Request.Builder()
            .url(httpUrl)
            .get()
            .build();

    final Call call = okHttpClient.newCall(request);

    /*final CancellationSignal signal = new DefaultCancellationSignal();//
    /*new Thread(() -> {
      try {
        Thread.sleep(4000);
        signal.cancel();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }).start();*/

    final MockPipes pipes = new MockPipes(file);

    final FileDescriptor fileDescriptor = perform(call, pipes);

    final FileInputStream fileInputStream =
        new FileInputStream(fileDescriptor);
    Thread.sleep(20);
    copyStreams(fileInputStream, System.out);
    fileInputStream.close();
  }

  /**
   * Copy streams.
   *
   * @param in input stream
   * @param out output stream
   *
   * @throws IOException the I/O Exception
   */
  @SuppressWarnings("SameParameterValue")
  private static void copyStreams(InputStream in, OutputStream out) throws IOException {
    final byte[] buffer = new byte[512]; int len;
    while ((len = in.read(buffer)) != -1)
      out.write(buffer, 0, len);
  }

  /** The Mock Pipes */
  private static final class MockPipes implements OkUtils.PipeFactory {

    /** The output file. */
    private final File mFile;

    /**
     * Constructs a new {@link MockPipes} with a target file.
     *
     * @param file target file
     */
    MockPipes(File file) {mFile = file;}

    /** Create new descriptors-pair. */
    @Override public final FileDescriptor[] create() {

      try {
        final FileDescriptor write = new FileOutputStream(mFile).getFD();
        final FileDescriptor read = new FileInputStream(mFile).getFD();
        return new FileDescriptor[] {read, write};
      } catch (IOException e) {throw new RuntimeException(e);}
    }

    /** Open file descriptor. */
    @Override public final FileOutputStream openFd(FileDescriptor fd)
    {return new FileOutputStream(fd);}
  }


}
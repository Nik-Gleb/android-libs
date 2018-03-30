/*
 * NetworkUtils.java
 * network
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

package network;

import android.content.res.AssetFileDescriptor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import ok.OkUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Common network utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 13/06/2017
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public final class NetworkUtils {

  /** The log-cat tag */
  private static final String TAG = "pipe";
  /** The start offset */
  private static final long START_OFFSET = 0;

  /* Static initialization. */
  static {
    try {System.loadLibrary(TAG);}
    catch (UnsatisfiedLinkError e)
    {e.printStackTrace();}
  }

  /**
   * @param read the read-side file descriptor
   * @param write the write-side file descriptor
   * @return true if a native fd's was attached, otherwise - false
   */
  native private static boolean createPipe(FileDescriptor read, FileDescriptor write);

  /**
   * @param fd source file descriptor
   * @return target parcel file descriptor
   */
  native private static ParcelFileDescriptor createPfd(FileDescriptor fd);

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private NetworkUtils() {throw new AssertionError();}

  /**
   * Perform Http Operation.
   *
   * @param call the http-ok call
   * @param signal the cancellation signal
   * @return response's file descriptor
   *
   * @throws IOException response exceptions
   */
  @SuppressWarnings("Convert2MethodRef")
  public static AssetFileDescriptor perform (Call call, CancellationSignal signal) throws IOException {

    if (call == null) throw new IllegalArgumentException("call == null");
    if (signal == null) throw new IllegalArgumentException("canceller == null");

    //noinspection Convert2Lambda

    cancelable(call, signal);

    final OkUtils.IOLock lock = new OkUtils.IOLock();

    call.enqueue(new Callback() {

      /** {@inheritDoc} */
      @Override public final void onFailure(Call call, IOException exception)
      {forward(exception, lock);}

      /** {@inheritDoc} */
      @Override
      public final void onResponse(Call call, Response response) throws IOException {
        final ResponseBody body = response.body();
        try {
          if (body != null) {
            lock.length = body.contentLength();
            lock.mediaType = body.contentType();
            forward(body.source(), lock);
          } else onFailure(call, new IOException("body == null"));
        } finally {response.close();}
      }
    });

    final IOException exception = lock.getException();
    if (exception != null) throw exception;
    else return getAssetFileDescriptor(lock);
  }


  /**
   * @param lock source lock
   * @return result parcel file descriptor
   * @throws IOException can't create pfd
   */
  public static AssetFileDescriptor getAssetFileDescriptor(OkUtils.IOLock lock)
      throws IOException {
    final FileDescriptor fileDescriptor = lock.getFileDescriptor();
    final ParcelFileDescriptor parcelFileDescriptor = PipeFactory.create(fileDescriptor);
    return new AssetFileDescriptor(parcelFileDescriptor, START_OFFSET, lock.length);}

  /**
   * @param file the source file
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(File file, OkUtils.IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(file)), lock);}

  /**
   * @param socket the source socket
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(Socket socket, OkUtils.IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(socket)), lock);}

  /**
   * @param in the source stream
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(InputStream in, OkUtils.IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(in)), lock);}

  /**
   * @param src the source
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(Source src, OkUtils.IOLock lock) throws IOException {
    final PipeFactory factory = new PipeFactory();
    if (src == null) throw new IllegalArgumentException("src == null");
    if (lock == null) throw new IllegalArgumentException("lock == null");
    final FileDescriptor[] pair = factory.create(); lock.setFileDescriptor(pair[0]);
    final BufferedSink sink = Okio.buffer(Okio.sink(factory.openFd(pair[1])));
    try {sink.writeAll(src);} finally {Util.closeQuietly(sink);}
  }

  /**
   * @param exception the throwable of reading source
   * @param lock the lock-object
   */
  public static void forward(IOException exception, OkUtils.IOLock lock) {lock.setException(exception);}

  /**
   * Wrap Ok-Http's call to cancelable.
   *
   * @param call the call
   * @param signal the canceler
   */
  @SuppressWarnings("SameParameterValue")
  private static void cancelable(final Call call, CancellationSignal signal)
  {signal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
    @Override
    public void onCancel() {call.cancel();}
  });}

  /**
   * @author Nikitenko Gleb
   * @since 1.0, 20/06/2017
   */
  @SuppressWarnings("unused")
  public static final class PipeFactory implements OkUtils.PipeFactory {

    /**
     * @param fd source file descriptor
     * @return target parcel file descriptor
     * @throws IOException can't create the parcel file descriptor
     */
    static ParcelFileDescriptor create(FileDescriptor fd) throws IOException {
      final ParcelFileDescriptor result = createPfd(fd);
      if (result != null) return result;
      throw new IOException("Can't create pfd");
    }

    /** {@inheritDoc} */
    @Override
    public final FileDescriptor[] create() throws IOException {
      final FileDescriptor[] result = new FileDescriptor[]
          {new FileDescriptor(), new FileDescriptor()};
      if (!createPipe(result[0], result[1]) ||
          !result[0].valid() || !result[1].valid())
        throw new IOException("Can't create pipe");
      return result;
    }

    /** {@inheritDoc} */
    @Override
    public final FileOutputStream openFd(FileDescriptor fileDescriptor) throws IOException
    {return new ParcelFileDescriptor.AutoCloseOutputStream(create(fileDescriptor));}

  }

}

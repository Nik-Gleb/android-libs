/*
 * OkUtils.java
 * ok-tools
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

package ok;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import clean.CancellationSignal;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Common OK utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 13/06/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class OkUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private OkUtils() {throw new AssertionError();}

  /**
   * Perform Http Operation.
   *
   * @param call the http-ok call
   * @param signal the canceller
   * @param pipe the pipe factory
   * @return response's file descriptor
   *
   * @throws IOException response exceptions
   */
  public static FileDescriptor perform (Call call, CancellationSignal signal,
      final PipeFactory pipe) throws IOException {

    if (call == null) throw new IllegalArgumentException("call == null");
    if (signal == null) throw new IllegalArgumentException("canceller == null");
    if (pipe == null) throw new IllegalArgumentException("pipe == null");

    cancelable(call, signal);
    final IOLock lock = new IOLock();

    call.enqueue(new Callback() {

      /** {@inheritDoc} */
      @Override public final void onFailure(Call call, IOException exception)
      {forward(exception, lock);}

      /** {@inheritDoc} */
      @Override
      public final void onResponse(Call call, Response response) throws IOException {
        final ResponseBody body = response.body();
        try {
          if (body != null) forward(body.source(), pipe, lock);
          else onFailure(call, new IOException("body == null"));
        } finally {response.close();}
      }
    });

    final IOException exception = lock.getException();
    if (exception != null) throw exception;
    else return lock.getFileDescriptor();
  }

  /**
   * Wrap Ok-Http's call to cancelable.
   *
   * @param call the call
   * @param signal the canceler
   */
  @SuppressWarnings("SameParameterValue")
  public static void cancelable(final Call call, CancellationSignal signal)
  {signal.setOnCancelListener(call::cancel);}

  /**
   * @param file the source file
   * @param factory the file-descriptors factory
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(File file, PipeFactory factory, IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(file)), factory, lock);}

  /**
   * @param socket the source socket
   * @param factory the file-descriptors factory
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(Socket socket, PipeFactory factory, IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(socket)), factory, lock);}

  /**
   * @param in the source stream
   * @param factory the file-descriptors factory
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(InputStream in, PipeFactory factory, IOLock lock) throws IOException
  {forward(Okio.buffer(Okio.source(in)), factory, lock);}

  /**
   * @param src the source
   * @param factory the file-descriptors factory
   * @param lock the lock-object
   *
   * @throws IOException forward exceptions
   */
  public static void forward(Source src, PipeFactory factory, IOLock lock) throws IOException {
    if (src == null) throw new IllegalArgumentException("src == null");
    if (factory == null) throw new IllegalArgumentException("factory == null");
    if (lock == null) throw new IllegalArgumentException("lock == null");
    final FileDescriptor[] pair = factory.create(); lock.setFileDescriptor(pair[0]);
    final BufferedSink sink = Okio.buffer(Okio.sink(factory.openFd(pair[1])));
    try {sink.writeAll(src);} finally {Util.closeQuietly(sink);}
  }

  /**
   * @param exception the throwable of reading source
   * @param lock the lock-object
   */
  public static void forward(IOException exception, IOLock lock) {lock.setException(exception);}

  /** The lockable i/o. */
  public static final class IOLock {

    /** The content length. */
    public long length = -1;
    /** The content type of response. */
    public MediaType mediaType = null;

    /** The file descriptor. */
    private FileDescriptor mFileDescriptor = null;
    /** The io-exception. */
    private IOException mException = null;

    /** @param fileDescriptor the fileDescriptor for setup */
    public final synchronized void setFileDescriptor
    (FileDescriptor fileDescriptor) {
      if (fileDescriptor == null)
        throw new IllegalArgumentException("fileDescriptor == null");
      mFileDescriptor = fileDescriptor;
      notify();
    }

    /** @param exception the exception for setup */
    public synchronized void setException
    (IOException exception) {
      if (exception == null)
        throw new IllegalArgumentException("exception == null");
      mException = exception;
      notify();
    }

    /** @return the file descriptor */
    public synchronized final FileDescriptor getFileDescriptor() {
      while (mFileDescriptor == null && mException == null)
        try {wait();} catch (InterruptedException e)
        {throw new RuntimeException("Unexpected interruption");}
      return mFileDescriptor;
    }

    /** @return the exception */
    public synchronized final IOException getException() {
      while (mFileDescriptor == null && mException == null)
        try {wait();} catch (InterruptedException e)
        {throw new RuntimeException("Unexpected interruption");}
      return mException;
    }

  }


  /** The pipe's factory */
  public interface PipeFactory {
    /** Create new descriptors-pair */
    FileDescriptor[] create() throws IOException;
    /** Open file descriptor */
    FileOutputStream openFd(FileDescriptor fd) throws IOException;
  }

  /** The stream-body */
  public static final class StreamBody extends RequestBody {

    /** The media type of body. */
    private final MediaType mMediaType;
    /** The body content. */
    private final FileInputStream mContent;
    /** The content length. */
    private final long mLength;

    /**
     * Constructs a new {@link StreamBody} with content and type
     *
     * @param mediaType the content type
     * @param content the content body
     */
    public StreamBody(MediaType mediaType, FileInputStream content, long length) {
      if (mediaType == null)
        throw new IllegalArgumentException("mediaType == null");
      if (content == null)
        throw new IllegalArgumentException("content == null");

      mMediaType = mediaType;
      mContent = content;
      mLength = length;
    }

    /** {@inheritDoc} */
    @Override public final long contentLength() {return mLength;}

    /** Returns the Content-Type header for this body. */
    @Override public final MediaType contentType() {
      return mMediaType;
    }

    /** Writes the content of this request to {@code out}. */
    @Override public final void writeTo(BufferedSink sink) throws IOException
    {sink.writeAll(Okio.buffer(Okio.source(mContent))); Util.closeQuietly(mContent);}
  }
}
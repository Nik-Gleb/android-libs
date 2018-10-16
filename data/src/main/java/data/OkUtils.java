/*
 * OkUtils.java
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

package data;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static android.os.ParcelFileDescriptor.createReliablePipe;
import static android.os.Process.THREAD_PRIORITY_DEFAULT;
import static android.os.Process.setThreadPriority;
import static java.util.Objects.requireNonNull;
import static okhttp3.MediaType.parse;

/**
 * OK-IPC Utils
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
@SuppressWarnings("unused")
final class OkUtils {

  /** Log cat tag. */
  private static final String TAG = "Transfer";

  /** I/O Based Thread Pool Executor. */
  static final ExecutorService EXECUTOR = newThreadPool();

  /** Const keys. */
  @SuppressWarnings("WeakerAccess")
  static final String TYPE = "type";

  /** Request const. */
  static String REQUEST = "request";

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private OkUtils() {throw new AssertionError();}

  /**
   * @param descriptor target descriptor
   * @param data source request
   */
  static void write
  (@NonNull AssetFileDescriptor descriptor, @NonNull RequestBody data)
  {flash(descriptor.getParcelFileDescriptor(), data::writeTo);}

  /**
   * @param data input content
   *
   * @return asset file descriptor
   */
  @NonNull static AssetFileDescriptor fromRequest(@NonNull RequestBody data) {
    try {
      final ParcelFileDescriptor[] pipe = createReliablePipe();
      final Bundle extras = mediaType(data.contentType());
      try {return new AssetFileDescriptor(pipe[0], 0, data.contentLength(), extras);}
      finally {flash(pipe[1], data::writeTo);}
    } catch(IOException exception) {throw new CompletionException(exception);}
  }

  /**
   * @param descriptor asset file descriptor
   *
   * @return input content
   */
  @NonNull static RequestBody toRequest(@NonNull AssetFileDescriptor descriptor) {
    try {
      final BufferedSource source = OkUtils.source
        (descriptor.createInputStream());
      return new RequestBody() {
        @Nullable @Override
        public final MediaType contentType()
        {return mediaType(descriptor.getExtras());}
        @Override public final long contentLength()
        {return descriptor.getLength();}
        @Override public final void writeTo(@NonNull BufferedSink sink)
          throws IOException {sink.writeAll(source);}
      };
    } catch (IOException exception) {throw new CompletionException(exception);}
  }

  /**
   * @param data output content
   *
   * @return asset file descriptor
   */
  @NonNull static AssetFileDescriptor fromResponse(@NonNull ResponseBody data) {
    try {
      final ParcelFileDescriptor[] pipe = createReliablePipe();
      final Bundle extras = mediaType(data.contentType());
      try {return new AssetFileDescriptor(pipe[0], 0, data.contentLength(), extras);}
      finally {flash(pipe[1], data.source());}
    } catch(IOException exception) {throw new CompletionException(exception);}
  }

  /**
   * @param descriptor asset file descriptor
   *
   * @return output content
   */
  @NonNull static ResponseBody toResponse(@NonNull AssetFileDescriptor descriptor) {
    try {
      final BufferedSource source = OkUtils.source
        (descriptor.createInputStream());
      return new ResponseBody() {
        @Nullable @Override public final MediaType contentType()
        {return mediaType(descriptor.getExtras());}
        @Override public final long contentLength()
        {return descriptor.getLength();}
        @Override public final BufferedSource source()
        {return source;}
      };
    } catch (IOException exception) {throw new CompletionException(exception);}
  }

  /**
   * @param input input stream
   * @return buffered source
   */
  @NonNull static BufferedSource source (@NonNull InputStream input)
  {return Okio.buffer(Okio.source(input));}

  /**
   * @param output output stream
   * @return buffered sink
   */
  @NonNull private static BufferedSink sink(@NonNull OutputStream output)
  {return Okio.buffer(Okio.sink(toSync(output)));}

  /**
   * @param output source output stream
   * @return synced output stream
   */
  @NonNull private static OutputStream
  toSync(@NonNull OutputStream output) {
    return new OutputStream() {
      @Override public final void write(int data)
        throws IOException {output.write(data);}
      @Override public final void write(@NonNull byte[] data)
          throws IOException {output.write(data);}
      @Override public final void write(@NonNull byte[] data, int off, int len)
          throws IOException {output.write(data, off, len);}
      @Override public final void flush()
          throws IOException {output.flush(); sync(output);}
      @Override public final void close()
          throws IOException {flush(); output.close();}
    };
  }

  /** @param output source output stream */
  private static void sync(@NonNull OutputStream output)
  {if (output instanceof FileOutputStream)
    try {((FileOutputStream)output).getFD().sync();}
    catch (IOException ignored) {}
  }

  @NonNull private static Bundle mediaType(@Nullable MediaType type) {
    final Bundle extras; if (type == null) extras = Bundle.EMPTY;
    else {extras = new Bundle(); extras.putString(TYPE, type.toString());}
    return extras;
  }

  @Nullable static MediaType mediaType(@Nullable Bundle type) {
    return type == Bundle.EMPTY || type == null ? null :
        parse(requireNonNull(type.getString(TYPE)));
  }

  /**
   * @param descriptor target file descriptor
   * @param request request instance
   */
  private static void flash(@NonNull ParcelFileDescriptor descriptor,
    @NonNull SinkConsumer request) {
    EXECUTOR.execute(() -> {
      final BufferedSink sink = sink(new AutoCloseOutputStream(descriptor));
      try {request.writeTo(sink);} catch (IOException exception) {
        try {descriptor.closeWithError(exception.getMessage());}
        catch (IOException e) {Log.w(TAG, e);}
      } finally {Util.closeQuietly(sink);}
    });
  }

  /**
   * @param descriptor target file descriptor
   * @param response response instance
   */
  @SuppressWarnings("UnusedReturnValue")
  private static void flash
  (@NonNull ParcelFileDescriptor descriptor, @NonNull BufferedSource response) {
    EXECUTOR.execute(() -> {
      final BufferedSink sink = sink(new AutoCloseOutputStream(descriptor));
      try {sink.writeAll(response);} catch (IOException exception) {
        try {descriptor.closeWithError(exception.getMessage());}
        catch (IOException e) {Log.w(TAG, e);}
      } finally {Util.closeQuietly(sink);}
    });
  }

  /** Sink Consumer. */
  @FunctionalInterface interface SinkConsumer {
    /** Writes the content of this request to sink. */
    @SuppressWarnings("RedundantThrows") void writeTo(@NonNull BufferedSink sink) throws IOException;
  }

  /** @return new create cached thread pool */
  @NonNull private static ExecutorService newThreadPool() {
    final int core = 0, max = Integer.MAX_VALUE;
    final long time = 60L; final TimeUnit unit = TimeUnit.SECONDS;
    final BlockingQueue<Runnable> queue = new SynchronousQueue<>();
    final SecurityManager security = System.getSecurityManager();
    final ThreadGroup group = security != null ?
      security.getThreadGroup() :
      Thread.currentThread().getThreadGroup();
    final String name = "Thread(I/O)-";
    final AtomicInteger number = new AtomicInteger(0);
    final ThreadFactory factory = runnable ->
      new Thread(group, runnable, name + number.getAndIncrement(), 0) {{
        setDaemon(false);
        setPriority(NORM_PRIORITY);
        priority =
          Process.THREAD_PRIORITY_DEFAULT +
          Process.THREAD_PRIORITY_LESS_FAVORABLE;
    }};
    return new ThreadPoolExecutor(core, max, time, unit, queue, factory);
  }

  /** Internal advanced thread */
  @SuppressWarnings("WeakerAccess")
  public static class Thread extends java.lang.Thread implements Consumer<Runnable> {

    /** Interruption listener. */
    private volatile Runnable mHook = null;

    /** Process priority */
    int priority = THREAD_PRIORITY_DEFAULT;

    /** {@inheritDoc} */
    public Thread() {}

    /** {@inheritDoc} */
    public Thread(Runnable runnable) {super(runnable);}

    /** {@inheritDoc} */
    public Thread(ThreadGroup threadGroup, Runnable runnable)
    {super(threadGroup, runnable);}

    /** {@inheritDoc} */
    public Thread(String s) {super(s);}

    /** {@inheritDoc} */
    public Thread(ThreadGroup threadGroup, String s) {super(threadGroup, s);}

    /** {@inheritDoc} */
    public Thread(Runnable runnable, String s) {super(runnable, s);}

    /** {@inheritDoc} */
    public Thread(ThreadGroup threadGroup, Runnable runnable, String s)
    {super(threadGroup, runnable, s);}

    /** {@inheritDoc} */
    public Thread(ThreadGroup threadGroup, Runnable runnable, String s, long l)
    {super(threadGroup, runnable, s, l);}

    /** {@inheritDoc} */
    @Override public final void run()
    {setThreadPriority(priority); super.run();}

    /** {@inheritDoc} */
    @Override public final void accept(@Nullable Runnable hook)
    {mHook = hook;}

    /** {@inheritDoc} */
    @Override public final void interrupt() {
      final Runnable hook = mHook;
      if (hook != null) hook.run();
      else super.interrupt();
    }
  }
}

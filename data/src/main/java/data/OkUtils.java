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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static android.os.ParcelFileDescriptor.createReliablePipe;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
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
  private static final String TAG = "OkUtils";

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
  static void write (@NonNull AssetFileDescriptor descriptor, @NonNull RequestBody data)
  {flash(descriptor.getParcelFileDescriptor(), data::writeTo, Runnable::run);}

  /**
   * @param data input content
   * @param executor transfer executor
   *
   * @return asset file descriptor
   */
  @NonNull static AssetFileDescriptor fromRequest
  (@NonNull RequestBody data, @Nullable Executor executor) throws IOException {
    final ParcelFileDescriptor[] pipe = createReliablePipe();
    final Bundle extras = mediaType(data.contentType());
    try {return new AssetFileDescriptor(pipe[0], 0, data.contentLength(), extras);}
    finally {flash(pipe[1], data::writeTo, executor);}
  }

  /**
   * @param descriptor asset file descriptor
   *
   * @return input content
   */
  @NonNull static RequestBody toRequest(@NonNull AssetFileDescriptor descriptor) {
    return new RequestBody() {

      /** {@inheritDoc} */
      @Nullable @Override
      public final MediaType contentType()
      {return mediaType(descriptor.getExtras());}

      /** {@inheritDoc} */
      @Override public final long contentLength()
      {return descriptor.getLength();}

      /** {@inheritDoc} */
      @Override
      public final void
      writeTo(@NonNull BufferedSink sink) throws IOException {
        try (final BufferedSource source = source(descriptor.createInputStream()))
        {sink.writeAll(source);}
      }
    };
  }

  /**
   * @param data output content
   *
   * @return asset file descriptor
   */
  @NonNull static AssetFileDescriptor fromResponse
  (@NonNull ResponseBody data, @Nullable Executor executor) throws IOException {
    final ParcelFileDescriptor[] pipe = createReliablePipe();
    final Bundle extras = mediaType(data.contentType());
    try {return new AssetFileDescriptor(pipe[0], 0, data.contentLength(), extras);}
    finally {flash(pipe[1], data.source(), executor);}
  }

  /**
   * @param descriptor asset file descriptor
   *
   * @return output content
   */
  @NonNull static ResponseBody toResponse(@NonNull AssetFileDescriptor descriptor) throws IOException {
    return new ResponseBody() {

      /** Buffered source. */
      private final BufferedSource mSource =
          OkUtils.source(descriptor.createInputStream());

      /** {@inheritDoc} */
      @Nullable @Override
      public final MediaType contentType()
      {return mediaType(descriptor.getExtras());}

      /** {@inheritDoc} */
      @Override public final long contentLength()
      {return descriptor.getLength();}

      /** {@inheritDoc} */
      @Override public final BufferedSource source()
      {return mSource;}
    };
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
  @NonNull private static OutputStream toSync(@NonNull OutputStream output) {
    return new OutputStream() {
      /** {@inheritDoc} */
      @Override public final void write(int data)
          throws IOException {output.write(data);}
      /** {@inheritDoc} */
      @Override public final void write(@NonNull byte[] data)
          throws IOException {output.write(data);}
      /** {@inheritDoc} */
      @Override public final void write(@NonNull byte[] data, int off, int len)
          throws IOException {output.write(data, off, len);}
      /** {@inheritDoc} */
      @Override public final void flush()
          throws IOException {output.flush(); sync(output);}
      /** {@inheritDoc} */
      @Override public final void close()
          throws IOException {flush(); output.close();}
    };
  }

  /**
   * @param output source output stream
   *
   * @throws IOException I/O exception
   */
  private static void sync(@NonNull OutputStream output) throws IOException
  {if (output instanceof FileOutputStream) ((FileOutputStream)output).getFD().sync();}

  @NonNull private static Bundle mediaType(@Nullable MediaType type) {
    final Bundle extras; if (type == null) extras = Bundle.EMPTY;
    else {extras = new Bundle(); extras.putString(TYPE, type.toString());}
    return extras;
  }

  @Nullable private static MediaType mediaType(@Nullable Bundle type) {
    return type == Bundle.EMPTY || type == null ? null :
        parse(requireNonNull(type.getString(TYPE)));
  }

  /**
   * @param descriptor target file descriptor
   * @param request request instance
   * @param executor transfer executor
   *
   * @return completable result
   */
  @SuppressWarnings("UnusedReturnValue")
  @NonNull private static CompletableFuture<Void> flash
  (@NonNull ParcelFileDescriptor descriptor, @NonNull SinkConsumer request, @Nullable Executor executor) {
    final Runnable task = () -> {
      try (final BufferedSink sink = sink(new AutoCloseOutputStream(descriptor))) {
        try {request.writeTo(sink);} catch (IOException exception) {
          try {descriptor.closeWithError(exception.getMessage());}
          catch (IOException e) {Log.w(TAG, e);}
        }
      } catch (IOException exception) {throw new CompletionException(exception);}
    }; return executor == null ? runAsync(task) : runAsync(task, executor);
  }

  /**
   * @param descriptor target file descriptor
   * @param response response instance
   * @param executor transfer executor
   *
   * @return completable result
   */
  @SuppressWarnings("UnusedReturnValue")
  @NonNull private static CompletableFuture<Void> flash
  (@NonNull ParcelFileDescriptor descriptor, @NonNull BufferedSource response, @Nullable Executor executor) {
    final Runnable task = () -> {
      try (final BufferedSink sink = sink(new AutoCloseOutputStream(descriptor))) {
        try {sink.writeAll(response);} catch (IOException exception) {
          try {descriptor.closeWithError(exception.getMessage());}
          catch (IOException e) {Log.w(TAG, e);}
        }
      } catch (IOException exception) {throw new CompletionException(exception);}
    }; return executor == null ? runAsync(task) : runAsync(task, executor);
  }

  /** Sink Consumer. */
  @FunctionalInterface interface SinkConsumer {
    /** Writes the content of this request to sink. */
    @SuppressWarnings("RedundantThrows") void writeTo(@NonNull BufferedSink sink) throws IOException;
  }


}

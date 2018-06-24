package data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static okhttp3.MediaType.parse;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/06/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
public interface DataSource extends Closeable {

  /** Bundle keys. */
  String TYPE = "type", MODE = "mode", READ = "r", WRITE = "w";

  /** @return base content uri */
  @NonNull
  Uri getContentUri();

  /**
   * @param table table name
   * @return uri address
   */
  @NonNull default Uri getTable(@NonNull String table)
  {return getContentUri().buildUpon().path(table).build();}

  /**
   * @param table table name
   * @param id id of row
   *
   * @return uri address
   */
  @NonNull default Uri getRow(@NonNull String table, long id)
  {return ContentUris.withAppendedId(getTable(table), id);}

  /**
   * @param table table name
   * @param key string key
   *
   * @return uri address
   */
  @NonNull default Uri getRow(@NonNull String table, @NonNull String key)
  {return getRow(table, key.hashCode() & 0x00000000ffffffffL);}

  /** {@inheritDoc} */
  @Override
  void close();

  /**
   * @param uri data resource
   * @param data input content
   *
   * @return output content
   */
  @Nullable
  AssetFileDescriptor transact
  (@NonNull Uri uri, @Nullable AssetFileDescriptor data);

  /**
   * @param uri data resource
   * @param data input content
   *
   * @return output content
   */
  @Nullable default ResponseBody transact
  (@NonNull Uri uri, @Nullable RequestBody data)  {
    final AssetFileDescriptor input = data != null ? fromRequest(data) : null;
    final AssetFileDescriptor output = transact(uri, input);
    return output != null ? toResponse(output) : null;
  }

  /**
   * @param uri the data resource
   * @return stream of rows
   */
  @NonNull default Cursor cursor(@NonNull Uri uri)
  {throw new UnsupportedOperationException();}

  /**
   * @param uri the data resource
   * @return stream of rows
   */
  @NonNull default Stream<TableRow> query(@NonNull Uri uri)
  {return TableRow.stream(cursor(uri));}

  /**
   * @param uri data resource
   * @param raw raw bytes
   */
  default void put(@NonNull Uri uri, @NonNull byte[] raw)
  {throw new UnsupportedOperationException();}

  /** @param uri data resource */
  default void delete(@NonNull Uri uri)
  {throw new UnsupportedOperationException();}

  /**
   * @param uri data resource
   * @param observer content observer
   */
  @NonNull default ContentObserver register
  (@NonNull Uri uri, @NonNull BiConsumer<Boolean, Uri> observer,
      @Nullable Handler handler)
  {throw new UnsupportedOperationException();}

  /** @param observer content observer */
  default void unregister(@NonNull ContentObserver observer)
  {throw new UnsupportedOperationException();}

  /**
   * @param data input content
   *
   * @return asset file descriptor
   */
  @NonNull static AssetFileDescriptor fromRequest(@NonNull RequestBody data) {
    try {
      final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
      final Bundle extras; final MediaType type = data.contentType();

      if (type == null) extras = Bundle.EMPTY;
      else {extras = new Bundle();
      extras.putString(TYPE, type.toString());}

      try {return new AssetFileDescriptor
          (pipe[0], 0, data.contentLength(), extras);}
      finally {
        runAsync(() -> {
          try (final BufferedSink sink = sink
              (new AutoCloseOutputStream(pipe[1])))
          {data.writeTo(sink);} catch (IOException exception)
          {throw new RuntimeException(exception);}
        });
      }

    } catch (IOException exception)
    {throw new RuntimeException(exception);}
  }

  /**
   * @param descriptor descriptor
   *
   * @return request body
   */
  @NonNull static RequestBody toRequest(@NonNull AssetFileDescriptor descriptor) {
    return new RequestBody() {

      /** {@inheritDoc} */
      @Nullable @Override
      public final MediaType contentType() {
        final Bundle extras = descriptor.getExtras();
        return extras == Bundle.EMPTY ? null :
            parse(requireNonNull(extras.getString(TYPE)));
      }

      /** {@inheritDoc} */
      @Override
      public final void
      writeTo(@NonNull BufferedSink sink) throws IOException {
        try (final BufferedSource source = source
            (descriptor.createInputStream()))
        {source.readAll(sink);}
      }

      /** {@inheritDoc} */
      @Override
      public final long contentLength()
      {return descriptor.getLength();}
    };
  }

  /**
   * @param data output content
   *
   * @return descriptor
   */
  @NonNull static AssetFileDescriptor fromResponse(@NonNull ResponseBody data) {
    try {
      final ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
      final Bundle extras; final MediaType type = data.contentType();

      if (type == null) extras = Bundle.EMPTY;
      else {extras = new Bundle();
        extras.putString(TYPE, type.toString());}

      try {return new AssetFileDescriptor
          (pipe[0], 0, data.contentLength(), extras);}
      finally {
        runAsync(() -> {
          try (final BufferedSink sink = sink
              (new AutoCloseOutputStream(pipe[1]))) {
            data.source().readAll(sink);
          } catch (IOException exception) {
            throw new RuntimeException(exception);
          }
        });
      }
    } catch (IOException exception)
    {throw new RuntimeException(exception);}
  }

  /**
   * @param descriptor descriptor
   *
   * @return response body
   */
  @NonNull static ResponseBody toResponse(
      @NonNull AssetFileDescriptor descriptor) {
    return new ResponseBody() {

      /** Buffered source. */
      final BufferedSource mSource;

      {try {mSource = DataSource.source(descriptor.createInputStream());}
        catch (IOException exception) {throw new RuntimeException(exception);}}

      /** {@inheritDoc} */
      @Nullable @Override
      public final MediaType contentType() {
        final Bundle extras = descriptor.getExtras();
        return extras == Bundle.EMPTY || extras == null ? null :
            parse(requireNonNull(extras.getString(TYPE)));
      }

      /** {@inheritDoc} */
      @Override
      public final long contentLength()
      {return descriptor.getLength();}

      /** {@inheritDoc} */
      @Override
      public final
      BufferedSource source()
      {return mSource;}

    };
  }

  /**
   * @param input input stream
   * @return buffered source
   */
  @NonNull static BufferedSource source(@NonNull InputStream input)
  {return Okio.buffer(Okio.source(input));}

  /**
   * @param output output stream
   * @return buffered sink
   */
  @NonNull static BufferedSink sink(@NonNull OutputStream output)
  {return Okio.buffer(Okio.sink(output));}

  /**
   * @return access mode appended
   */
  @NonNull default Uri asReadResource()
  {return getContentUri().buildUpon().appendQueryParameter(MODE, READ).build();}

  /**
   * @return access mode appended
   */
  @NonNull default Uri asWriteResource()
  {return getContentUri().buildUpon().appendQueryParameter(MODE, WRITE).build();}

  /*
   * @param file the file resource for open
   * @return json-object representation
   */
  /*@NonNull default JSONObject jsonObject(@NonNull String file) {
    try (final BufferedSource source = source(file))
    {return new JSONObject(source.readUtf8());}
    catch (JSONException | IOException exception)
    {throw new RuntimeException(exception);}
  }*/

  /*
   * @param file the file resource for open
   * @return json-array representation
   */
  /*@NonNull default JSONArray jsonArray(@NonNull String file) {
    try (final BufferedSource source = source(file))
    {return new JSONArray(source.readUtf8());}
    catch (JSONException | IOException exception)
    {throw new RuntimeException(exception);}
  }*/

  /*
   * @param file    input file
   * @param parser  serialization parser
   * @param <T>     type of object
   * @return        object instance
   */
  /*@NonNull default <T> T load(@NonNull String file,
      @NonNull Function<BufferedSource, T> parser) {
    try(final BufferedSource source = source(input(file)))
    {return parser.apply(source);} catch (IOException exception)
    {throw new RuntimeException(exception);}
  }*/

  /*
   * @param file   output file
   * @param parser serialization parser
   * @param value  value for write
   * @param <T> type of value
   */
  /*default <T> void save(@NonNull String file, @NonNull T value,
      @NonNull BiConsumer<BufferedSink, T> parser) {
    try(final BufferedSink sink = sink(output(file)))
    {parser.accept(sink, value);}
    catch (IOException exception)
    {throw new RuntimeException(exception);}
  }*/

  /**
   * @param assets assets manager
   *
   * @return data source instance
   */
  @NonNull static DataSource create
  (@NonNull AssetManager assets)
  {return new AssetsSource(assets);}

  /**
   * @param resolver  content resolver
   * @param read      read permissions
   * @param write     write permissions
   *
   * @return data source instance
   */
  @NonNull static DataSource create(
      @NonNull ContentResolver resolver,
      @NonNull BooleanSupplier read,
      @NonNull BooleanSupplier write)
  {return new ExternalSource(resolver, write, read);}

  /** @return data source instance */
  @NonNull static DataSource create
  (@NonNull ContentResolver resolver, @NonNull String authority)
  {return new LocalSource(resolver, authority);}

}

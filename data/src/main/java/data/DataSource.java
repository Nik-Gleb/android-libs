package data;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.OperationCanceledException;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.text.TextUtils.isEmpty;
import static java.util.Collections.newSetFromMap;
import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
@Keep@KeepPublicProtectedClassMembers
@Singleton public final class DataSource implements Closeable {

  /** Access mode, mime type. */
  static final String MODE = "mode", TYPE = "type", DATA = "data";

  /** Content Resolver. */
  private final ContentResolver mResolver;

  /** Content provider client. */
  private final ContentProviderClient mClient;

  /** Cancellation signals. */
  private final Set<CancellationSignal> mCancels = newSetFromMap
      (new ConcurrentHashMap<CancellationSignal, Boolean>());

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link DataSource}
   *
   * @param resolver content resolver
   * @param authority content authority
   */
  @Inject public DataSource
  (@NonNull ContentResolver resolver, @NonNull String authority)
  {mClient = resolver.acquireContentProviderClient(authority);
  mResolver = resolver;}

  /** @param closeables for push */
  @Inject public final void inject
  (@NonNull @Named("stack") Stack<Closeable> closeables)
  {closeables.push(this);}

  /** {@inheritDoc} */
  @AnyThread @Override public final void close() {
    if (mClosed) return; mClosed = true;
    for (final Iterator<CancellationSignal> it = mCancels.iterator(); it.hasNext();)
    {final CancellationSignal signal = it.next(); it.remove(); signal.cancel();}
    mClient.close();
  }

  /** {@inheritDoc} */
  @AnyThread @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** Check not-closed state. */
  @WorkerThread private void checkState()
  {if (mClosed) throw new IllegalStateException("Already closed");}

  /**
   * @param uri       uri of resource
   * @param request   request body
   * @param executor  request executor
   *
   * @return  response of request
   */
  @WorkerThread @NonNull final ResponseBody call
  (@NonNull Uri uri, @NonNull RequestBody request, @NonNull Executor executor)
      throws IOException {return call(uri, OkUtils.fromRequest(request, executor));}

  /**
   * @param uri uri of resource
   *
   * @return response of request
   */
  @WorkerThread @NonNull final ResponseBody read(@NonNull Uri uri)
      throws IOException {return call(uri, null);}

  /** @param uri uri of resource */
  @WorkerThread final void write(@NonNull Uri uri, @NonNull RequestBody request)
      throws IOException {OkUtils.write(openFile(uri, null), request);}

  /**
   * @param uri   uri of resource
   * @param args  request arguments
   *
   * @return response of request
   */
  @WorkerThread @NonNull private ResponseBody call
  (@NonNull Uri uri, @Nullable AssetFileDescriptor args) throws IOException {
    final Bundle options = args == null ? Bundle.EMPTY : new Bundle();
    if (options != Bundle.EMPTY) options.putParcelable(OkUtils.REQUEST, args);
    return OkUtils.toResponse(openFile(uri, options));
  }

  /**
   * @param uri     uri resource
   * @param options open options
   *
   * @return resource descriptor
   */
  @WorkerThread @NonNull final AssetFileDescriptor openFile
  (@NonNull Uri uri, @Nullable Bundle options) throws IOException {
    final Set<String> keys = new HashSet<>(uri.getQueryParameterNames());
    final String mode = cutQuery(uri, keys, MODE);
    final String type = cutQuery(uri, keys, TYPE);
    final Uri.Builder builder = uri.buildUpon().clearQuery();
    for (final String key : keys) builder.appendQueryParameter
        (key, uri.getQueryParameter(key)); uri = builder.build();
    return !isEmpty(mode) ? openAssetFile(uri, mode) :
        openTypedAssetFileDescriptor(uri, isEmpty(type) ? "*/*" : type, options);
  }

  /**
   * @param uri   uri resource
   * @param keys  query keys
   * @param key   search key
   *
   * @return query value
   */
  @Nullable private static String cutQuery
  (@NonNull Uri uri, @NonNull Set<String> keys, @NonNull String key) {
    if (!keys.contains(key)) return null;
    else
      try {return uri.getQueryParameter(key);}
      finally {keys.remove(key);}
  }

  /**
   * @param uri   uri resource
   * @param mode  access mode
   *
   * @return descriptor result
   */
  @WorkerThread @NonNull private AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode) throws IOException {
    checkState(); final CancellationSignal cancel; mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.openAssetFile(uri, mode, cancel));}
    catch (OperationCanceledException | RemoteException e)
    {throw new IOException(e.getMessage());} finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri     uri resource
   * @param type    type of request
   * @param options bundle options
   *
   * @return        response data
   */
  @WorkerThread @NonNull private AssetFileDescriptor openTypedAssetFileDescriptor
  (@NonNull Uri uri, @NonNull String type, @Nullable Bundle options) throws IOException {
    checkState(); final CancellationSignal cancel; mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.openTypedAssetFileDescriptor(uri, type, options, cancel));}
    catch (OperationCanceledException | RemoteException e)
    {throw new IOException(e.getMessage());} finally {mCancels.remove(cancel);}
  }



  /**
   * @param uri           uri resource
   *
   * @return              stream of values
   */
  @WorkerThread @NonNull final <T> Stream<T> query(@NonNull Uri uri,  @Nullable String[] proj,
  @Nullable String sel, @Nullable String[] args, @Nullable String sort, @NonNull Function<Cursor, T> mapper) {
    checkState(); final CancellationSignal cancel; mCancels.add(cancel = new CancellationSignal());
    try {return toEntities(requireNonNull(mClient.query(uri, proj, sel, args, sort, cancel)), mapper);}
    catch (RemoteException exception) {throw new RuntimeException(exception);} finally {mCancels.remove(cancel);}
  }

  /**
   * @param cursor cursor
   * @param mapper cursor mapper
   *
   * @return stream of entities
   */
  @Keep @NonNull static <T> Stream<T> toEntities
  (@NonNull Cursor cursor, @NonNull Function<Cursor, T> mapper) {
    return StreamSupport.stream(((Iterable<T>) () -> new Iterator<T>() {
      private boolean hasNext = false; {setHasNext(cursor.moveToFirst());}
      @Override public final boolean hasNext() {return hasNext;}
      @Override @NonNull public final T next()
      {try {return mapper.apply(cursor);}
      finally {setHasNext(cursor.moveToNext());}}
      private void setHasNext(boolean value)
      {if(!(hasNext = value)) cursor.close();}
    }).spliterator(), false);
  }

  /**
   * @param uri uri of resource
   *
   * @return type of resource
   */
  @WorkerThread @NonNull final String getType(@NonNull Uri uri) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.getType(uri));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri uri of resource
   * @param mimeTypeFilter mime-filter
   *
   * @return stream types of resource
   */
  @WorkerThread @NonNull final String[] getStreamTypes
  (@NonNull Uri uri, @NonNull String mimeTypeFilter) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.getStreamTypes(uri, mimeTypeFilter));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /** {@inheritDoc} */
  @WorkerThread final void
  put(@NonNull Uri uri, @NonNull byte[] raw) {
    final boolean update; long id;
    try {id = ContentUris.parseId(uri);}
    catch (NumberFormatException exception) {id = -1;}
    if (id != -1) {
      final Cursor cursor = cursor(uri, new String[]{ BaseColumns._ID});
      update = cursor.getCount() == 1; cursor.close();
    } else update = false;
    final ContentValues values = new ContentValues();
    if (id != -1) values.put(BaseColumns._ID, id);
    values.put(DATA, raw);
    try {
      if (!update) mClient.insert(uri, values);
      else mClient.update(uri, values, null, null);
    } catch (RemoteException ignored) {}
  }

  /**
   * @param uri resource
   * @param projection columns
   *
   * @return cursor data
   */
  @SuppressWarnings("unused")
  @SuppressLint("Recycle")
  @NonNull private Cursor cursor(@NonNull Uri uri, @Nullable String[] projection) {
    Cursor cursor; try {cursor = mClient.query(uri, null, null, null, null);}
    catch (RemoteException exception) {cursor = null;}
    return cursor == null ? new MatrixCursor(new String[]{ BaseColumns._ID, DATA}) : cursor;
  }

  /**
   * @param uri    uri resource
   * @param values content values
   *
   * @return result uri
   */
  @WorkerThread @NonNull final Uri insert
  (@NonNull Uri uri, @Nullable ContentValues values) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.insert(uri, values));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri     uri resource
   * @param values  content values
   *
   * @return result count
   */
  @WorkerThread final int bulkInsert
  (@NonNull Uri uri, @NonNull ContentValues[] values) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.bulkInsert(uri, values));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri           uri resource
   * @param sel     selection string
   * @param args selection args
   *
   * @return result count
   */
  @WorkerThread final int delete
  (@NonNull Uri uri, @Nullable String sel, @Nullable String[] args) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.delete(uri, sel, args));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri uri resource
   */
  @WorkerThread final void delete(@NonNull Uri uri) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {mClient.delete(uri, null, null);}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param uri           uri of resource
   * @param values        content values
   * @param sel     selection string
   * @param args selection args
   *
   * @return  count of values
   */
  @WorkerThread final int update(@NonNull Uri uri,
  @Nullable ContentValues values, @Nullable String sel, @Nullable String[] args) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return mClient.update(uri, values, sel, args);}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /**
   * @param operations content provider operations
   *
   * @return apply operations results
   */
  @WorkerThread @NonNull final ContentProviderResult[] applyBatch
  (@NonNull ArrayList<ContentProviderOperation> operations) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.applyBatch(operations));}
    catch (RemoteException | OperationApplicationException e)
    {throw new RuntimeException(e);} finally {mCancels.remove(cancel);}
  }

  /**
   * @param method  custom method
   * @param arg     method arguments
   * @param extras  extras
   *
   * @return  bundle result
   */
  @WorkerThread @NonNull final Bundle call
  (@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    checkState(); final CancellationSignal cancel;
    mCancels.add(cancel = new CancellationSignal());
    try {return requireNonNull(mClient.call(method, arg, extras));}
    catch (RemoteException e) {throw new RuntimeException(e);}
    finally {mCancels.remove(cancel);}
  }

  /** {@inheritDoc} */
  @NonNull final ContentObserver register
  (@NonNull Uri uri, @NonNull BiConsumer<Boolean, Uri> observer,
      @Nullable Handler handler, boolean selfNotify, boolean descedants) {
    final ContentObserver result = new Observer(observer, handler, selfNotify);
    mResolver.registerContentObserver(uri, descedants, result);
    try {return result;} finally {mResolver.notifyChange(uri, result);}
  }

  /** {@inheritDoc} */
  final void unregister(@NonNull ContentObserver observer)
  {mResolver.unregisterContentObserver(observer);}

  /**
   * @param uri uri resource
   *
   * @return  data intent
   *
   * @throws IOException I/O Failure
   */
  @NonNull final Intent getIntent(@NonNull Uri uri) throws IOException {
    try {return new Intent().setDataAndTypeAndNormalize(uri = mClient.canonicalize(uri),
        mClient.getType(requireNonNull(uri))).addFlags("w".equals(uri.getQueryParameter(MODE)) ?
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION : Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .putExtra(Intent.EXTRA_STREAM, uri).putExtra(MediaStore.EXTRA_OUTPUT, uri);
    } catch (RemoteException | NullPointerException exception) {throw new IOException(exception.getMessage());}
  }

  /**
   * @param key key of row
   *
   * @return long id analog
   */
  static long keyToId(@NonNull String key)
  {return key.hashCode() & 0x00000000ffffffffL;}

  /** @return new created operations butch builder */
  @NonNull public final BatchOps applyBatch() {return new BatchOps(this);}

  /** Observer record. */
  private static final class Observer extends ContentObserver {

    /** Data mObserver. */
    private final BiConsumer<Boolean, Uri> mObserver;

    /** Deliver self notification. */
    private final boolean mSelfNotify;

    /**
     * Creates a content mObserver.
     *
     * @param observer file mObserver
     */
    Observer(@NonNull BiConsumer<Boolean, Uri> observer, @Nullable Handler handler, boolean selfNotify)
    {super(handler); this.mObserver = observer; mSelfNotify = selfNotify;}

    /** {@inheritDoc} */
    @Override
    public final void onChange(boolean selfChange, @Nullable Uri uri)
    {super.onChange(selfChange, uri); mObserver.accept(selfChange, uri); }

    /** {@inheritDoc} */
    @Override
    public final boolean deliverSelfNotifications()
    {return mSelfNotify;}
  }
}

package data;

import android.annotation.SuppressLint;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.function.BiConsumer;

import static android.content.ContentResolver.SCHEME_CONTENT;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2018
 */
final class LocalSource implements DataSource {

  /** Default projection */
  private static final String[] PROJECTION = { BaseColumns._ID, "data"};

  /** Content uri. */
  private final Uri mContentUri;

  /** Content resolver. */
  private final ContentResolver mResolver;
  /** Content Provider client */
  private final ContentProviderClient mClient;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  LocalSource(@NonNull ContentResolver resolver, @NonNull String authority) {
    mContentUri = new Uri.Builder()
        .scheme(SCHEME_CONTENT)
        .authority(authority)
        .build();

    mClient = (mResolver = resolver)
        .acquireContentProviderClient(mContentUri);
  }

  /** @return base content uri */
  @NonNull @Override
  public Uri getContentUri() {return mContentUri;}

  /** {@inheritDoc} */
  @Override
  public final void close()
  {if (mClosed) return; mClient.close(); mClosed = true;}

  /** {@inheritDoc} */
  @Override
  protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /**
   * @param uri   data resource
   * @param data  input content
   *
   * @return output content
   */
  @Nullable @Override
  public final AssetFileDescriptor transact
  (@NonNull Uri uri, @Nullable AssetFileDescriptor data) {return null;}

  /** {@inheritDoc} */
  @NonNull @Override
  public final Cursor cursor(@NonNull Uri uri)
  {return cursor(uri, null);}

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
    return cursor == null ? new MatrixCursor(PROJECTION) : cursor;
  }

  /** {@inheritDoc} */
  @Override
  public final  void delete(@NonNull Uri uri)
  {try {mClient.delete(uri, null, null);} catch (RemoteException ignored) {}}

  /** {@inheritDoc} */
  @Override
  public final void put(@NonNull Uri uri, @NonNull byte[] raw) {
    final boolean update; long id;
    try {id = ContentUris.parseId(uri);}
    catch (NumberFormatException exception) {id = -1;}
    if (id != -1) {
      final Cursor cursor = cursor(uri, new String[]{ BaseColumns._ID});
      update = cursor.getCount() == 1; cursor.close();
    } else update = false;
    final ContentValues values = new ContentValues();
    if (id != -1) values.put(BaseColumns._ID, id);
    values.put("data", raw);
    try {
      if (!update) mClient.insert(uri, values);
      else mClient.update(uri, values, null, null);
    } catch (RemoteException ignored) {}

  }

  /** {@inheritDoc} */
  @NonNull @Override
  public final ContentObserver register
  (@NonNull Uri uri, @NonNull BiConsumer<Boolean, Uri> observer, @Nullable
      Handler handler) {
    final ContentObserver result = new Observer(observer, handler);
    mResolver.registerContentObserver(uri, true, result);
    try {return result;} finally {/*observer.accept(true, null);*/mResolver.notifyChange(uri, result);}
  }

  /** {@inheritDoc} */
  @Override
  public final void unregister(@NonNull ContentObserver observer)
  {mResolver.unregisterContentObserver(observer);}

  /** Observer record. */
  private static final class Observer extends ContentObserver {

    /** Data mObserver. */
    private final BiConsumer<Boolean, Uri> mObserver;

    /**
     * Creates a content mObserver.
     *
     * @param observer file mObserver
     */
    Observer(@NonNull BiConsumer<Boolean, Uri> observer, @Nullable
        Handler handler)
    {super(handler); this.mObserver = observer;}

    /** {@inheritDoc} */
    @Override
    public final void onChange(boolean selfChange, @Nullable Uri uri)
    {super.onChange(selfChange, uri); mObserver.accept(selfChange, uri); }

    /** {@inheritDoc} */
    @Override
    public final boolean deliverSelfNotifications()
    {return /*super.deliverSelfNotifications()*/true;}
  }
}

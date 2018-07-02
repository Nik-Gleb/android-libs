package data;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.MediaStore.Video;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.function.BooleanSupplier;

import static android.content.ContentUris.withAppendedId;
import static android.os.Environment.DIRECTORY_MOVIES;
import static android.os.Environment.getExternalStoragePublicDirectory;
import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.text.TextUtils.isEmpty;
import static java.util.Objects.requireNonNull;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/06/2018
 */
final class ExternalSource implements DataSource {

  /** Modes. */
  private static final String READ = "r", WRITE = "w";

  /* Mode query pattern */
  //private final Pattern mPattern = Pattern.compile("mode=([^&]*)");

  /** Main uri */
  private final Uri mUri;

  /** Content provider client. */
  private final ContentProviderClient mClient;

  /** Files directory. */
  private final File mDirectory;

  /** Permissions checkers. */
  private final BooleanSupplier mRead, mWrite;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs  a new {@link ExternalSource}.
   *
   * @param resolver  content resolver
   * @param read      read permissions
   * @param write     write permissions
   */
  ExternalSource(
      @NonNull ContentResolver resolver,
      @NonNull BooleanSupplier read,
      @NonNull BooleanSupplier write)
  {this(resolver, Video.Media.EXTERNAL_CONTENT_URI,
        getExternalStoragePublicDirectory(DIRECTORY_MOVIES), read, write);}

  /**
   * Constructs a new {@link ExternalSource}.
   *
   * @param resolver state resolver
   * @param uri main uri address
   * @param directory files directory
   */
  private ExternalSource(
      @NonNull ContentResolver resolver,
      @NonNull Uri uri,
      @NonNull File directory,
      @NonNull BooleanSupplier read,
      @NonNull BooleanSupplier write) {
    mClient = resolver.acquireContentProviderClient(mUri = uri/*.buildUpon().appendPath("voir.mp4").build()*/);
    mDirectory = directory;  mRead = read; mWrite = write;
  }

  /** @return base content uri */
  @NonNull @Override
  public final Uri getContentUri() {return mUri;}

  /** {@inheritDoc} */
  @Override
  public final void close()
  {if (mClosed) return; mClient.close(); mClosed = true;}

  /** {@inheritDoc} */
  @Override
  protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  @Nullable @Override
  public final AssetFileDescriptor transact
      (@NonNull Uri uri, @Nullable AssetFileDescriptor data) {
    if (data != null) throw new IllegalArgumentException();
    final String query = uri.getQueryParameter(MODE);
    final String mode = isEmpty(query) ? READ : query;
    switch (mode) {
      case READ: if (!mRead.getAsBoolean()) return null; break;
      case WRITE: if (!mWrite.getAsBoolean()) return null; break;
      default: throw new IllegalArgumentException();
    } return open("video.mp4", mode);
  }

  /**
   * Open file descriptor.
   *
   * @param file file define
   * @param mode access mode
   *
   * @return parcel file descriptor
   */
  @NonNull private AssetFileDescriptor open
  (@NonNull String file, @NonNull String mode) {
    try {return requireNonNull(mClient.openAssetFile(getFile(file), mode));}
    catch (RemoteException | FileNotFoundException exception)
    {throw new RuntimeException(exception);}
  }

  /**
   * @param file file description
   * @return uri address
   */
  @VisibleForTesting
  @SuppressWarnings("WeakerAccess")
  @NonNull final Uri getFile(@NonNull String file) {
    final String path = new File(mDirectory, file).getAbsolutePath();
    final String[] projection = new String[] { _ID}, args = new String[] {path};
    final String selection = DATA + " = ?", sort = null;
    try (final Cursor rows = mClient.query(mUri, projection, selection, args, sort)) {
      if (!requireNonNull(rows).moveToFirst()) {
        final int size = 1; final ContentValues values;
        values = new ContentValues(size); values.put(DATA, path);
        try {return requireNonNull(mClient.insert(mUri, values));}
        catch (RemoteException exception) {throw new RuntimeException(exception);}
      } else return withAppendedId(mUri, rows.getLong(rows.getColumnIndexOrThrow(_ID)));
    } catch (RemoteException exception) {throw new RuntimeException(exception);}
  }
}

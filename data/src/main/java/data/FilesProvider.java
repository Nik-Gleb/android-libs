/*
 * FilesProvider.java
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.OnCloseListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import static android.content.ClipDescription.compareMimeTypes;
import static android.content.res.AssetFileDescriptor.UNKNOWN_LENGTH;
import static android.os.ParcelFileDescriptor.MODE_CREATE;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;
import static android.os.ParcelFileDescriptor.MODE_TRUNCATE;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;
import static android.os.ParcelFileDescriptor.open;
import static android.provider.OpenableColumns.DISPLAY_NAME;
import static android.provider.OpenableColumns.SIZE;
import static data.Provider.file;
import static data.Provider.getMimeType;
import static data.Provider.isCanceled;
import static data.Provider.notifyUri;
import static java.lang.System.err;
import static java.util.Arrays.copyOf;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
final class FilesProvider implements Provider {

  /** File locks. */
  private final Map<String, StampedLock> mLocks = new ConcurrentHashMap<>();

  /** Callback handler. */
  private final Handler mHandler = new Handler();

  /** Internal content observer */
  private final ContentObserver mObserver = new ContentObserver(mHandler) {};

  /** Content resolver. */
  private final ContentResolver mResolver;

  /** Files directory. */
  private final File mFiles;

  /**
   * Constructs a new assets provider.
   *
   * @param context   app context
   * @param authority content authority
   * @param version   provider version
   */
  @SuppressWarnings("unused")
  FilesProvider(@NonNull Context context, @NonNull String authority, int version)
  {mResolver = context.getContentResolver(); mFiles = context.getFilesDir();}

  /** {@inheritDoc} */
  @NonNull @Override public final Cursor query(@NonNull Uri uri,
      @Nullable String[] proj, @Nullable String sel, @Nullable String[] args,
      @Nullable String sort) {return query(uri, proj, sel, args, sort, null);}

  /** {@inheritDoc} */
  @NonNull @Override public final Cursor query(@NonNull Uri uri,
      @Nullable String[] proj, @Nullable String sel, @Nullable String[] args,
      @Nullable String sort, @Nullable CancellationSignal signal) {

    isCanceled(signal); final File file = file(mFiles, uri);
    proj = proj != null ? proj : new String[] {DISPLAY_NAME, SIZE};

    int i = 0;
    String[] cols = new String[proj.length];
    Object[] values = new Object[proj.length];
    for (final String col : proj)
      if (DISPLAY_NAME.equals(col)) {
      cols[i] = DISPLAY_NAME;
      values[i++] = file.getName();
    } else if (SIZE.equals(col)) {
      cols[i] = SIZE;
      values[i++] = file.length();
    }

    cols = copyOf(cols, i); values = copyOf(values, i);
    final MatrixCursor result = new MatrixCursor(cols, 1);
    result.addRow(values); isCanceled(signal); return result;
  }

  /** {@inheritDoc} */
  @NonNull @Override public final String getType(@NonNull Uri uri)
  {return getMimeType(file(mFiles, uri).getPath());}

  /** {@inheritDoc} */
  @Nullable @Override public String[] getStreamTypes
  (@NonNull Uri uri, @NonNull String filter) {
    File file = file(mFiles, uri);
    file = file.isDirectory() ? file : file.getParentFile();
    final Set<String> set = Arrays.stream(file.list())
        .map(Provider::getMimeType)
        .filter(type -> compareMimeTypes(type, filter))
        .collect(Collectors.toSet());
    return set.size() != 0 ? set.toArray(new String[set.size()]) : null;
  }

  /** {@inheritDoc} */
  @NonNull @Override public final ParcelFileDescriptor
  openFile(@NonNull Uri uri, @NonNull String mode)
  throws FileNotFoundException {return openFile(uri, mode, null);}

  /** {@inheritDoc} */
  @NonNull @Override public final ParcelFileDescriptor openFile(@NonNull Uri uri,
  @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException
  {return openFile(uri, mode, mFiles, mLocks, mResolver, mObserver, mHandler, new Bundle(), signal);}

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor
  openAssetFile(@NonNull Uri uri, @NonNull String mode)
  throws FileNotFoundException {return openAssetFile(uri, mode, null);}

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor openAssetFile(@NonNull Uri uri,
  @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
    final Bundle extras = new Bundle(); final ParcelFileDescriptor result =
    openFile(uri, mode, mFiles, mLocks, mResolver, mObserver, mHandler, extras, signal);
    return new AssetFileDescriptor(result, 0, UNKNOWN_LENGTH, extras);
  }

  /** {@inheritDoc} */
  @NonNull @Override public final AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts)
      throws FileNotFoundException {return openTypedAssetFile(uri, filter, opts, null);}

  /** {@inheritDoc} */
  @NonNull @Override public AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts,
      @Nullable CancellationSignal signal) throws FileNotFoundException {
    if ("*/*".equals(filter) || compareMimeTypes(getType(uri), filter))
      return openAssetFile(uri, READ_MODE, signal);
    else throw new FileNotFoundException("Can't open " + uri + " as type " + filter);
  }

  /**
   *
   * @param uri       uri of resource
   * @param mode      access mode ("w" or "r")
   * @param files     root files-directory
   * @param locks     read/write locks map
   * @param resolver  content resolver
   * @param observer  content observer
   * @param handler   callbacks handler
   * @param bundle    extras bundle
   * @param signal    cancellation signal
   *
   * @return readable or writable parcel file descriptor
   *
   * @throws FileNotFoundException when file not exists
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @NonNull private static ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode, @NonNull File files,
      @NonNull Map<String, StampedLock> locks, @NonNull ContentResolver resolver,
      @NonNull ContentObserver observer, @NonNull Handler handler, @NonNull Bundle bundle,
      @Nullable CancellationSignal signal) throws FileNotFoundException {
    if (signal != null && signal.isCanceled()) throw new OperationCanceledException();
    final File file = file(files, uri); final String path; final boolean write = mode.contains("w");
    final int flags = write ? MODE_WRITE_ONLY | MODE_CREATE | MODE_TRUNCATE : MODE_READ_ONLY;
    final StampedLock lock = locks.computeIfAbsent(path = file.getPath(), key -> new StampedLock());
    final long stamp = write ? lock.writeLock() : lock.readLock();
    final OnCloseListener listener = exception -> {
      if (write) {
        if (exception == null) notifyUri(resolver, uri, observer);
        else {if (!file.delete()) err.println("Can't delete " + path);}
        lock.unlockWrite(stamp);} else lock.unlockRead(stamp);
    }; bundle.putString(OkUtils.TYPE, getMimeType(path));
    try {isCanceled(signal); return open(file, flags, handler, listener);}
    catch (IOException exception) { listener.onClose(exception);
      if (!FileNotFoundException.class.isAssignableFrom(exception.getClass()))
        throw new OperationCanceledException(exception.getMessage());
      else throw (FileNotFoundException) exception;
    } catch (OperationCanceledException exception)
    {listener.onClose(new IOException()); throw exception;}
  }

}

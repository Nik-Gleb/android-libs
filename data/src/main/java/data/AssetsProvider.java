/*
 * AssetsProvider.java
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
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.BufferedSource;

import static android.content.ClipDescription.compareMimeTypes;
import static android.provider.OpenableColumns.DISPLAY_NAME;
import static android.provider.OpenableColumns.SIZE;
import static data.OkUtils.TYPE;
import static data.OkUtils.fromResponse;
import static data.Provider.getMimeType;
import static data.Provider.isCanceled;
import static java.util.Arrays.copyOf;
import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/06/2018
 */
final class AssetsProvider implements Provider {

  /** Asset authority. */
  private static final String ASSETS_AUTHORITY = "/android_asset";

  /** Assets manager. */
  private final AssetManager mAssets;

  /**
   * Constructs a new assets provider.
   *
   * @param context   app context
   * @param authority content authority
   * @param version   provider version
   */
  @SuppressWarnings("unused") AssetsProvider
  (@NonNull Context context, @NonNull String authority, int version) {
    mAssets = context.getAssets();
  }

  /** Asset File Request. */
  private static final class AssetRequest extends ResponseBody {

    /** Buffered source. */
    private final BufferedSource mSource;

    /** Media type. */
    private final MediaType mType;

    /** Size of content. */
    private final long mLength;

    /**
     * Constructs a new {@link AssetRequest}.
     *
     * @param stream input stream
     * @param type   media type
     */
    AssetRequest(@NonNull InputStream stream, @NonNull String type)
      throws IOException {
      mSource = OkUtils.source(stream);
      mType = MediaType.parse(type);
      mLength = stream.available();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public final MediaType contentType() {return mType;}

    /** {@inheritDoc} */
    @Override
    public final long contentLength() {return mLength;}

    /** {@inheritDoc} */
    @Override
    public final BufferedSource source() {return mSource;}
  }  /**
   * @param uri content uri
   *
   * @return assets uri
   */
  @NonNull
  private static Uri convert(@NonNull Uri uri) {
    return uri.buildUpon()
      .scheme(ContentResolver.SCHEME_FILE)
      .encodedAuthority(ASSETS_AUTHORITY)
      .build();
  }

  /**
   * @param uri resource uri
   *
   * @return full path with name
   */
  @NonNull
  private static String full(@NonNull Uri uri) {
    return uri.getPath()
      .substring(1);
  }

  /**
   * @param uri resource uri
   *
   * @return path without name
   */
  @NonNull
  private static String path(@NonNull Uri uri) {
    final String[] split = uri.getPath().substring(1).split("/");
    return split.length == 1 ? "" : split[0];
  }

  /**
   * @param uri resource uri
   *
   * @return the name of file
   */
  @NonNull
  private static String name(@NonNull Uri uri) {
    return uri.getLastPathSegment();
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final Cursor query
  (@NonNull Uri uri, @Nullable String[] proj, @Nullable String sel,
    @Nullable String[] args, @Nullable String sort) {
    return query(uri, proj, sel, args, sort, null);
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final Cursor query
  (@NonNull Uri uri, @Nullable String[] proj, @Nullable String sel,
    @Nullable String[] args, @Nullable String sort,
    @Nullable CancellationSignal signal) {

    isCanceled(signal);
    uri = convert(uri);
    proj = proj != null ? proj : new String[] { DISPLAY_NAME, SIZE };

    int i = 0;
    String[] cols = new String[proj.length];
    Object[] values = new Object[proj.length];
    for (final String col : proj) {
      if (DISPLAY_NAME.equals(col)) {
        cols[i] = DISPLAY_NAME;
        values[i++] = name(uri);
      } else if (SIZE.equals(col)) {
        cols[i] = SIZE;
        values[i++] = -1;
      }
    }

    cols = copyOf(cols, i);
    values = copyOf(values, i);

    final MatrixCursor result = new MatrixCursor(cols, 1);
    result.addRow(values);
    isCanceled(signal);

    return result;
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final String getType(@NonNull Uri uri) {
    return getMimeType(full(convert(uri)));
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public String[] getStreamTypes
  (@NonNull Uri uri, @NonNull String filter) {
    final String path = path(convert(uri));
    String[] files;
    try {
      files = mAssets.list(path);
    } catch (IOException exception) {
      files = null;
    }

    if (files == null || files.length == 0) return null;

    final Set<String> set =
      Arrays.stream(files)
        .map(Provider::getMimeType)
        .filter(type -> compareMimeTypes(type, filter))
        .collect(Collectors.toSet());

    return set.size() != 0 ? set.toArray(new String[set.size()]) : null;
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final ParcelFileDescriptor
  openFile(@NonNull Uri uri, @NonNull String mode)
    throws FileNotFoundException {return openFile(uri, mode, null);}

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal)
    throws FileNotFoundException {
    return openAssetFile(uri, mode, signal).getParcelFileDescriptor();
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    return openAssetFile(uri, mode, null);
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal)
    throws FileNotFoundException {return openFile(uri, mAssets, signal);}

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts)
    throws FileNotFoundException {
    return openTypedAssetFile(uri, filter, opts, null);
  }

  /** {@inheritDoc} */
  @NonNull
  @Override
  public final AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts,
    @Nullable CancellationSignal signal) throws FileNotFoundException {
    if ("*/*".equals(filter) || compareMimeTypes(getType(uri), filter))
      return openAssetFile(uri, READ_MODE, signal);
    else
      throw new FileNotFoundException
        ("Can't open " + uri + " as type " + filter);

  }

  /**
   * @param uri    uri resource
   * @param assets assets manager
   *
   * @return asset file descriptor
   */
  @NonNull
  private static AssetFileDescriptor openFile
  (@NonNull Uri uri, @NonNull AssetManager assets,
    @Nullable CancellationSignal signal) throws FileNotFoundException {
    final String path = full(convert(uri)), type = getMimeType(path);
    final Bundle extras = new Bundle();
    extras.putString(TYPE, type);
    try {
      isCanceled(signal);
      final AssetFileDescriptor afd = assets.openFd(path);
      return new AssetFileDescriptor(
        afd.getParcelFileDescriptor(),
        afd.getStartOffset(),
        afd.getDeclaredLength(),
        extras
      );
    } catch (IOException exception) {
      try {
        final ResponseBody response = new AssetRequest(assets.open(path), type);
        return fromResponse(response, commonPool());
      } catch (IOException e) {
        if (!FileNotFoundException.class.isAssignableFrom(e.getClass()))
          throw new OperationCanceledException(e.getMessage());
        else throw (FileNotFoundException) e;
      }
    }
  }


}

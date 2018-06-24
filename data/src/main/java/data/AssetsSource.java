package data;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 18/06/2018
 */
final class AssetsSource implements DataSource {

  /** Assets Manager */
  private final AssetManager mAssets;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link AssetsSource}.
   *
   * @param assets assets manager
   */
  AssetsSource(@NonNull AssetManager assets)
  {mAssets = assets;}

  /** @return base content uri */
  @NonNull @Override
  public final Uri getContentUri()
  {return new Uri.Builder().scheme(ContentResolver.SCHEME_FILE)
      .encodedAuthority("/android_asset").build();}

  /** {@inheritDoc} */
  @Override
  public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override
  protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Nullable @Override
  public final AssetFileDescriptor transact
      (@NonNull Uri uri, @Nullable AssetFileDescriptor data) {
    if (data != null) throw new IllegalArgumentException();
    try {return mAssets.openFd(uri.getPath().substring(1));}
    catch (IOException exception) {return null;}
  }
}

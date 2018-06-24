package widgets.collections;

import android.support.v7.widget.LinearSnapHelper;

import java.io.Closeable;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Collection Snap Helper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 29/04/2018
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep@KeepPublicProtectedClassMembers
public final class CollectionSnapHelper
    extends LinearSnapHelper implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "CollectionSnapHelper";


  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}
}

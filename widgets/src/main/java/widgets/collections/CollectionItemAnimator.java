package widgets.collections;

import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

import java.io.Closeable;
import java.util.List;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Collection Item Animator.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 29/04/2018
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
public final class CollectionItemAnimator
    extends DefaultItemAnimator
    implements Closeable {

  /** The log-cat tag. */
  private static final String TAG = "CollectionItemAnimator";

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  public CollectionItemAnimator(boolean changes) {
    setSupportsChangeAnimations(changes);
    /*setAddDuration(getAddDuration() * 2);*/
    //setChangeDuration(getChangeDuration() * 2);
    /*setRemoveDuration(getRemoveDuration() * 2);*/
    //setMoveDuration(getMoveDuration() * 2);
  }

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  @Override
  public boolean canReuseUpdatedViewHolder(
      @NonNull RecyclerView.ViewHolder viewHolder,
      @NonNull List<Object> payloads) {
    return !getSupportsChangeAnimations() && super.canReuseUpdatedViewHolder(viewHolder, payloads);
  }

  @Override
  public boolean canReuseUpdatedViewHolder(
      @NonNull RecyclerView.ViewHolder viewHolder) {
    return !getSupportsChangeAnimations() && super.canReuseUpdatedViewHolder(viewHolder);
  }
}

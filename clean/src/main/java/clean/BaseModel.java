package clean;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Objects;

/**
 * Base java-model.
 *
 * @param <V> the type of view
 *
 * @author Nikitenko Gleb
 * @since 1.0, 27/02/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public abstract class BaseModel<V> implements Closeable {

  /** Threader builder. */
  private final Threader.Builder mBuilder;

  /** The View. */
  protected V view = null;

  /** The threader. */
  private Threader mThreader = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link BaseModel}.
   *
   * @param builder the builder
   */
  protected BaseModel(Threader.Builder builder)
  {mBuilder = builder;}

  /** @param view the view instance for attach/detach */
  public final void setView(V view) {
    if (this.view == view) return;
    if (view != null && mThreader == null)
    {this.view = view; mThreader = mBuilder.build();}
    else if (view == null && mThreader != null)
    {mThreader.close(); mThreader = null; this.view = null;}
    else this.view = view;
  }

  /** @return all active actions */
  public final HashMap<Integer, Object> state() {
    Objects.requireNonNull(mThreader).backup();
    return mBuilder.savedState;
  }

  /**
   * Apply the action.
   *
   * @param id action id
   */
  protected final void apply(int id)
  {mThreader.apply(id, Void.TYPE);}

  /**
   * Apply the action.
   *
   * @param id action id
   * @param args action args
   *
   * @param <U> the type of args
   */
  @SuppressWarnings("WeakerAccess")
  protected final <U> void apply(int id, U args)
  {mThreader.apply(new AsyncTask(id, args));}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; onClose(); mClosed = true;}

  /** Causes by close */
  @SuppressWarnings("WeakerAccess")
  protected void onClose()
  {if (mThreader != null) {mThreader.close(); mThreader = null;}}


}

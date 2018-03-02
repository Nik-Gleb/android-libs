package app;

import android.os.Bundle;

import java.io.Closeable;

/**
 * Base Android Repository.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 02/03/2018
 */
@SuppressWarnings("unused")
public abstract class Repository implements Closeable {

  /** STATE KEYS. */
  private static final String
      STATE_REPOSITORY = "repository";

  /** This instance. */
  @SuppressWarnings("WeakerAccess")
  protected final Repository instance = this;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * @param inState saved state instance
   *
   * @param <T> type of model
   *
   * @return model instance or null
   */
  public static <T extends Repository> T get(Bundle inState)
  {return inState != null ? Retain.get(inState, STATE_REPOSITORY) : null;}

  /** @param outState saved state container */
  public final void save(Bundle outState)
  {Retain.put(outState, STATE_REPOSITORY, instance);}

  /** @param isFinishing true by exit */
  public final void release(boolean isFinishing)
  {if (isFinishing) close();}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; onClosed();  mClosed = true;}

  /** Calls by exit */
  @SuppressWarnings("WeakerAccess")
  protected abstract void onClosed();
}

package extensions;

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 25/06/2018
 */
@SuppressWarnings("unused")
@Singleton @dagger.Component(modules = {Module.class})
public abstract class Component implements Closeable {

  /** Closed state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; try {module().close();}
  catch (IOException ignored){} finally {mClosed = true;}}

  /** @return closeable dependency. */
  @SuppressWarnings({ "NullableProblems", "WeakerAccess" })
  @NonNull @Named("module") protected abstract Closeable module();
}

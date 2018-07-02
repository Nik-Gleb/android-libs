package extensions;

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.Stack;

import javax.inject.Named;

import dagger.Provides;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 25/06/2018
 */
@SuppressWarnings("unused")
@dagger.Module public class Module implements Closeable {

  /** Closeable dependencies. */
  @NonNull private final Stack<Closeable> mCloseables = new Stack<>();

  /** Closed state. */
  private volatile boolean mClosed;

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return; mClosed = true;
    while (!mCloseables.empty()) {
      try {mCloseables.pop().close();}
      catch (IOException ignored) {}
    }
  }

  /** @return closeable dependencies. */
  @Provides @Named("stack") @NonNull protected final
  Stack<Closeable> closeables() {return mCloseables;}

  /** @return module */
  @Provides @Named("module") @NonNull protected final
  Closeable module() {return this;}
}

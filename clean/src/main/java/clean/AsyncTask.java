package clean;

import java.io.Closeable;
import java.util.Objects;

/**
 * Asynchronous Task.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 25/02/2018
 */
final class AsyncTask implements Runnable, Closeable {

  /** AsyncTask ID. */
  private final int mId;

  /** I/O fields.. */
  @SuppressWarnings("WeakerAccess")
  volatile Object input, output;

  /** Worker thread. */
  volatile Thread thread = null;

  /** Worker function. */
  volatile Function function = null;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link AsyncTask}.
   *
   * @param id    TASK ID
   * @param data  TASK ARGS
   */
  AsyncTask(int id, Object data) {mId = id; input = data;}

  /** {@inheritDoc} */
  @SuppressWarnings("WeakerAccess")
  public final boolean apply(AsyncTask task) {
    final Object input = task.input;
    if (same(input)) return false;
    this.input = input; final Thread t;
    if ((t = this.thread) != null)
      t.interrupt();
    return true;
  }

  /** {@inheritDoc} */
  @Override public final void run() {
    final Thread t = thread;
    final Function f = function;
    if (t == null || f == null) return;
    Object input, output; boolean exit;
    do {
      input = this.input; exit = true;
      try {
        this.output = (output = f.apply(input)) == null ? Void.TYPE : output;}
      catch (Throwable throwable) {
        if (throwable instanceof InterruptedException &&
            (exit = same(input))) t.interrupt();
        if (exit) this.output = throwable;
      }
    } while (!exit);
  }

  /**
   * @param input new input
   * @return true if this new input equals already existing
   */
  private boolean same(Object input)
  {return Objects.equals(this.input, input);}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; output = input = null; mClosed = true;}

  /** {@inheritDoc} */
  @Override public final boolean equals(Object object) {
    return this == object || object instanceof AsyncTask ?
        mId == ((AsyncTask) object).mId :
        object instanceof Integer && mId == (Integer) object;
  }

  /** {@inheritDoc} */
  @Override public final int hashCode() {return mId;}

  /** {@inheritDoc} */
  @Override public final String toString() {
    final StringBuilder builder =
        new StringBuilder(getClass().getSimpleName())
            .append("{")
              .append("mId=")       .append(mId)
              .append(", input=")  .append(input)
              .append(", output=") .append(output)
            .append('}');
    try {return builder.toString();}
    finally {builder.setLength(0);}
  }

}

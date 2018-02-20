package clean;

import java.io.Closeable;

import clean.cancellation.CancellationSignal;
import clean.cancellation.CancellationSignal.OperationCanceledException;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 20/02/2018
 */
@SuppressWarnings("unused")
public interface Manager extends Closeable {

  /**
   * @param args the method arguments
   * @param signal the cancellation signal
   *
   * @throws OperationCanceledException when action was cancelled
   */
  void apply(Action args, CancellationSignal signal)
      throws OperationCanceledException;

  /** {@inheritDoc} */
  @Override void close();

  /** Action. */
  class Action<T> {

    /** ID of action. */
    final int id;

    /** Arguments of action. */
    final T args;

    /**
     * Constructs a new {@link Action}.
     *
     * @param id ID of action
     * @param args arguments of action
     */
    private Action(int id, T args)
    {this.id = id; this.args = args;}

    /**
     * Constructs a new {@link Action}.
     *
     * @param id ID of action
     * @param args arguments of action
     */
    public static <T> Action<T> create(int id, T args)
    {return new Action<T>(id, args) {};}

    /** {@inheritDoc} */
    @Override public final String toString() {
      final StringBuilder builder = new StringBuilder("Action{").
          append("id=").append(id).append(", args=").append(args).append('}');
      try {return builder.toString();} finally {builder.setLength(0);}
    }
  }
}

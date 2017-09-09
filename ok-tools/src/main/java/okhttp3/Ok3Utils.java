package okhttp3;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 10/09/2017
 */
@SuppressWarnings("unused")
public final class Ok3Utils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Ok3Utils() {throw new AssertionError();}

  /**
   * Cancel the http-call.
   *
   * @param call the ok-http call
   */
  public static void cancel(Call call) {
    if (call != null) call.cancel();
  }
}

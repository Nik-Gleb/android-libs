package async;

/**
 * Common Utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 21/02/2018
 */
public final class Utils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private Utils() {throw new AssertionError();}

}

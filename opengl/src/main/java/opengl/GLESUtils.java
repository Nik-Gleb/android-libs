package opengl;

import android.opengl.GLES20;
import android.support.annotation.NonNull;
import android.util.Log;

import static opengl.Logger.LS;

/**
 * Common GLES-Specific utils.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/12/2017
 */
@SuppressWarnings("WeakerAccess")
public final class GLESUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private GLESUtils() {throw new AssertionError();}

  /**
   * GLES for information, then formats it all into one giant str.
   *
   * @param builder the {@link StringBuilder} instance
   */
  static void info(@NonNull StringBuilder builder) {
    builder
        .append(LS).append("===== GL Information =====")
        .append(LS).append("vendor    : ")
        .append(GLES20.glGetString(GLES20.GL_VENDOR))
        .append(LS).append("version   : ")
        .append(GLES20.glGetString(GLES20.GL_VERSION))
        .append(LS).append("renderer  : ")
        .append(GLES20.glGetString(GLES20.GL_RENDERER))
        .append(LS).append("extensions:").append(LS)
        .append(OpenGL.format(GLES20.glGetString(GLES20.GL_EXTENSIONS)));
  }

  static void checkError() {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
      Log.e("ERROR", String.valueOf(error));

  }
}

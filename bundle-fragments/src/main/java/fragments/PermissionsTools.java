package fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashSet;
import java.util.Set;

import static android.os.Process.myPid;
import static android.os.Process.myUid;

/**
 * Permissions Tools.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 06/05/2018
 */
@SuppressWarnings("unused")
public final class PermissionsTools {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private PermissionsTools()
  {throw new AssertionError();}

  /** @return new created empty "NEVER ASK" set */
  @NonNull
  public static
  Set<String> createNeverAskSet() {
    return new HashSet<String>() {
      /** {@inheritDoc} */
      @Override public final boolean
      add(@NonNull String string)
      {super.add(string); return true;}
      /** {@inheritDoc} */
      @Override public final boolean
      remove(@Nullable Object object)
      {super.remove(object); return true;}
    };
  }

  /**
   * @param context     application context
   * @param permission  target permission
   * @param neverAsk    never ask set
   *
   * @return            permission status
   */
  @Status public static int permission(@NonNull Context context,
      @NonNull String permission, @NonNull Set<String> neverAsk)
  {return permission(context, permission, neverAsk, Status.NEVER_ASK);}

  /**
   * @param context application context
   * @param permission target permission
   *
   * @return activity result
   */
  @Status private static int permission
  (@NonNull Context context, @NonNull String permission,
      @NonNull Set<String> neverAsk, int mode) {
    return !isGranted(context, neverAsk, permission, mode) ?
        isNeverAsk(neverAsk, permission, mode) ? Status.NEVER_ASK :
            mode <= Status.GRANTED ? Status.DENIED :
                permission(context, permission, neverAsk,
                    ProxyActivity.permission(context, permission)) :
        Status.GRANTED;
  }

  /**
   * @param context     application context
   * @param neverAsk    "neverAsk" set
   * @param permission  target permission
   * @param result      previous result
   *
   * @return            true - for granted, otherwise false
   */
  private static boolean isGranted(@NonNull Context context,
      @NonNull Set<String> neverAsk, @NonNull String permission, int result) {
    if (result > Status.GRANTED) result = context.checkPermission(permission, myPid(), myUid());
    return result == PackageManager.PERMISSION_GRANTED && neverAsk.remove(permission);
  }

  /**
   * @param neverAsk    "neverAsk" set
   * @param permission  target permission
   * @param result      previous result
   *
   * @return            true - for never ask, otherwise false
   */
  private static boolean isNeverAsk
  (@NonNull Set<String> neverAsk, @NonNull String permission, int result) {
    return result > Status.GRANTED ? neverAsk.contains(permission) :
        result < Status.DENIED && neverAsk.add(permission);//No should
  }

  @IntDef({ Status.DENIED, Status.GRANTED, Status.NEVER_ASK})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Status {
    /** Permission status constants. */
    int
        DENIED = -1,    // Permission denied
        GRANTED = 0,    // Permission granted
        NEVER_ASK = 1;  // Permission denied and can't be requested
  }
}

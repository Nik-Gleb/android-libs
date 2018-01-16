package opengl;

import android.Manifest;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import org.junit.ClassRule;

import java.io.Closeable;
import java.util.Objects;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 16/12/2017
 */
final class WindowsAccess implements Closeable {

  /** Grant overlay permission for window-system access. */
  @ClassRule
  public static final GrantPermissionRule PERMISSION_SYSTEM_ALERT_WINDOW =
      GrantPermissionRule.grant(Manifest.permission.SYSTEM_ALERT_WINDOW);

  /** "MATCH-PARENT" mode. */
  private static final LayoutParams LAYOUT_MATCH_PARENT =
      new LayoutParams(LayoutParams.TYPE_APPLICATION_OVERLAY);

  /** "WRAP-CONTENT" mode. */
  private static final LayoutParams LAYOUT_WRAP_CONTENT =
      new LayoutParams(LayoutParams.TYPE_APPLICATION_OVERLAY)
      {{width = WRAP_CONTENT; height = WRAP_CONTENT;}};

  /** "ZERO-SIZE" mode. */
  private static final LayoutParams LAYOUT_ZERO_SIZE =
      new LayoutParams(LayoutParams.TYPE_APPLICATION_OVERLAY)
      {{width = 0; height = 0;}};


  /** Main thread handler. */
  private final Handler mMainThread = new Handler(Looper.getMainLooper());

  /** The window manager instance. */
  private final WindowManager mWindowManager = getWindowManager();

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a new {@link WindowsAccess}.
   */
  WindowsAccess() {
  }

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    mMainThread.removeCallbacksAndMessages(null);
    mClosed = true;
  }

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}


  final void showAsMatchParent(@NonNull ViewFactory viewFactory) {
    mMainThread.post(() -> mWindowManager.addView
        (viewFactory.getView(), LAYOUT_MATCH_PARENT));
  }

  final void showAsWrapContent(@NonNull ViewFactory viewFactory) {
    mMainThread.post(() -> mWindowManager.addView
        (viewFactory.getView(), LAYOUT_WRAP_CONTENT));
  }

  final void showAsZeroSize(@NonNull ViewFactory viewFactory) {
    mMainThread.post(() -> mWindowManager.addView
        (viewFactory.getView(), LAYOUT_ZERO_SIZE));
  }

  final void hide(@NonNull View view) {
    mMainThread.post(() -> mWindowManager.removeView(view));
  }

  /** @return platform window manager */
  @NonNull private static WindowManager getWindowManager() {
    final Context context = InstrumentationRegistry.getContext();
    final String name = Context.WINDOW_SERVICE;
    final Object service = context.getSystemService(name);
    final WindowManager result = (WindowManager) service;
    return Objects.requireNonNull(result);
  }

  /** View's provider. */
  interface ViewFactory {
    /** Provide the view instance */
    @NonNull View getView();
  }
}

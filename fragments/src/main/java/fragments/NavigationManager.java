/*
 * NavigationManager.java
 * fragments
 *
 * Copyright (C) 2018, Gleb Nikitenko. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fragments;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ExtendedDialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import java.io.Closeable;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

/**
 * Base Navigation Manager.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 02/03/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
@Keep
@KeepPublicProtectedClassMembers
public abstract class NavigationManager implements Closeable {

  /** The name of back stack. */
  protected static final String BACK_STACK_NAME = "stack";

  /** This instance */
  private final NavigationManager mInstance = this;

  /** Back Stack Changed Listener. */
  private OnStackChanged mOnStackChanged = new OnStackChanged(mInstance);

  /** The default intent. */
  @Nullable private final Intent mDefaultIntent = getDefaultIntent();

  /** The launch intent. */
  @Nullable private Intent mIntent = null;

  /** The host activity. */
  private final Activity mActivity;

  /** Fragment manager. */
  @SuppressWarnings("WeakerAccess")
  @NonNull protected final FragmentManager fragments;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a {@link NavigationManager}
   *
   * @param fragments fragment manager
   */
  protected NavigationManager(@NonNull FragmentManager fragments) {
    if((mActivity = ExtendedDialogFragment.getActivity
        (this.fragments = fragments)) == null)
      throw new NullPointerException("Activity is null!");
    setIntent(mActivity.getIntent());
    mOnStackChanged.onBackStackChanged();
    fragments.addOnBackStackChangedListener(mOnStackChanged);
  }

  /** The launch intent by default */
  @Nullable protected Intent getDefaultIntent() {return null;}

  /** Close the stack. */
  protected final void closeStack(boolean immediate) {
    if (immediate)
      fragments.popBackStackImmediate
          (BACK_STACK_NAME, POP_BACK_STACK_INCLUSIVE);
    else
      fragments.popBackStack
          (BACK_STACK_NAME, POP_BACK_STACK_INCLUSIVE);
  }

  /** Exit from application. */
  protected final void exit(boolean affinity)
  {if (affinity) mActivity.finishAffinity(); else mActivity.finish();}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final void close() {
    if (mClosed) return;
    fragments.removeOnBackStackChangedListener(mOnStackChanged);
    mClosed = true;
  }

  /** @param intent incoming intent for remember */
  public final void onNewIntent(@NonNull Intent intent) {setIntent(intent);}

  /** @param intent incoming intent for remember */
  private void setIntent(@NonNull Intent intent) {mIntent = convert(intent);}

  /**
   * @param intent the incoming intent
   * @return the result intent
   */
  @Nullable private Intent convert(@NonNull Intent intent)
  {return !isDefaultIntent(intent) ? intent : null;}

  /**
   * @param intent incoming intent
   * @return true when this is default
   */
  private boolean isDefaultIntent(@NonNull Intent intent)
  {return mDefaultIntent == null || mDefaultIntent.filterEquals(intent) ||
      (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;}

  /** Resume the router. */
  public final void onResume()
  {final Intent intent; if ((intent = getIntent()) != null) go(intent);}

  /** @return launch intent */
  @Nullable private Intent getIntent() {
    final boolean immediate = false;
    if (mIntent != null) closeStack(immediate);
    try {return mIntent;} finally {mIntent = null;}
  }

  /** @param intent task to moving */
  protected abstract void go(@NonNull Intent intent);

  /**
   * Back stack changed callback.
   *
   * @param top the top back stack entry
   */
  protected void onBackStackChanged(@Nullable FragmentManager.BackStackEntry top) {
      if (mActivity instanceof AppCompatActivity) {
        final ActionBar actionBar = ((AppCompatActivity) mActivity).getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(top != null);
      }
  }

  /**
   * Render current back stack state.
   *
   * @param titleShort the title resource id
   * @param titleLong the subtitle resource id
   */
  protected final void renderStackState
  (@StringRes int titleShort, @StringRes int titleLong) {
    if (mActivity instanceof View) {
      final View view = (View) mActivity;
      view.onBackStackChanged(titleShort, titleLong);
    }
  }


  /** The back stack changed lister. */
  private static final class OnStackChanged implements
      FragmentManager.OnBackStackChangedListener {

    /** The router. */
    @NonNull private final NavigationManager mRouter;

    /**
     * Constructs a new {@link OnStackChanged}
     *
     * @param router the router instance
     */
    OnStackChanged(@NonNull NavigationManager router)
    {mRouter =  router;}

    /** Called whenever the contents of the back stack change. */
    @Override public final void onBackStackChanged() {
      final FragmentManager manager = mRouter.fragments;
      final int index = manager.getBackStackEntryCount() - 1;
      final FragmentManager.BackStackEntry entry = index == -1 ?
          null : manager.getBackStackEntryAt(index);
      mRouter.onBackStackChanged(entry);
    }
  }

  /** The router view. */
  public interface View {

    /**
     * Back stack changed.
     *
     * @param titleShort the title resource id
     * @param titleLong the subtitle resource id
     */
    void onBackStackChanged(@StringRes int titleShort, @StringRes int titleLong);
  }

}

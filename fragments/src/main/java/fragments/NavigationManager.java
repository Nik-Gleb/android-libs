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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.ExtendedFragmentTransaction;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import java.io.Closeable;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;
import static android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE;
import static android.support.v4.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
import static java.util.Objects.requireNonNull;

/**
 * Base Navigation Manager.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 02/03/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
@Keep
@KeepPublicProtectedClassMembers
public class NavigationManager implements Closeable {

  /** Root back stack. */
  private static final String ROOT_STACK = "ROOT";

  /** Common screens name. */
  private static final String MAIN = "MAIN", INTRO = "INTRO";

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

  /** Screens package. */
  private final String mScreensPackage;

  /** Stack name */
  private final String mStack;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;

  /**
   * Constructs a {@link NavigationManager}
   *
   * @param fragments fragment manager
   */
  private NavigationManager
  (@NonNull FragmentActivity activity, @NonNull FragmentManager fragments,
      @NonNull String screens, @NonNull String name) {
    mScreensPackage = screens; mStack = name; this.fragments = fragments;
    mActivity = activity; setIntent(mActivity.getIntent());
    mOnStackChanged.onBackStackChanged();
    fragments.addOnBackStackChangedListener(mOnStackChanged);
  }

  /**
   * Constructs a new {@link NavigationManager}
   *
   * @param fragment parent fragment
   */
  protected NavigationManager(@NonNull ExtendedFragment fragment) {
    this(requireNonNull(fragment.getActivity()), fragment.getChildFragmentManager(),
        fragment.getClass().getPackage().getName(), fragment.getName());
  }

  /**
   * Constructs a new {@link NavigationManager}
   *
   * @param activity parent activity
   */
  protected NavigationManager(@NonNull FragmentActivity activity) {
    this(activity, activity.getSupportFragmentManager(),
        activity.getClass().getPackage().getName(), ROOT_STACK);
  }


    /** The launch intent by default */
  @SuppressWarnings("SameReturnValue")
  @Nullable protected Intent getDefaultIntent() {return null;}

  /** Close the stack. */
  @SuppressWarnings("UnusedReturnValue")
  protected final boolean close(boolean immediate, boolean inclusive) {
    if (immediate)
      return fragments.popBackStackImmediate
          (mStack, inclusive ? POP_BACK_STACK_INCLUSIVE : 0);
    else {
      final boolean popped = fragments.getBackStackEntryCount() > 0;
      fragments.popBackStack(mStack, inclusive ? POP_BACK_STACK_INCLUSIVE : 0);
      return popped;
    }
  }

  /** Exit from application. */
  protected final void exit(boolean affinity)
  {if (affinity) mActivity.finishAffinity(); else mActivity.finish();}

  /** Show "INTRO" Screen. */

  @SuppressWarnings("Convert2Lambda")
  protected final void intro(@Nullable Bundle args) {
    final Boolean rootIsMain = rootIsMain(fragments, MAIN, INTRO);
    if (rootIsMain != null && !rootIsMain) return;
    //final boolean immediate = true, inclusive = true; close(immediate, inclusive);
    final NavigationManager navigation = this;
    final ExtendedFragment fragment = create(INTRO, args);
    new ExtendedFragmentTransaction(fragments)
        .setTransition(TRANSIT_FRAGMENT_FADE)
        .replace(fragment.container, fragment, fragment.getName())
        .runOnCommit(new Runnable() {
          @Override
          public void run() {
            navigation.renderStackState(fragment.title, fragment.title);
          }
        }).commit();
  }

  /** Show "MAIN" Screen. */

  @SuppressWarnings("Convert2Lambda")
  protected final void main(@Nullable Bundle args) {
    final Boolean rootIsMain = rootIsMain(fragments, MAIN, INTRO);
    if (rootIsMain != null && rootIsMain) return;
    //final boolean immediate = true, inclusive = true; close(immediate, inclusive);
    final NavigationManager navigation = this;
    final ExtendedFragment fragment = create(MAIN, args);
    new ExtendedFragmentTransaction(fragments)
        .setPrimaryNavigationFragment(fragment)
        .setTransition(TRANSIT_FRAGMENT_FADE)
        .replace(fragment.container, fragment, fragment.getName())
        .runOnCommit(new Runnable() {
          @Override
          public void run() {
            navigation.renderStackState(fragment.title, fragment.subtitle);
          }
        }).commit();
  }

  /** Show secondary screen */
  protected final void show
  (@NonNull String name, boolean replace, @Nullable Bundle args) {
    final FragmentTransaction result = transaction(name, replace, args);
    if (result != null) result.commit();
  }

  /** Create transaction for secondary screen */
  @Nullable protected final FragmentTransaction transaction
  (@NonNull String name, boolean replace, @Nullable Bundle args) {
    if (fragments.findFragmentByTag(name) != null) return null;
    final FragmentTransaction transaction =
        new ExtendedFragmentTransaction(fragments).setTransition
            (replace ? TRANSIT_FRAGMENT_FADE : TRANSIT_FRAGMENT_OPEN);
    final ExtendedFragment fragment = create(name, args);
    final boolean inflate = fragment.container != 0;
    if (!replace) {
      if (!inflate) transaction.add(fragment, fragment.getName());
      else transaction.replace(fragment.container, fragment, fragment.getName());
      transaction.addToBackStack(mStack);
    } else transaction.replace(fragment.container, fragment, fragment.getName());
    if (fragment.title != 0) transaction.setBreadCrumbShortTitle(fragment.title);
    if (fragment.subtitle != 0) transaction.setBreadCrumbTitle(fragment.subtitle);
    if (replace) transaction.setPrimaryNavigationFragment(fragment);
    return transaction;
  }

  @SuppressWarnings("unchecked")
  @NonNull private <T extends ExtendedFragment> T create
      (@NonNull String tag, @Nullable Bundle args) {
    final String screen = tag.toLowerCase();
    final String name = screen.substring(0,1).toUpperCase() +
        screen.substring(1).toLowerCase() + "Fragment";
    final String path = mScreensPackage + "." + screen + "." + name;
    return (T) Fragment.instantiate(mActivity, path, args);
  }


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

  /** @return launch intent */
  @Nullable public final Intent getIntent() {
    /*final boolean immediate = false;
    if (mIntent != null) close(immediate);*/
    try {return mIntent;} finally {mIntent = null;}
  }

  /** @param intent task to moving */
  @SuppressWarnings("EmptyMethod")
  protected void go(@NonNull Intent intent) {}

  /**
   * Back stack changed callback.
   *
   * @param top the top back stack entry
   */
  private void onBackStackChanged
  (@Nullable FragmentManager.BackStackEntry top) {
    if (mActivity instanceof AppCompatActivity) {
       final ActionBar actionBar =
           ((AppCompatActivity) mActivity)
           .getSupportActionBar();
       if (actionBar != null)
         actionBar.setDisplayHomeAsUpEnabled(top != null);
    }
    if (top != null)
      renderStackState
          (top.getBreadCrumbShortTitleRes(),
              top.getBreadCrumbTitleRes());
    else {
      final Boolean rootIsMain = rootIsMain
          (fragments, MAIN, INTRO);
      if (rootIsMain == null) return;
      final ExtendedFragment fragment =
          (ExtendedFragment) fragments
          .findFragmentByTag
              (rootIsMain ? MAIN : INTRO);
      if (fragment == null) return;
      renderStackState
          (fragment.title, fragment.subtitle);
    }
  }

  /**
   * Render current back stack state.
   *
   * @param titleShort the title resource id
   * @param titleLong the subtitle resource id
   */
  private void renderStackState
  (@StringRes int titleShort, @StringRes int titleLong) {
    if (mActivity instanceof View) {
      final View view = (View) mActivity;
      view.onBackStackChanged(titleShort, titleLong);
    }
  }

  /** Get Root Screen */
  private static Boolean rootIsMain(@NonNull FragmentManager mgr,
      @NonNull String mainScreenName, @NonNull String introScreenName) {
    final boolean main = mgr.findFragmentByTag(mainScreenName) != null;
    final boolean intro = mgr.findFragmentByTag(introScreenName) != null;
    if (main && intro) throw new IllegalStateException();
    else if (!main && !intro) return null; else return main;
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
  @Keep
  @KeepPublicProtectedClassMembers
  public interface View {

    /**
     * Back stack changed.
     *
     * @param titleShort the title resource id
     * @param titleLong the subtitle resource id
     */
    void onBackStackChanged
    (@StringRes int titleShort,
        @StringRes int titleLong);
  }

}

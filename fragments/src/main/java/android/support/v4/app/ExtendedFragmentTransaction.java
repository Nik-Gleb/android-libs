/*
 * ExtendedFragmentTransaction.java
 * fragments
 *
 * Copyright (C) 2017, Gleb Nikitenko. All Rights Reserved.
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

package android.support.v4.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 13/05/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("unused")
public final class ExtendedFragmentTransaction extends FragmentTransaction {

    /** The delegate transaction */
    @NonNull private final FragmentTransaction mFragmentTransaction;

    /**
     * Constructs a new {@link FragmentTransaction}.
     *
     * @param fragmentTransaction the delegate transaction.
     */
    public ExtendedFragmentTransaction(@NonNull FragmentTransaction fragmentTransaction)
    {mFragmentTransaction = fragmentTransaction;}

    /**
     * Constructs a new {@link FragmentTransaction}.
     *
     * @param fragmentManager the delegate transaction.
     */
    @SuppressLint("CommitTransaction")
    public ExtendedFragmentTransaction(@NonNull FragmentManager fragmentManager)
    {mFragmentTransaction = fragmentManager.beginTransaction();}

    /** Calls {@link #add(int, Fragment, String)} with a 0 containerViewId. */
    @Override public final FragmentTransaction add(@NonNull Fragment fragment, @NonNull String tag)
    {mFragmentTransaction.add(fragment, tag); addHub(0, fragment); return this;}

    /** Calls {@link #add(int, Fragment, String)} with a null tag. */
    @Override public final FragmentTransaction add(int containerViewId, @NonNull Fragment fragment)
    {mFragmentTransaction.add(containerViewId, fragment); addHub(0, fragment); return this;}

    /**
     * Add a fragment to the activity state.  This fragment may optionally
     * also have its view (if {@link Fragment#onCreateView Fragment.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param containerViewId Optional identifier of the container this fragment is
     *                        to be placed in.  If 0, it will not be placed in a container.
     * @param fragment        The fragment to be added.  This fragment must not already
     *                        be added to the activity.
     * @param tag             Optional tag name for the fragment, to later retrieve the
     *                        fragment with {@link FragmentManager#findFragmentByTag(String)
     *                        FragmentManager.findFragmentByTag(String)}.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction add
    (int containerViewId, @NonNull Fragment fragment, @Nullable String tag)
    {mFragmentTransaction.add(containerViewId, fragment, tag);
    addHub(containerViewId, fragment); return this;}

    /** Calls {@link #replace(int, Fragment, String)} with a null tag.*/
    @Override public final FragmentTransaction replace(int containerViewId, @NonNull Fragment fragment)
    {mFragmentTransaction.replace(containerViewId, fragment);
    addHub(containerViewId, fragment); return this;}

    /**
     * Replace an existing fragment that was added to a container.  This is
     * essentially the same as calling {@link #remove(Fragment)} for all
     * currently added fragments that were added with the same containerViewId
     * and then {@link #add(int, Fragment, String)} with the same arguments
     * given here.
     *
     * @param containerViewId Identifier of the container whose fragment(s) are
     *                        to be replaced.
     * @param fragment        The new fragment to place in the container.
     * @param tag             Optional tag name for the fragment, to later retrieve the
     *                        fragment with {@link FragmentManager#findFragmentByTag(String)
     *                        FragmentManager.findFragmentByTag(String)}.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction replace
    (int containerViewId, @NonNull Fragment fragment, @Nullable String tag)
    {mFragmentTransaction.replace(containerViewId, fragment, tag);
    addHub(containerViewId, fragment); return this;}

    /**
     * Remove an existing fragment.  If it was added to a container, its view
     * is also removed from that container.
     *
     * @param fragment The fragment to be removed.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction remove(@NonNull Fragment fragment)
    {return mFragmentTransaction.remove(fragment);}

    /**
     * Hides an existing fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be hidden.
     *
     * @param fragment The fragment to be hidden.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction hide(@NonNull Fragment fragment)
    {mFragmentTransaction.hide(fragment); return this;}

    /**
     * Shows a previously hidden fragment.  This is only relevant for fragments whose
     * views have been added to a container, as this will cause the view to
     * be shown.
     *
     * @param fragment The fragment to be shown.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction show(@NonNull Fragment fragment)
    {mFragmentTransaction.show(fragment); return this;}

    /**
     * Detach the given fragment from the UI.  This is the same state as
     * when it is put on the back stack: the fragment is removed from
     * the UI, however its state is still being actively managed by the
     * fragment manager.  When going into this state its view hierarchy
     * is destroyed.
     *
     * @param fragment The fragment to be detached.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction detach(@NonNull Fragment fragment)
    {mFragmentTransaction.detach(fragment); return this;}

    /**
     * Re-attach a fragment after it had previously been detached from
     * the UI with {@link #detach(Fragment)}.  This
     * causes its view hierarchy to be re-created, attached to the UI,
     * and displayed.
     *
     * @param fragment The fragment to be attached.
     * @return Returns the same FragmentTransaction instance.
     */
    @Override public final FragmentTransaction attach(@NonNull Fragment fragment)
    {mFragmentTransaction.attach(fragment);  return this;}

    /**
     * Set a currently active fragment in this FragmentManager as the primary navigation fragment.
     *
     * <p>The primary navigation fragment's
     * {@link Fragment#getChildFragmentManager() child FragmentManager} will be called first
     * to process delegated navigation actions such as {@link FragmentManager#popBackStack()}
     * if no ID or transaction name is provided to pop to. Navigation operations outside of the
     * fragment system may choose to delegate those actions to the primary navigation fragment
     * as returned by {@link FragmentManager#getPrimaryNavigationFragment()}.</p>
     *
     * <p>The fragment provided must currently be added to the FragmentManager to be set as
     * a primary navigation fragment, or previously added as part of this transaction.</p>
     *
     * @param fragment the fragment to set as the primary navigation fragment
     * @return the same FragmentTransaction instance
     */
    @Override
    public FragmentTransaction setPrimaryNavigationFragment(Fragment fragment) {
        return null;
    }

    /**
     * @return <code>true</code> if this transaction contains no operations,
     * <code>false</code> otherwise.
     */
    @Override public final boolean isEmpty()
    {return mFragmentTransaction.isEmpty();}

    /**
     * Set specific animation resources to run for the fragments that are entering and exiting in
     * this transaction.
     *
     * These animations will not be played when popping the back stack.
     */
    @Override public final FragmentTransaction setCustomAnimations(int enter, int exit)
    {mFragmentTransaction.setCustomAnimations(enter, exit);  return this;}

    /**
     * Set specific animation resources to run for the fragments that are entering and exiting in
     * this transaction.
     *
     * The <code>popEnter</code> and <code>popExit</code> animations will be played for enter/exit
     * operations specifically when popping the back stack.
     */
    @Override public final FragmentTransaction setCustomAnimations
    (int enter, int exit, int popEnter, int popExit)
    {mFragmentTransaction.setCustomAnimations(enter, exit, popEnter, popExit);  return this;}

    /**
     * Used with custom Transitions to map a View from a removed or hidden
     * Fragment to a View from a shown or added Fragment.
     * <var>sharedElement</var> must have a unique transitionName in the View hierarchy.
     *
     * @param sharedElement A View in a disappearing Fragment to match with a View in an
     *                      appearing Fragment.
     * @param name          The transitionName for a View in an appearing Fragment to match to the
     *                      shared
     *                      element.
     * @see Fragment#setSharedElementReturnTransition(Object)
     * @see Fragment#setSharedElementEnterTransition(Object)
     */
    @Override public final FragmentTransaction addSharedElement
    (@NonNull View sharedElement, @NonNull String name)
    {mFragmentTransaction.addSharedElement(sharedElement, name);  return this;}

    /**
     * Select a standard transition animation for this transaction.
     *
     * May be one of {@link #TRANSIT_NONE}, {@link #TRANSIT_FRAGMENT_OPEN},
     * {@link #TRANSIT_FRAGMENT_CLOSE}, or {@link #TRANSIT_FRAGMENT_FADE}.
     */
    @Override public final FragmentTransaction setTransition(int transit)
    {mFragmentTransaction.setTransition(transit);  return this;}

    /** Set a custom style resource that will be used for resolving transit animations. */
    @Override public final FragmentTransaction setTransitionStyle(int styleRes)
    {mFragmentTransaction.setTransitionStyle(styleRes);  return this;}

    /**
     * Add this transaction to the back stack.
     *
     * This means that the transaction will be remembered after it is committed, and will reverse
     * its operation when later popped off the stack.
     *
     * @param name An optional name for this back stack state, or null.
     */
    @Override public final FragmentTransaction addToBackStack(@Nullable String name)
    {mFragmentTransaction.addToBackStack(name);  return this;}

    /**
     * Returns true if this FragmentTransaction is allowed to be added to the back stack.
     *
     * If this method would return false, {@link #addToBackStack(String)}
     * will throw {@link IllegalStateException}.
     *
     * @return True if {@link #addToBackStack(String)} is permitted on this transaction.
     */
    @Override public final boolean isAddToBackStackAllowed()
    {return mFragmentTransaction.isAddToBackStackAllowed();}

    /**
     * Disallow calls to {@link #addToBackStack(String)}.
     *
     * Any future calls to addToBackStack will throw {@link IllegalStateException}.
     * If addToBackStack has already been called, this method will throw IllegalStateException.
     */
    @Override public final FragmentTransaction disallowAddToBackStack()
    {mFragmentTransaction.disallowAddToBackStack(); return this;}

    /**
     * Set the full title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    @Override public final FragmentTransaction setBreadCrumbTitle(int res)
    {mFragmentTransaction.setBreadCrumbTitle(res); return this;}

    /**
     * Like {@link #setBreadCrumbTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    @Override public final FragmentTransaction setBreadCrumbTitle(@NonNull CharSequence text)
    {mFragmentTransaction.setBreadCrumbTitle(text); return this;}

    /**
     * Set the short title to show as a bread crumb when this transaction is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    @Override public final FragmentTransaction setBreadCrumbShortTitle(int res)
    {mFragmentTransaction.setBreadCrumbShortTitle(res);  return this;}

    /**
     * Like {@link #setBreadCrumbShortTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    @Override public final FragmentTransaction setBreadCrumbShortTitle(@NonNull CharSequence text)
    {mFragmentTransaction.setBreadCrumbShortTitle(text);  return this;}

    /**
     * Sets whether or not to allow optimizing operations within and across
     * transactions. This will remove redundant operations, eliminating
     * operations that cancel. For example, if two transactions are executed
     * together, one that adds a fragment A and the next replaces it with fragment B,
     * the operations will cancel and only fragment B will be added. That means that
     * fragment A may not go through the creation/destruction lifecycle.
     * <p>
     * The side effect of removing redundant operations is that fragments may have state changes
     * out of the expected order. For example, one transaction adds fragment A,
     * a second adds fragment B, then a third removes fragment A. Without removing the redundant
     * operations, fragment B could expect that while it is being created, fragment A will also
     * exist because fragment A will be removed after fragment B was added.
     * With removing redundant operations, fragment B cannot expect fragment A to exist when
     * it has been created because fragment A's add/remove will be optimized out.
     * <p>
     * It can also reorder the state changes of Fragments to allow for better Transitions.
     * Added Fragments may have {@link Fragment#onCreate(Bundle)} called before replaced
     * Fragments have {@link Fragment#onDestroy()} called.
     * <p>
     * {@link Fragment#postponeEnterTransition()} requires {@code setReorderingAllowed(true)}.
     * <p>
     * The default is {@code false}.
     *
     * @param reorderingAllowed {@code true} to enable optimizing out redundant operations
     *                          or {@code false} to disable optimizing out redundant
     *                          operations on this transaction.
     */
    @Override public final FragmentTransaction setReorderingAllowed(boolean reorderingAllowed)
    {return mFragmentTransaction.setReorderingAllowed(reorderingAllowed);}

    /**
     * Sets whether or not to allow optimizing operations within and across transactions.
     *
     * Optimizing fragment transaction's operations can eliminate
     * operations that cancel. For example, if two transactions are executed
     * together, one that adds a fragment A and the next replaces it with fragment B,
     * the operations will cancel and only fragment B will be added. That means that
     * fragment A may not go through the creation/destruction lifecycle.
     * <p>
     * The side effect of optimization is that fragments may have state changes
     * out of the expected order. For example, one transaction adds fragment A,
     * a second adds fragment B, then a third removes fragment A. Without optimization,
     * fragment B could expect that while it is being created, fragment A will also
     * exist because fragment A will be removed after fragment B was added.
     * With optimization, fragment B cannot expect fragment A to exist when
     * it has been created because fragment A's add/remove will be optimized out.
     * <p>
     * The default is {@code false}.
     *
     * @param allowOptimization {@code true} to enable optimizing operations
     *                          or {@code false} to disable optimizing
     *                          operations on this transaction.
     */
    @SuppressWarnings("deprecation")
    @Override public final FragmentTransaction setAllowOptimization(boolean allowOptimization)
    {mFragmentTransaction.setAllowOptimization(allowOptimization);  return this;}

    /**
     * Add a Runnable to this transaction that will be run after this transaction has
     * been committed. If fragment transactions are {@link #setReorderingAllowed(boolean) optimized}
     * this may be after other subsequent fragment operations have also taken place, or operations
     * in this transaction may have been optimized out due to the presence of a subsequent
     * fragment transaction in the batch.
     *
     * <p>If a transaction is committed using {@link #commitAllowingStateLoss()} this runnable
     * may be executed when the FragmentManager is in a state where new transactions may not
     * be committed without allowing state loss.</p>
     *
     * <p><code>runOnCommit</code> may not be used with transactions
     * {@link #addToBackStack(String) added to the back stack} as Runnables cannot be persisted
     * with back stack state. {@link IllegalStateException} will be thrown if
     * {@link #addToBackStack(String)} has been previously called for this transaction
     * or if it is called after a call to <code>runOnCommit</code>.</p>
     *
     * @param runnable Runnable to add
     * @return this FragmentTransaction
     * @throws IllegalStateException if {@link #addToBackStack(String)} has been called
     */
    @Override public final FragmentTransaction runOnCommit(Runnable runnable)
    {return mFragmentTransaction.runOnCommit(runnable);}

    /**
     * Schedules a commit of this transaction.
     *
     * The commit does not happen immediately; it will be scheduled as work on the main thread
     * to be done the next time that thread is ready.
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     *
     * @return Returns the identifier of this transaction's back stack entry,
     * if {@link #addToBackStack(String)} had been called.  Otherwise, returns
     * a negative number.
     */
    @Override public final int commit()
    {return commitHub(mFragmentTransaction.commit());}

    /**
     * Like {@link #commit} but allows the commit to be executed after an
     * activity's state is saved.
     *
     * This is dangerous because the commit can be lost if the activity needs to later be restored
     * from its state, so this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    @Override public final int commitAllowingStateLoss()
    {return commitHub(mFragmentTransaction.commitAllowingStateLoss());}

    /**
     * Commits this transaction synchronously.
     *
     * Any added fragments will be initialized and brought completely to the lifecycle state of
     * their host and any removed fragments will be torn down accordingly before this call returns.
     * Committing a transaction in this way allows fragments to be added as dedicated, encapsulated
     * components that monitor the lifecycle state of their host while providing firmer ordering
     * guarantees around when those fragments are fully initialized and ready.
     *
     * Fragments that manage views will have those views created and attached.
     *
     * <p>Calling <code>commitNow</code> is preferable to calling
     * {@link #commit()} followed by {@link FragmentManager#executePendingTransactions()}
     * as the latter will have the side effect of attempting to commit <em>all</em>
     * currently pending transactions whether that is the desired behavior
     * or not.</p>
     *
     * <p>Transactions committed in this way may not be added to the
     * FragmentManager's back stack, as doing so would break other expected
     * ordering guarantees for other asynchronously committed transactions.
     * This method will throw {@link IllegalStateException} if the transaction
     * previously requested to be added to the back stack with
     * {@link #addToBackStack(String)}.</p>
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     */
    @Override public final void commitNow()
    {mFragmentTransaction.commitNow();}

    /**
     * Like {@link #commitNow} but allows the commit to be executed after an
     * activity's state is saved.
     *
     * This is dangerous because the commit can be lost if the activity needs to later be restored
     * from its state, so this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    @Override public final void commitNowAllowingStateLoss()
    {mFragmentTransaction.commitNowAllowingStateLoss();}

    /** Common add functionality. */
    private void addHub(int containerViewId, @NonNull Fragment fragment) {
        if (fragment instanceof ExtendedDialogFragment && containerViewId == 0) {
            final ExtendedDialogFragment extendedDialogFragment = (ExtendedDialogFragment)fragment;
            extendedDialogFragment.dialog();
        }
    }

    /** @param backStackId stack id of commit */
    private int commitHub(int backStackId) {
        if (mFragmentTransaction instanceof BackStackRecord) {
            final BackStackRecord backStackRecord = (BackStackRecord)mFragmentTransaction;
            for (int i = 0; i < backStackRecord.mOps.size(); i++) {
                final Fragment fragment = backStackRecord.mOps.get(i).fragment;
                if (fragment instanceof ExtendedDialogFragment)
                    ((ExtendedDialogFragment) fragment).mBackStackId = backStackId;
            }
        }
        return backStackId;
    }
}

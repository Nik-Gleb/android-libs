/*
 * ExtendedDialogFragment.java
 * bundle-fragments
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

package android.support.v4.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 17/04/2017
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
public class ExtendedDialogFragment extends DialogFragment {

    /** The state saved flag. */
    private boolean mSavedState = false;

    /** This is a dialog. */
    final void dialog() {
        mDismissed = false;
        mShownByMe = true;
        mViewDestroyed = false;
    }

    /** @param backStackId setup back-stack entry */
    @SuppressWarnings("unused")
    final void commit(int backStackId) {
        mBackStackId = backStackId;
    }

    /** @param value saved state value */
    private void setSavedState(boolean value) {
        if (mSavedState == value) return;
        mSavedState = value;
        onSavedStateChanged(mSavedState);
    }

    /** Set not saved state */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    void setStateNotSaved() {
        final FragmentManager fragmentManager =
                peekChildFragmentManager();
        if (fragmentManager != null)
            setStateNotSaved(fragmentManager);
        setSavedState(false);
    }

    /** {@inheritDoc} */
    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setSavedState(true);
    }

    /** @param value true when state saved */
    @SuppressWarnings({ "unused", "EmptyMethod" })
    protected void onSavedStateChanged(boolean value) {}

    /** @param fragmentManager the fragment manager for apply saved state */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    public static void setStateNotSaved(@NonNull FragmentManager fragmentManager) {
        final List<Fragment> fragments = getFragments(fragmentManager);
        if (fragments != null) {
            for (final Fragment fragment : fragments)
                if (fragment != null && fragment instanceof ExtendedDialogFragment)
                    ((ExtendedDialogFragment)fragment).setStateNotSaved();
        }
    }

    /**
     * @param fragmentManager the fragment manager
     * @return active fragments
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Fragment> getFragments(@NonNull FragmentManager fragmentManager)
    {return ((FragmentManagerImpl)fragmentManager).getActiveFragments();}

    /**
     * @param fragmentManager the fragment manager instance
     * @return the attached activity
     */
    @SuppressWarnings("unused")
    @Nullable
    public static FragmentActivity getActivity(@NonNull FragmentManager fragmentManager) {
        final FragmentHostCallback fragmentHostCallback =
            ((FragmentManagerImpl)fragmentManager).mHost;
        return fragmentHostCallback == null ? null :
            (FragmentActivity) fragmentHostCallback.getActivity();
    }

}

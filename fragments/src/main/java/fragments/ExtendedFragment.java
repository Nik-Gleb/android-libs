/*
 * ExtendedFragment.java
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.ExtendedDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 17/04/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("unused")
public class ExtendedFragment extends ExtendedDialogFragment {

    /** The attach to root inflate mode. */
    private static final boolean ATTACH_TO_ROOT = false;

    /** The title resources. */
    @StringRes protected int title, subtitle = 0;
    /** Inflate container. */
    @IdRes protected int container = android.R.id.content;
    /** The content layout. */
    @LayoutRes protected int content = 0;

    /** Primary fragment. */
    protected boolean primary = true;

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder builder = createAlertBuilder(savedInstanceState);
        return builder == null ? super.onCreateDialog(savedInstanceState): builder.create();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
        @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return content != 0 ? inflater.inflate(content, container, ATTACH_TO_ROOT) :
            getDialog() != null && getDialog().getWindow() != null &&
                getDialog().getWindow().getDecorView() != null ?
                getDialog().getWindow().getDecorView() :
                super.onCreateView(inflater, container, savedInstanceState);
    }


    /**
     * @param savedInstanceState the saved state
     * @return the alert dialog
     */
    @SuppressWarnings("SameReturnValue")
    @Nullable
    protected AlertDialog.Builder createAlertBuilder(@Nullable Bundle savedInstanceState)
    {return null;}//

    /** {@inheritDoc} */
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {

        if (dialog instanceof AppCompatDialog) {

            // If the dialog is an AppCompatDialog, we'll handle it
            AppCompatDialog acd = (AppCompatDialog) dialog;

            switch (style) {
                case STYLE_NO_INPUT:
                    //noinspection ConstantConditions
                    dialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    // fall through...
                case STYLE_NO_FRAME:
                case STYLE_NO_TITLE:
                    acd.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
            }
        } else {
            switch (style) {
                case STYLE_NO_INPUT:
                    //noinspection ConstantConditions
                    dialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    // fall through...
                case STYLE_NO_FRAME:
                case STYLE_NO_TITLE:
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            }
        }
    }

    /**
     * @param clazz class of fragment
     * @return tag-name
     */
    protected static String getTag(@NonNull Class<? extends ExtendedFragment> clazz) {
        final String target = "Fragment"; final String replacement = "";
        final String regex = "\\."; final String parts[] = clazz.getName().split(regex);
        if (parts.length < 1) throw new IllegalArgumentException("Invalid class");
        return parts[parts.length - 1].replace(target, replacement).toUpperCase();
    }

    /** @return the name of this fragment */
    @NonNull final String getName() {return getTag(getClass());}

    /**
     * @param view source view for extract context
     * @param <S> type of view
     *
     * @return related application context
     */
    public static <S> Context toContext(S view) {
        if (view instanceof FragmentActivity)
            return ((FragmentActivity)view).getApplicationContext();
        else if (view instanceof Fragment)
            return toContext(((Fragment)view).getActivity());
        else if (view instanceof android.view.View)
            return ((android.view.View)view).getContext().getApplicationContext();
        else throw new IllegalArgumentException("Unknown type");
    }

}

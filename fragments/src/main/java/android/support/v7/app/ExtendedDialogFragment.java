/*
 * ExtendedDialogFragment.java
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

package android.support.v7.app;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 17/04/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("unused")
public class ExtendedDialogFragment extends android.support.v4.app.ExtendedDialogFragment {

    /** The theme resource field name. */
    private static final String THEME_RESOURCE_FIELD_NAME = "mThemeResource";
    /** The theme resource field. */
    private static final Field THEME_RESOURCE_FIELD = getThemeResourceField();

    /** {@inheritDoc} */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final int theme = getTheme();
        final FragmentActivity activity = getActivity();
        return new AppCompatDialog(getContext(),
                theme == 0 && activity != null ?
                        getThemeResourceId(activity) : theme);
    }

    /** {@inheritDoc} */
    @Override
    public void setupDialog(@NonNull Dialog dialog, int style) {
        if (dialog instanceof AppCompatDialog) {
            // If the dialog is an AppCompatDialog, we'll handle it
            final AppCompatDialog appCompatDialog = (AppCompatDialog) dialog;
            switch (style) {
                case STYLE_NO_INPUT:
                    final Window window = dialog.getWindow();
                    if (window != null)
                        dialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    break;
                    // fall through...
                case STYLE_NO_FRAME:
                case STYLE_NO_TITLE:
                    appCompatDialog.supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
                    break;
            }
        }
    }


    /** The theme resource field. */
    @Nullable private static Field getThemeResourceField() {
        try {final Field result = ContextThemeWrapper.class
                    .getDeclaredField(THEME_RESOURCE_FIELD_NAME);
            result.setAccessible(true); return result;
        } catch (NoSuchFieldException e) {return null;}
    }

    /** The id of theme-resource. */
    private static int getThemeResourceId(@NonNull ContextThemeWrapper contextThemeWrapper) {
        if (THEME_RESOURCE_FIELD != null)
            try {return (int) THEME_RESOURCE_FIELD
                    .get(contextThemeWrapper);}
            catch (IllegalAccessException ignored) {}
        return 0;
    }


}

/*
 * OneShotLoader.java
 * loaders
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

package loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * The One-Shot Loader for only-once tasks.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 29/03/2017
 */
@Keep@KeepPublicProtectedClassMembers
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class OneShotLoader<T> extends BaseLoader<T> {

    /** The log-cat tag. */
    private static final String TAG = "OneShotLoader";

    /** The data was delivered. */
    private boolean mDelivered = false;

    /**
     * Constructs a new {@link OneShotLoader}.
     *
     * @param context the activity-context
     */
    public OneShotLoader(@NonNull Context context) {
        super(context, false);
    }

    /**
     * Constructs a new {@link OneShotLoader}.
     *
     * @param context  the activity-context
     * @param executor the runtime executor
     */
    public OneShotLoader(@NonNull Context context, @NonNull Executor executor) {
        super(context, executor, false);
    }

    /** {@inheritDoc} */
    @Override
    protected final void onDelivered(@Nullable T data, boolean isStarted) {
        mDelivered = true;
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean needDelivery(@Nullable T data) {
        return mDelivered;
    }

    /** {@inheritDoc} */
    @Override
    protected final boolean needLoad(@Nullable T data) {
        return !mDelivered;
    }

    /** {@inheritDoc} */
    @Override
    protected final void release(@NonNull T data) {}

    /** {@inheritDoc} */
    @Override
    protected final void onReset() {
        super.onReset();
    }
}

/*
 * LoaderManager.java
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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LoadersUtils;

import java.util.ArrayList;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

//import static android.support.v4.app.SupportUtils.getContext;

/**
 * The loader manager.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 30/03/2017
 */
@SuppressWarnings({"WeakerAccess", "unused", "EmptyMethod", "SameReturnValue"})
@Keep@KeepPublicProtectedClassMembers
public abstract class LoaderManager {

    /** Can not perform state exception. */
    private static final String CANT_PERFORM_STATE =
            "Can not perform this action after onSaveInstanceState";
    /** Can not perform closed exception. */
    private static final String CANT_PERFORM_CLOSE =
            "Can not perform this action after close";

    /** "Loader's" - state key. */
    private static final String STATE_LOADERS = "loaders";
    /** "Stable Loader's" - state key. */
    private static final String STATE_STABLES = "stables";

    /** The loader callbacks. */
    private final android.support.v4.app.LoaderManager.LoaderCallbacks<Object> mCallbacks =
            new android.support.v4.app.LoaderManager.LoaderCallbacks<Object>() {

                /** {@inheritDoc} */
                @SuppressWarnings("NullableProblems")
                @Override @NonNull public final Loader<Object> onCreateLoader(int id, @NonNull Bundle args)
                {checkState(); return LoaderManager.this.onCreateLoader(mContext, id, args);}

                /** {@inheritDoc} */
                @Override
                public final void onLoadFinished(@NonNull Loader<Object> loader, @Nullable Object data) {
                    checkState();
                    final int loaderId = loader.getId();

                    if (!mStableIds.contains(loaderId)) {
                        mLoaderManager.destroyLoader(loaderId);
                        mLoaders.remove(loaderId);
                    }

                    LoaderManager.this.onLoadFinished(loaderId, data);
                }

                /** {@inheritDoc} */
                @Override
                public final void onLoaderReset(@NonNull Loader<Object> loader) {
                    LoaderManager.this.onLoaderReset(loader.getId());
                }
            };


    /** The loader manager. */
    private final android.support.v4.app.LoaderManager mLoaderManager;
    /** Current loaders. */
    private final BundleMap mLoaders;
    /** Stable loader ids. */
    private final ArrayList<Integer> mStableIds;

    /** The application context. */
    private final Context mContext;

    /** Is loader-map is saved. */
    private boolean mSaved = false;
    /** Is loader-manager was closed. */
    private boolean mClosed;

    /**
     * Constructs a new {@link LoaderManager} with saved state.
     *
     * @param mgr the frameworks loader manager
     * @param state the saved state
     */
    public LoaderManager
    (@NonNull Context context, @NonNull android.support.v4.app.LoaderManager mgr,
        @Nullable Bundle state) { mContext = context;
        mLoaderManager = mgr;
        if (state != null) {
            final ArrayList<Integer> stableIds =
                state.getIntegerArrayList(STATE_STABLES);
            mStableIds = stableIds != null ? stableIds : new ArrayList<>();
            final BundleMap loaders = state.getParcelable(STATE_LOADERS);
            if (loaders != null) {
                mLoaders = loaders;
            } else {
                mLoaders = new BundleMap();
            }
        } else {
            mLoaders = new BundleMap();
            mStableIds = new ArrayList<>();
        }
    }

    /** Initialization. */
    @SuppressWarnings("ConstantConditions")
    protected final void init() {
        // Retain loader's callback
        final int count = mLoaders.size();
        for (int i = 0; i < count; i++) {
            final int loaderId = mLoaders.keyAt(i);
            if (mLoaderManager.getLoader(loaderId) != null) {
                final Bundle args = null;
                if (mLoaderManager.initLoader(loaderId, args, mCallbacks) == null) {
                    mLoaders.removeAt(i);
                    mStableIds.remove(loaderId);
                }
            } else {
                if (mLoaderManager.initLoader
                    (loaderId, mLoaders.get(loaderId), mCallbacks) == null) {
                    mLoaders.remove(loaderId);
                    mStableIds.remove(loaderId);
                }
            }
        }
    }

    /** Saved state changed callback */
    public final void savedStateChanged(boolean value) {mSaved = value;}

    /**
     * Backup current state.
     *
     * @param state the state container
     */
    public final void backup(@NonNull Bundle state) {
        checkState();
        state.putParcelable(STATE_LOADERS, mLoaders);
        state.putIntegerArrayList(STATE_STABLES, mStableIds);
    }

    /**
     * Start the loader.
     *
     * @param id the loader id
     * @param args the loader args
     */
    @SuppressWarnings("ConstantConditions")
    public final void startLoad(int id, @NonNull Bundle args, boolean stable) {
        checkState();
        if (mLoaderManager.getLoader(id) == null) {
            if (mLoaderManager.initLoader(id, args, mCallbacks) != null) {
                mLoaders.put(id, args);
                if (stable) mStableIds.add(id);
            }
        } else {
            if (mLoaderManager.restartLoader(id, args, mCallbacks) == null) {
                mLoaders.remove(id);
                mStableIds.remove(id);
            }
        }
    }

    /**
     * Stop the loader.
     *
     * @param id the loader id.
     */
    public final void stopLoad(int id) {
        checkState();
        if (mStableIds.contains(id)) {
            mLoaderManager.destroyLoader(id);
            mLoaders.remove(id);
            mStableIds.remove(id);
        } else {
            throw new IllegalStateException("Loader " + id + " missing");
        }
    }

    /**
     * @param id the loader id
     * @return true if loader exist, otherwise - false
     */
    protected boolean hasLoader(int id) {
        return mLoaderManager.getLoader(id) != null;
    }

    /**
     * @param id the loader id
     * @return true if loader running, otherwise - false
     */
    protected boolean isRunning(int id) {
        final Loader loader = mLoaderManager.getLoader(id);
        return loader != null && (!(loader instanceof AsyncTaskLoader)
            || LoadersUtils.isRunning((AsyncTaskLoader) loader));
    }

    /** Check Common State */
    private void checkState()
    {checkNotClosed(); if (mSaved) throw new IllegalStateException(CANT_PERFORM_STATE);}
    /** Check Created State */
    private void checkNotClosed()
    {if (mClosed) throw new IllegalStateException(CANT_PERFORM_CLOSE);}

    /**
     * The loaders resolver.
     *
     * @param context the context
     * @param id the loader id
     * @param args the args
     *
     * @return the loader instance
     */
    @NonNull
    protected abstract Loader<Object> onCreateLoader
    (@NonNull Context context, int id, @NonNull Bundle args);

    /**
     * Load finished resolver.
     *
     * @param id the loader id
     * @param data the loader data
     */
    protected void onLoadFinished(int id, @Nullable Object data)
    {/*SupportUtils.resetNoTransactionsBecause(mLoaderManager);*/}

    /**
     * Load finished resolver.
     *
     * @param id the loader id
     */
    protected void onLoaderReset(int id)
    {/*SupportUtils.resetNoTransactionsBecause(mLoaderManager);*/}

    /** Release resources */
    public final void close() {
        checkNotClosed();
        if (!mSaved) {
            mLoaders.clear();
            mStableIds.clear();
        }
        mClosed = true;
    }

    /** {@inheritDoc} */
    protected final void finalize() throws Throwable {
        try {
            if (!mClosed) {
                close();
                throw new RuntimeException (
                        "\nA resource was acquired at attached stack trace but never released." +
                                "\nSee java.io.Closeable for info on avoiding resource leaks."
                );
            }
        } finally {
            super.finalize();
        }
    }
}

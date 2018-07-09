/*
 * CollectionAdapter.java
 * widgets
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

package widgets.collections;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.AdapterListUpdateCallback;
import android.support.v7.util.BatchingListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import android.view.ViewGroup;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 29/04/2018
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
public final class CollectionAdapter<T extends CollectionAdapter.Item, U extends View & Consumer<T> & BooleanSupplier>
  extends RecyclerView.Adapter<CollectionAdapter.ViewHolder<T, U>> implements Observer<T[]> {

  /** The log-cat tag. */
  private static final String TAG = "CollectionAdapter";

  /** This adapter instance. */
  private final CollectionAdapter<T, U> mInstance = this;

  /** Adapter changes callback. */
  private final AdapterCallback<T> mCallback =
      new AdapterCallback<>(mInstance, mInstance::onPayloadReceived);

  /** View factory. */
  private final BiFunction<ViewGroup, Integer, ViewHolder<T, U>> mFactory;
  /** Click listener. */
  @NonNull private BiConsumer<RecyclerView, Integer>
      mClickListener = mInstance::onItemSelected;
  /** RecyclerView map. */
  private final HashMap<RecyclerView, RecyclerViewConfig>
      mRecyclerViews = new HashMap<>();

  /** Config supplier. */
  private final Supplier<RecyclerViewConfig> mConfigSupplier;

  /** Predictions skipper. */
  private final Predicate<T> mPredictionsSkipper;

  /** Array of items. */
  @NonNull private T[] mItems;

  /**
   * Constructs a {@link CollectionAdapter}.
   *
   * @param items initial items
   * @param factory view factory
   */
  public CollectionAdapter
  (@NonNull T[] items, @NonNull BiFunction<ViewGroup, Integer, ViewHolder<T, U>> factory,
      @NonNull Supplier<RecyclerViewConfig> supplier, @NonNull Predicate<T> predictionsSkipper)
  {setHasStableIds(false); mItems = items; mFactory = factory;
  mConfigSupplier = supplier; mPredictionsSkipper = predictionsSkipper;}

  /** {@inheritDoc} */
  @NonNull @Override public final ViewHolder<T, U> onCreateViewHolder
  (@NonNull ViewGroup parent, int type) {return mFactory.apply(parent, type);}

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override public final void onBindViewHolder
  (@NonNull ViewHolder<T, U> holder, int position, @NonNull List<Object> payloads) {
    if (payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
    else for (final Object value : payloads) holder.view.accept((T) value);
  }

  /** {@inheritDoc} */
  @Override public final void onBindViewHolder(@NonNull ViewHolder<T, U> holder, int position)
  {holder.view.accept(mItems[position]);}

  /** {@inheritDoc} */
  @Override public final void onViewRecycled(@NonNull ViewHolder<T, U> holder)
  {final T empty = null; holder.view.accept(empty);}

  /** {@inheritDoc} */
  @Override public final boolean onFailedToRecycleView(@NonNull ViewHolder<T, U> holder)
  {return holder.view.getAsBoolean();}

  /** {@inheritDoc} */
  @Override public final int getItemCount()
  {return mItems.length;}

  /** {@inheritDoc} */
  @Override public final int getItemViewType(int position)
  {return mItems[position].type;}

  /** {@inheritDoc} */
  @Override public final long getItemId(int position)
  {return mItems[position].hashCode();}

  /** {@inheritDoc} */
  @Override public final void onAttachedToRecyclerView(@NonNull
      RecyclerView recyclerView)
  {mRecyclerViews.put(recyclerView, mConfigSupplier.get().set(recyclerView, mClickListener));}

  /** {@inheritDoc} */
  @Override public final void onDetachedFromRecyclerView
  (@NonNull RecyclerView recyclerView)
  {mRecyclerViews.remove(recyclerView).reset(recyclerView);}

  /**
   * Calls when item-click was detected
   *
   * @param recyclerView recycler view
   * @param position position in adapter
   */
  private void onItemSelected(@NonNull RecyclerView recyclerView, int position)
  {final T item = mItems[position]; mRecyclerViews.get(recyclerView)
      .clickListener.accept(item.id, item.type);}

  /** @param payload payload changes */
  private void onPayloadReceived(@Nullable T payload) {
    final Set<RecyclerView> recyclers = mRecyclerViews.keySet();
    for (final RecyclerView recycler : recyclers) {
      final RecyclerView.LayoutManager manager = recycler.getLayoutManager();
      if (manager != null && Objects.equals
          (manager.getClass(), CollectionLayoutManagerLinear.class))
        if (mPredictionsSkipper.test(payload))
          ((CollectionLayoutManagerLinear)manager).skipPredictions();
    }
  }
  /**
   * Called when the data is changed.
   *
   * @param newItems the new data
   */
  @Override public final void onChanged(@Nullable T[] newItems) {
    final T[] oldItems = mItems; mItems = Objects.requireNonNull(newItems);
    CollectionDiffs.calc(oldItems, newItems).dispatch(mCallback);
  }

  /** The view holder of this adapter. */
  @Keep
  @KeepPublicProtectedClassMembers
  public static final class ViewHolder
      <T, U extends View & Consumer<T> & BooleanSupplier> extends RecyclerView.ViewHolder
  {public final U view; public ViewHolder(@NonNull U view) {super(view); this.view = view;}}

  /**
   * Base Item..
   *
   * @author Nikitenko Gleb
   * @since 1.0, 29/04/2018
   */
  @Keep
  @KeepPublicProtectedClassMembers
  public static abstract class Item {

    /** ID of item. */
    public final int id;

    /** Type of view. */
    public final int type;

    /** Hash of object */
    private final int mHash;

    /**
     * Constructs a new {@link Item}
     *
     * @param id id of view
     * @param type type of view
     */
    protected Item(int id, int type)
    {this.id = id; this.type = type; mHash = (31 + id) * 31 + type;}

    /** {@inheritDoc} */
    @Override public final int hashCode() {return mHash;}
  }

  /** The {@link RecyclerView} configuration */
  @Keep
  @KeepPublicProtectedClassMembers
  public static final class RecyclerViewConfig {

    /** Click listener. */
    final BiConsumer<Integer, Integer> clickListener;
    /** Snap helper. */
    private final SnapHelper mSnapHelper;
    /** Item animator. */
    private final RecyclerView.ItemAnimator mItemAnimator;

    /** On item touch listener. */
    @Nullable private RecyclerView.OnItemTouchListener mOnItemTouchListener = null;

    /**
     * Constructs a new {@link RecyclerViewConfig}.
     *
     * @param clickListener click listener
     * @param snapHelper snap helper
     * @param itemAnimator item animator
     */
    public RecyclerViewConfig(BiConsumer<Integer, Integer> clickListener,
        SnapHelper snapHelper, RecyclerView.ItemAnimator itemAnimator)
    {this.clickListener = clickListener; mSnapHelper = snapHelper;
    mItemAnimator = itemAnimator;}

    /**
     * @param recyclerView recycler-view for setup
     * @param listener click listener
     */
    @NonNull RecyclerViewConfig set(@NonNull RecyclerView recyclerView,
        @NonNull BiConsumer<RecyclerView, Integer> listener) {
      recyclerView.addOnItemTouchListener(mOnItemTouchListener =
          new CollectionTouchHelper(recyclerView.getContext(), listener));
      if (mSnapHelper != null) mSnapHelper.attachToRecyclerView(recyclerView);
      recyclerView.setItemAnimator(mItemAnimator);
      return this;
    }

    /** @param recyclerView reset recycler */
    void reset(@NonNull RecyclerView recyclerView) {
      recyclerView.setItemAnimator(close(mItemAnimator));
      if (mSnapHelper != null) mSnapHelper.attachToRecyclerView(null);
      recyclerView.removeOnItemTouchListener
          (mOnItemTouchListener = close(mOnItemTouchListener));
    }

    /** @param object to close */
    @Nullable private static <T> T close(@Nullable Object object) {
      if (object == null) return null;
      if (object instanceof Closeable)
        try {((Closeable)object).close();}
        catch (IOException ignored) {}
      return null;
    }
  }

  /**
   * Adapter changes callback.
   *
   * @param <T> type of element
   */
  private static final class AdapterCallback<T> extends
      BatchingListUpdateCallback {

    /** Payloads consumer. */
    private final Consumer<T> mConsumer;

    /**
     * Constructs a new {@link AdapterCallback}.
     *
     * @param adapter adapter-owner
     * @param consumer payloads consumer
     */
    AdapterCallback(@NonNull CollectionAdapter adapter, @NonNull Consumer<T> consumer)
    {super( new AdapterListUpdateCallback(adapter)); mConsumer = consumer;}

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public final void onChanged(int position, int count, @Nullable Object payload)
    {mConsumer.accept((T) payload); super.onChanged(position, count, payload);}
  }
}

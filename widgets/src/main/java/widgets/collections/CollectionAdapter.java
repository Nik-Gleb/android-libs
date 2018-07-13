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

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.AdapterListUpdateCallback;
import android.support.v7.util.BatchingListUpdateCallback;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView.BufferType;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Collections.binarySearch;
import static java.util.Comparator.comparingInt;
import static widgets.collections.CollectionAdapter.Item.create;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 29/04/2018
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep@KeepPublicProtectedClassMembers
public final class CollectionAdapter<T extends View & Consumer<CollectionAdapter.Item> & BooleanSupplier>
  extends RecyclerView.Adapter<CollectionAdapter.ViewHolder<T>> {

  /** The log-cat tag. */
  private static final String TAG = "CollectionAdapter";

  /** This adapter instance. */
  private final CollectionAdapter<T> mInstance = this;

  /** Adapter changes callback. */
  private final AdapterCallback mCallback =
      new AdapterCallback(mInstance, mInstance::onPayloadReceived);

  /** View factory. */
  private final BiFunction<ViewGroup, Integer, ViewHolder<T>> mFactory;

  /** Click listener. */
  @NonNull private BiConsumer<RecyclerView, Integer>
      mClickListener = mInstance::onItemSelected;

  /** RecyclerView map. */
  private final HashMap<RecyclerView, RecyclerViewConfig>
      mRecyclerViews = new HashMap<>();

  /** Config supplier. */
  private final Supplier<RecyclerViewConfig> mConfigSupplier;

  /** Predictions skipper. */
  private final Predicate<Object> mPredictionsSkipper;

  /** Array of items. */
  @NonNull private Item[] mItems;

  /** Items selections. */
  @NonNull private Set<Integer> mSelections;

  /**
   * Constructs a {@link CollectionAdapter}.
   *
   * @param items initial items
   * @param factory view factory
   */
  public CollectionAdapter
  (@NonNull Item[] items, @NonNull Set<Integer> selections,
    @NonNull BiFunction<ViewGroup, Integer, ViewHolder<T>> factory,
      @NonNull Supplier<RecyclerViewConfig> supplier, @NonNull Predicate<Object> predictionsSkipper)
  {setHasStableIds(false); mItems = items; mSelections = selections; mFactory = factory;
  mConfigSupplier = supplier; mPredictionsSkipper = predictionsSkipper;}

  /** {@inheritDoc} */
  @NonNull @Override public final ViewHolder<T> onCreateViewHolder
  (@NonNull ViewGroup parent, int type) {return mFactory.apply(parent, type);}

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override public final void onBindViewHolder
  (@NonNull ViewHolder<T> holder, int position, @NonNull List<Object> payloads) {
    if (payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
    else
      for (final Object value : payloads)
       if(value.getClass() == Boolean.class)
         holder.view.setActivated((Boolean) value);
       else holder.view.accept((Item) value);
  }

  /** {@inheritDoc} */
  @Override public final void onBindViewHolder(@NonNull ViewHolder<T> holder, int position)
  {holder.view.accept(mItems[position]); holder.view.setActivated(mSelections.contains(mItems[position].id));}

  /** {@inheritDoc} */
  @Override public final void onViewRecycled(@NonNull ViewHolder<T> holder)
  {final Item empty = null; holder.view.accept(empty);}

  /** {@inheritDoc} */
  @Override public final boolean onFailedToRecycleView(@NonNull ViewHolder<T> holder)
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
  @Override public final void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
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
  {mRecyclerViews.get(recyclerView).clickListener.accept(position);}

  /** @param payload payload changes */
  private void onPayloadReceived(@Nullable Object payload) {
    final Set<RecyclerView> recyclers = mRecyclerViews.keySet();
    for (final RecyclerView recycler : recyclers) {
      final RecyclerView.LayoutManager manager = recycler.getLayoutManager();
      if (manager != null && manager instanceof CollectionLayoutManagerLinear)
        if (mPredictionsSkipper.test(payload))
          ((CollectionLayoutManagerLinear)manager).skipPredictions();
    }
  }

  /**
   * Called when the data is changed.
   *
   * @param newItems the new data
   */
  public final void onChanged(@NonNull Item[] newItems) {
    sort(newItems, comparingInt(o -> o.id));
    final Item[] oldItems = mItems; mItems = Objects.requireNonNull(newItems);
    CollectionDiffs.calc(oldItems, newItems).dispatch(mCallback);
  }

  /**
   * Called when the data is changed.
   *
   * @param newSelections the new data
   */
  public final void onChanged(@NonNull Set<Integer> newSelections) {
    if (Objects.equals(mSelections, newSelections)) return;
    final Set<Integer> retained = new HashSet<>(mSelections);
    final Set<Integer> removed = new HashSet<>(mSelections);
    final Set<Integer> added = new HashSet<>(newSelections);
    retained.retainAll(newSelections);
    added.removeAll(retained);
    removed.removeAll(retained);
    mSelections = newSelections;
    notifySelections(added, true);
    notifySelections(removed, false);
    mCallback.dispatchLastEvent();
  }

  /**
   * @param set set of selections
   * @param added true for added, otherwise - removed
   */
  private void notifySelections(@NonNull Set<Integer> set, boolean added) {
    for (final int id : set) {
      final int position = getPositionById(id);
      if (position >= 0) mCallback.onChanged(position, 1, added);
    }
  }

  /**
   * @param id id of item
   *
   * @return index position
   */
  @SuppressWarnings("SuspiciousArrayMethodCall")
  public final int getPositionById(int id)
  {return binarySearch(asList(mItems), create(id), comparingInt(o -> o.id));}

  /** The view holder of this adapter. */
  @Keep@KeepPublicProtectedClassMembers
  public static final class ViewHolder<U extends View &
    Consumer<Item> & BooleanSupplier> extends RecyclerView.ViewHolder
  {public final U view; public ViewHolder(@NonNull U view)
  {super(view); this.view = view;}}



  /**
   * Base Item..
   *
   * @author Nikitenko Gleb
   * @since 1.0, 29/04/2018
   */
  @Keep@KeepPublicProtectedClassMembers
  public static final class Item {

    /** No drawable. */
    public static final Drawable NO_DRAWABLE = null;

    /** Default buffer. */
    public static final BufferType NORMAL = BufferType.NORMAL;

    /** Empty text. */
    public static final CharSequence NO_TEXT = "";

    /** Empty item. */
    public static final Item EMPTY =
      new Item(-1, -1, NORMAL, NO_TEXT, NO_DRAWABLE);

    /** ID of item. */
    public final int id;

    /** Type of view. */
    public final int type;

    /** Hash of object */
    private final int mHash;

    /** Char sequence */
    final CharSequence chars;

    /** Buffer type */
    final BufferType buffer;

    /** Drawable resource */
    final Drawable drawable;

    /**
     * Constructs a new {@link Item}
     *
     * @param id id of view
     * @param type type of view
     */
    private Item(int id, int type, @NonNull BufferType buffer,
      @NonNull CharSequence chars, @Nullable Drawable drawable)
    {this.id = id; this.type = type; mHash = (31 + id) * 31 + type;
    this.buffer = buffer; this.chars = chars; this.drawable = drawable;}

    /** {@inheritDoc} */
    @Override public final int hashCode() {return mHash;}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Item)) return false;
      final Item item = (Item) obj;
      return
        buffer == item.buffer &&
        Objects.equals(chars, item.chars) &&
        Objects.equals(drawable, item.drawable);
    }

    /** @return new created item */
    @NonNull public static <T> Item create
    (@NonNull T item, @NonNull Drawable icon)
    {return new Item(item.hashCode(), 0, NORMAL, NO_TEXT, icon);}

    /** @return new created item */
    @NonNull public static <T> Item create
    (@NonNull T item, int type, @NonNull Drawable icon)
    {return new Item(item.hashCode(), type, NORMAL, NO_TEXT, icon);}

    /** @return new created item */
    @NonNull public static <T> Item create
    (@NonNull T item, @NonNull CharSequence chars)
    {return create(item.hashCode(), NORMAL, chars);}

    /** @return new created item*/
    @NonNull public static <T> Item create
    (@NonNull T item, @NonNull BufferType buffer, @NonNull CharSequence chars)
    {return new Item(item.hashCode(), 0, buffer, chars, null);}

    /** @return new created item*/
    @NonNull static Item create(int id)
    {return new Item(id, 0, NORMAL, NO_TEXT, NO_DRAWABLE);}
  }

  /** The {@link RecyclerView} configuration */
  @Keep
  @KeepPublicProtectedClassMembers
  public static final class RecyclerViewConfig {

    /** Click listener. */
    final IntConsumer clickListener;

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
    public RecyclerViewConfig(@NonNull IntConsumer clickListener,
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

  /** Adapter changes callback. */
  private static final class AdapterCallback extends BatchingListUpdateCallback {

    /** Payloads consumer. */
    private final Consumer<Object> mConsumer;

    /**
     * Constructs a new {@link AdapterCallback}.
     *
     * @param adapter adapter-owner
     * @param consumer payloads consumer
     */
    AdapterCallback(@NonNull CollectionAdapter adapter, @NonNull Consumer<Object> consumer)
    {super( new AdapterListUpdateCallback(adapter)); mConsumer = consumer;}

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public final void onChanged(int position, int count, @Nullable Object payload)
    {mConsumer.accept(payload); super.onChanged(position, count, payload);}
  }
}

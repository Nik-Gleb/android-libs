/*
 * CollectionItemText.java
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

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import java.io.Closeable;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 27/09/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
@Keep
@KeepPublicProtectedClassMembers
public class CollectionItemText extends AppCompatTextView
    implements Closeable, Consumer<CollectionItemText.Item>, BooleanSupplier {

  /** The log cat tag. */
  private static final String TAG = "CollectionItemText";

  /** Empty attribute set. */
  private static final AttributeSet EMPTY_ATTRS_SET = null;
  /** The empty style resource. */
  @StyleRes private static final int EMPTY_STYLE = 0;

  /** The default attr resource. */
  @AttrRes private static final int DEFAULT_ATTRS = android.R.attr.textViewStyle;

  /** This instance. */
  private final CollectionItemText mInstance = this;

  /** Current item value. */
  @NonNull private Item mItem = Item.EMPTY;

  /** "CLOSE" flag-state. */
  private volatile boolean mClosed;


  /**
   * Constructs a new {@link CollectionItemText} with context.
   *
   * @param context current context
   */
  public CollectionItemText(@NonNull Context context)
  {this(context, EMPTY_ATTRS_SET);}

  /**
   * Constructs a new {@link CollectionItemText} with context and attributes.
   *
   * @param context current context
   * @param attrs   current attributes
   */
  public CollectionItemText(@NonNull Context context, @Nullable AttributeSet attrs)
  {this(context, attrs, DEFAULT_ATTRS);}


  /**
   * Constructs a new {@link CollectionItemText} with context, attributes and
   * default-Style.
   *
   * @param context      current context
   * @param attrs        attributes
   * @param defStyleAttr default Style
   */
  public CollectionItemText
  (@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr)
  {super(context, attrs, defStyleAttr); accept(mItem);}

  /** {@inheritDoc} */
  @Override public final void close()
  {if (mClosed) return; mClosed = true;}

  /** {@inheritDoc} */
  @Override protected final void finalize() throws Throwable
  {try {close();} finally {super.finalize();}}

  /** {@inheritDoc} */
  @Override public final boolean getAsBoolean() {
    System.out.println("CollectionItemView.getAsBoolean");
    return false;
  }

  /** {@inheritDoc} */
  @Override public final void accept(@Nullable Item item) {
    item = item == null ? Item.EMPTY : item;
    if (Objects.equals(mItem, item)) return;
    setText(item.chars, item.buffer);
    setSelected(item.selected);
    mItem = item;
  }

  @SuppressWarnings({ "ConstantConditions", "NumericOverflow" })
  @Override protected final void onMeasure(int widthSpec, int heightSpec) {
    final boolean horizontal = true;
    final float count = 4.0f;
    final int widthMode, heightMode;
    if (horizontal) {
      widthSpec = Math.round((float)((RecyclerView)getParent()).getWidth() / count);
      heightMode = MeasureSpec.getMode(heightSpec); widthMode = MeasureSpec.EXACTLY;
    } else {
      heightSpec = Math.round((float)((RecyclerView)getParent()).getMeasuredHeight() / count);
      widthMode = MeasureSpec.getMode(widthSpec); heightMode = MeasureSpec.EXACTLY;
    }
    super.onMeasure
        (MeasureSpec.makeMeasureSpec(widthSpec, widthMode),
            MeasureSpec.makeMeasureSpec(heightSpec, heightMode));
  }

  /** Item data */
  @Keep
  @KeepPublicProtectedClassMembers
  public static final class Item extends CollectionAdapter.Item {

    /** Empty item. */
    private static final Item EMPTY =
        new Item(-1, -1, "", BufferType.NORMAL, false);

    /** Char sequence */
    final CharSequence chars;

    /** Buffer type */
    final BufferType buffer;

    /** Selected state. */
    boolean selected;

    /**
     * Constructs a new {@link Item}.
     *
     * @param id   id of view
     * @param type type of view
     * @param selected selected flag
     *
     */
    public Item
    (int id, int type,@NonNull CharSequence chars, @NonNull BufferType buffer, boolean selected)
    {super(id, type); this.chars = chars; this.buffer = buffer; this.selected = selected;}


    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof Item)) return false;
      final Item item = (Item) obj;
      return selected == item.selected &&
          Objects.equals(chars, item.chars) &&
          Objects.equals(buffer, item.buffer);
    }

  }


}

package app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * The retain container.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 15/09/2017
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class Retain<T> implements Parcelable {

  /** The Parcelable Creator. */
  @SuppressWarnings("unused")
  public static final Creator CREATOR = new Creator();

  /** The content. */
  private final T mContent;

  /** Constructs a new {@link Retain} */
  private Retain(T content)
  {this.mContent = content;}

  /** Constructs a new {@link Retain}. */
  @SuppressWarnings("unused")
  private Retain(Parcel source)
  {this.mContent = null;}

  /** {@inheritDoc} */
  @Override public final void writeToParcel(Parcel dest, int flags){}

  /** {@inheritDoc} */
  @Override public final int describeContents() {return 0;}

  /**
   * Extract from saved-state.
   *
   * @param state the "saved-state" instance
   * @param key the saved-state key
   *
   * @param <U> casted type
   *
   * @return instance if exist
   */
  public static <U> U get(Bundle state, String key) {
    final Retain<U> retain = state.getParcelable(key);
    return retain != null ? retain.mContent : null;
  }

  /**
   * Put to saved-state.
   *
   * @param state the "saved-state" instance
   * @param key the saved-state
   * @param value the value
   *
   * @param <U> casted type
   */
  public static <U> void put(Bundle state, String key, U value)
  {state.putParcelable(key, new Retain<>(value));}

  /**
   * Parcel Creator.
   *
   * @author Gleb Nikitenko
   * @since 1.0, 04/09/2017
   */
  @SuppressWarnings("WeakerAccess, unused")
  public static final class Creator implements Parcelable.Creator<Retain> {

    /** {@inheritDoc} */
    @Override
    public final Retain createFromParcel(Parcel source)
    {return new Retain(source);}

    /** {@inheritDoc} */
    @Override
    public final Retain[] newArray(int size)
    {return new Retain[size];}
  }
}
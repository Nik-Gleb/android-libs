package data;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Raw table item-data.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2018
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
@Keep@KeepPublicProtectedClassMembers
public final class TableRow {

  /** Identifier. */
  public final int id;

  /** Blob-data. */
  public final byte[] data;

  /**
   * Constructs a new {@link TableRow}.
   *
   * @param cursor source cursor
   */
  private TableRow(@NonNull Cursor cursor)
  {this.id = (int) cursor.getLong(0); this.data = cursor.getBlob(1);}

  /**
   * Constructs a new {@link TableRow}.
   *
   * @param id identifier
   * @param data byte-content
   */
  public TableRow(int id, @NonNull byte[] data)
  {this.id = id; this.data = data;}

  /** {@inheritDoc} */
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof TableRow)) return false;
    final TableRow that = (TableRow) obj;
    return id == that.id && Arrays.equals(data, that.data);
  }

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {return id;}

  /**
   * @param cursor cursor
   *
   * @return stream of blobs
   */
  @NonNull public static Stream<TableRow> stream(@NonNull Cursor cursor) {
    return StreamSupport.stream(((Iterable<TableRow>) () -> new Iterator<TableRow>() {
      private boolean hasNext = false; {setHasNext(cursor.moveToFirst());}
      @Override public final boolean hasNext() {return hasNext;}
      @Override @NonNull public final TableRow next()
      {try {return new TableRow(cursor);}
      finally {setHasNext(cursor.moveToNext());}}
      private void setHasNext(boolean value)
      {if(!(hasNext = value)) cursor.close();}
    }).spliterator(), false);
  }
}

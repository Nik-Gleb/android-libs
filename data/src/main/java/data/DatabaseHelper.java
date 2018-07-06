/*
 * DatabaseHelper.java
 * data
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

package data;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.util.Locale;

/**
 * Database Helper.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 19/08/2016
 */
@SuppressWarnings("WeakerAccess, unused")
final class DatabaseHelper extends SQLiteOpenHelper {

  /** The log-cat tag */
  private static final String TAG = "DatabaseHelper";

  /** The Helper instance. */
  private static DatabaseHelper sSingleton = null;

  /** Tables. */
  private final DatabaseTable[] mDatabaseTables;

  /**
   * Create a helper object to create, open, and/or manage a database.
   * This method always returns very quickly.  The database is not actually
   * created or opened until one of {@link #getWritableDatabase} or
   * {@link #getReadableDatabase} is called.
   *
   * @param context to use to open or create the database
   * @param name    of the database file, or null for an in-memory database
   * @param factory to use for creating cursor objects, or null for the default
   * @param version number of the database (starting at 1); if the database is older,
   *                {@link #onUpgrade} will be used to upgrade the database; if the database is
   *                newer, {@link #onDowngrade} will be used to downgrade the database
   */
  private DatabaseHelper(@NonNull Context context, @NonNull String name,
      @NonNull CursorFactory factory, int version) {
    super(context, name, factory, version);
    mDatabaseTables = new DatabaseTable[0];
  }

  /**
   * Create a helper object to create, open, and/or manage a database.
   * The database is not actually created or opened until one of
   * {@link #getWritableDatabase} or {@link #getReadableDatabase} is called.
   * <p>
   * <p>Accepts input param: a concrete instance of {@link DatabaseErrorHandler} to be
   * used to handle corruption when sqlite reports database corruption.</p>
   *
   * @param context      to use to open or create the database
   * @param name         of the database file, or null for an in-memory database
   * @param factory      to use for creating cursor objects, or null for the default
   * @param version      number of the database (starting at 1); if the database is older,
   *                     {@link #onUpgrade} will be used to upgrade the database; if the database is
   *                     newer, {@link #onDowngrade} will be used to downgrade the database
   * @param errorHandler the {@link DatabaseErrorHandler} to be used when sqlite reports database
   */
  private DatabaseHelper(@NonNull Context context, @NonNull String name,
      @NonNull CursorFactory factory, int version,
      @NonNull DatabaseErrorHandler errorHandler) {
    super(context, name, factory, version, errorHandler);
    mDatabaseTables = new DatabaseTable[0];
  }

  /**
   * Constructs a new DatabaseHelper with context and hardcoded name and version.
   *
   * @param context application context
   */
  private DatabaseHelper(@NonNull Context context, @NonNull String name,
      int version, @NonNull DatabaseTable[] tables) {
    super(context, name, null, version);
    mDatabaseTables = tables;
  }

  /**
   * @param context Application context
   *
   * @return DB Helper SingleTone
   */
  public static synchronized DatabaseHelper getInstance
  (@NonNull Context context, @NonNull String name, int version,
      @NonNull DatabaseTable[] tables) {
    if (sSingleton == null)
      sSingleton = new DatabaseHelper(context, name, version, tables);
    return sSingleton;
  }

  /** @param context application context */
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  public static void delete(@NonNull Context context) {
    if (sSingleton != null)
      context.deleteDatabase(sSingleton.getDatabaseName());
  }

  /**
   * Called when the database is created for the first time. This is addWhere the
   * creation of tables and the initial population of the tables should happen.
   *
   * @param db The database.
   */
  @Override
  public final void onCreate(@NonNull SQLiteDatabase db) {
    db.setPageSize(8192);
    db.setLocale(Locale.US);
    db.setMaxSqlCacheSize(SQLiteDatabase.MAX_SQL_CACHE_SIZE);
    for (final DatabaseTable table : mDatabaseTables) table.onPrepare(db, 0);
  }

  /**
   * Called when the database needs to be upgraded. The implementation
   * should use this method to drop tables, add tables, or do anything else it
   * needs to upgrade to the new schema version.
   * <p>
   * <p>
   * The SQLite ALTER TABLE documentation can be found
   * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
   * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
   * you can use ALTER TABLE to rename the old table, then create the new table and then
   * populate the new table with the contents of the old table.
   * </p><p>
   * This method executes within a transaction.  If an exception is thrown, all changes
   * will automatically be rolled back.
   * </p>
   *
   * @param db         The database.
   * @param oldVersion The old database version.
   * @param newVersion The new database version.
   */
  @Override
  public final void onUpgrade
  (@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
    for (final DatabaseTable table : mDatabaseTables)
      table.onPrepare(db, oldVersion);
  }

  @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    super.onDowngrade(db, oldVersion, newVersion);
  }

  /** {@inheritDoc} */
  @Override
  public final void onOpen(@NonNull SQLiteDatabase db) {
    super.onOpen(db);
    System.out.println("DatabaseHelper.onOpen");
    db.enableWriteAheadLogging();
    //db.execSQL("PRAGMA temp_store MEMORY");
    db.rawQuery("PRAGMA synchronous = OFF;", null);
    db.rawQuery("PRAGMA journal_mode = WAL;", null);
  }

  /** {@inheritDoc} */
  @Override
  public final void onConfigure(SQLiteDatabase db) {
    super.onConfigure(db);
    db.setForeignKeyConstraintsEnabled(false);
  }

   /*
    *   Upgrade Tables Strategy
    *
        static void onPrepare(@NonNull SQLiteDatabase db, int oldVersion) {
            switch (oldVersion) {
                case  0: create(db); break;
                case 1: case 2: case 3:
                case 4: upgrade0105(db);
                case 6: case 7:
                case 8: upgrade0509(db);
            }
        }

        static void create(@NonNull SQLiteDatabase db) {}
        static void upgrade0105(@NonNull SQLiteDatabase db) {}
        static void upgrade0509(@NonNull SQLiteDatabase db) {}


        case  0: create(db);
        ...
        case <cur_ver>: upgrade01<cur_ver+1>(db);

        case ...: upgrade...<old_ver>(db);
        ...
        case <cur_ver>: upgrade<old_ver><cur_ver+1>(db);

    */
}

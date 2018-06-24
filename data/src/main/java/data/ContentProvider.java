package data;

import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static data.Provider.stub;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2018
 */
@Keep
@KeepPublicProtectedClassMembers
public final class ContentProvider extends
    android.content.ContentProvider implements Provider {

  /** Log cat tag. */
  @SuppressWarnings("unused")
  private static final String TAG = "ContentProvider";
  
  /** The root node. */
  private static final int ROOT = 0;

  /** The storages map. */
  @NonNull private final HashMap<String, Provider> mCallMap = new HashMap<>();

  /** The uri matcher. */
  @Nullable private UriMatcher mUriMatcher = null;

  /** The sub-storages. */
  @Nullable private Provider[] mProviders = null;

  /** {@inheritDoc} */
  @Override
  public final boolean onCreate() {
    final Context context  = Objects.requireNonNull(getContext());
    final ProviderInfo info = getProviderInfo(context);
    final Bundle meta = info.metaData == null ? Bundle.EMPTY : info.metaData;
    final String name = getDatabaseName(meta);
    final int version = getDatabaseVersion(meta);
    final String[] tables = getDatabaseTables(meta);
    final String authority = info.authority;

    mProviders = createProviders(context, authority, name, version, tables);
    mUriMatcher = new UriMatcher(ROOT) {{
      for (int i = 0; i < mProviders.length; i++) {
        final Provider storage = mProviders[i];
        final String[] paths = storage.getSupportedPaths();
        for (final String path : paths) {
          //System.out.println(authority + " " + path + " " + i);
          addURI(authority, path, i);
        }
        mCallMap.put(paths[0], storage);
        storage.onCreate();
      }
    }};
    return true;
  }

  /**
   * @param context application context
   *
   * @return content provider info
   */
  @NonNull private static ProviderInfo getProviderInfo(@NonNull Context context) {
    final int flag = PackageManager.GET_META_DATA;
    final PackageManager packageManager = context.getPackageManager();
    final ComponentName component = new ComponentName(context, ContentProvider.class);
    try {return packageManager.getProviderInfo(component, flag);}
    catch (PackageManager.NameNotFoundException exception)
    {throw new RuntimeException(exception);}
  }

  /**
   * @param meta content provider meta-data
   *
   * @return data-base name
   */
  @NonNull private static String getDatabaseName(@NonNull Bundle meta)
  {return meta.getString("database.name", "database.sqlite3");}

  /**
   * @param meta content provider meta-data
   *
   * @return data-base version
   */
  private static int getDatabaseVersion(@NonNull Bundle meta)
  {return meta.getInt("database.version", 1);}

  /**
   * @param meta content provider meta-data
   *
   * @return data-base tables
   */
  @NonNull private static String[] getDatabaseTables(@NonNull Bundle meta)
  {return meta.getString("database.tables", "").split(";");}

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].insert(uri, values);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Override
  public final int update(@NonNull Uri uri, @Nullable ContentValues values,
      @Nullable String selection, @Nullable String[] selectionArgs) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].update(uri, values, selection, selectionArgs);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Override
  public final int delete(@NonNull Uri uri, @Nullable String selection,
      @Nullable String[] selectionArgs) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].delete(uri, selection, selectionArgs);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Cursor query(@NonNull Uri uri, @Nullable String[] proj,
      @Nullable String sel, @Nullable String[] selArgs,
      @Nullable String sort, @Nullable CancellationSignal signal) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].query(uri, proj, sel, selArgs, sort, signal);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Cursor query(@NonNull Uri uri, @Nullable String[] proj,
      @Nullable String sel, @Nullable String[] selArgs,
      @Nullable String sort) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].query(uri, proj, sel, selArgs, sort);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final String getType(@NonNull Uri uri) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].getType(uri);
    return stub(uri);
  }

  /** {@inheritDoc} */
  @Override
  public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].bulkInsert(uri, values);
    return super.bulkInsert(uri, values);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @NonNull
  @Override
  public final ContentProviderResult[] applyBatch
  (@NonNull ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    assert mUriMatcher != null; assert mProviders != null;
    final HashMap<Provider, ArrayList<ContentProviderOperation>>
        providersMap = new HashMap<>();
    for (final ContentProviderOperation operation : operations) {
      final int match = mUriMatcher.match(operation.getUri());
      if (match > -1 && match < mProviders.length) {
        final Provider key = mProviders[match];
        final ArrayList<ContentProviderOperation> value =
            providersMap.getOrDefault(key, new ArrayList<>());
        value.add(operation);
      }
    }
    final ArrayList<ContentProviderResult> results = new ArrayList<>();
    final Set<Provider> keys = providersMap.keySet();
    for (final Provider provider : keys)
      results.addAll(Arrays.asList(provider.applyBatch
          (providersMap.getOrDefault(provider, new ArrayList<>()))));

    return results.toArray(new ContentProviderResult[results.size()]);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final ParcelFileDescriptor openFile(@NonNull Uri uri,
      @NonNull String mode) throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openFile(uri, mode);
    return super.openFile(uri, mode);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final ParcelFileDescriptor openFile(@NonNull Uri uri,
      @NonNull String mode, @Nullable CancellationSignal signal)
      throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openFile(uri, mode, signal);
    return super.openFile(uri, mode, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openAssetFile(@NonNull Uri uri,
      @NonNull String mode) throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openAssetFile(uri, mode);
    return super.openAssetFile(uri, mode);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  @SuppressWarnings("ConstantConditions")
  public final AssetFileDescriptor openAssetFile(@NonNull Uri uri,
      @NonNull String mode, @Nullable CancellationSignal signal)
      throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openAssetFile(uri, mode, signal);
    return super.openAssetFile(uri, mode, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
      @NonNull String filter, @Nullable Bundle opts)
      throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openTypedAssetFile(uri, filter, opts);
    return super.openTypedAssetFile(uri, filter, opts);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
      @NonNull String filter, @Nullable Bundle opts,
      @Nullable CancellationSignal signal) throws FileNotFoundException {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].openTypedAssetFile(uri, filter, opts, signal);
    return super.openTypedAssetFile(uri, filter, opts, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final String[] getStreamTypes(@NonNull Uri uri, @NonNull String filter) {
    assert mUriMatcher != null; assert mProviders != null;
    final int match = mUriMatcher.match(uri);
    if (match > -1 && match < mProviders.length)
      return mProviders[match].getStreamTypes(uri, filter);
    return super.getStreamTypes(uri, filter);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Bundle call
  (@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    final int slash = method.indexOf("/");
    final String key = slash != -1 ? method.substring(0, slash) : method;
    final Provider storage = mCallMap.get(key);
    return storage != null ? storage.call(method, arg, extras) :
        super.call(method, arg, extras);
  }

  /** {@inheritDoc} */
  @Override
  public final void shutdown() {
    assert mProviders != null;
    for (final Provider storage : mProviders)
      storage.shutdown();
  }

  /** {@inheritDoc} */
  @Override
  public final void dump(@NonNull FileDescriptor fd,
      @NonNull PrintWriter writer, @Nullable String[] args) {
    assert mProviders != null;
    for (final Provider storage : mProviders)
      storage.dump(fd, writer, args);
  }

  /**
   * Create child storages.
   *
   * @param context the application context
   * @return the child storages
   */
  @NonNull private Provider[] createProviders
  (@NonNull Context context, @NonNull String authority,
      @NonNull String name, int version, @NonNull String[] tables) {
    return new Provider[] {
        new DatabaseProvider(context, authority, name, version, tables)
    };
  }
}

package data;

import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
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
import android.text.TextUtils;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static android.content.ContentResolver.SCHEME_CONTENT;
import static data.Provider.stub;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 19/06/2018
 */
@Keep@KeepPublicProtectedClassMembers
public final class ContentProvider extends
    android.content.ContentProvider implements Provider {

  /** Log cat tag. */
  @SuppressWarnings("unused")
  private static final String TAG = "ContentProvider";

  /** Internal providers. */
  private final Map<String, Provider> mProviders = new HashMap<>();

  /** {@inheritDoc} */
  @Override
  public final boolean onCreate() {
    final Context context  = Objects.requireNonNull(getContext());

    //SQLiteStudioService.instance().start(context);

    final ProviderInfo info = getProviderInfo(context);
    final Bundle meta = info.metaData == null ? Bundle.EMPTY : info.metaData;
    final String name = getDatabaseName(meta);
    final int version = getDatabaseVersion(meta);
    final String[] tables = getDatabaseTables(meta);
    final String authority = info.authority;
    create(context, authority, name, version, tables, mProviders);
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
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.insert(uri, values);
  }

  /** {@inheritDoc} */
  @Override
  public final int update
  (@NonNull Uri uri, @Nullable ContentValues values,
      @Nullable String sel, @Nullable String[] args) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.update(uri, values, sel, args);
  }

  /** {@inheritDoc} */
  @Override
  public final int delete(@NonNull Uri uri, @Nullable String sel, @Nullable String[] args) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.delete(uri, sel, args);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Cursor query
  (@NonNull Uri uri, @Nullable String[] proj, @Nullable String sel, @Nullable String[] args,
      @Nullable String sort, @Nullable CancellationSignal signal) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.query(uri, proj, sel, args, sort, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Cursor query
  (@NonNull Uri uri, @Nullable String[] proj, @Nullable String sel, @Nullable String[] args, @Nullable String sort) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.query(uri, proj, sel, args, sort);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final String getType(@NonNull Uri uri) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? stub(uri) : storage.getType(uri);
  }

  /** {@inheritDoc} */
  @Override
  public final int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.bulkInsert(uri, values) : storage.bulkInsert(uri, values);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @NonNull
  @Override
  public final ContentProviderResult[] applyBatch
  (@NonNull ArrayList<ContentProviderOperation> operations)
      throws OperationApplicationException {
    final HashMap<Provider, ArrayList<ContentProviderOperation>>
        providersMap = new HashMap<>();
    for (final ContentProviderOperation operation : operations) {
      final Provider storage = mProviders.get(operation.getUri().getScheme());
      if (storage != null) {
        final ArrayList<ContentProviderOperation> value = providersMap
            .computeIfAbsent(storage, provider -> new ArrayList<>());
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
  public final ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openFile(uri, mode) : storage.openFile(uri, mode);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final ParcelFileDescriptor openFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openFile(uri, mode, signal) :
        storage.openFile(uri, mode, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openAssetFile(uri, mode) :
        storage.openAssetFile(uri, mode);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  @SuppressWarnings("ConstantConditions")
  public final AssetFileDescriptor openAssetFile
  (@NonNull Uri uri, @NonNull String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openAssetFile(uri, mode, signal) :
        storage.openAssetFile(uri, mode, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openTypedAssetFile
  (@NonNull Uri uri, @NonNull String filter, @Nullable Bundle opts) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openTypedAssetFile(uri, filter, opts) :
        storage.openTypedAssetFile(uri, filter, opts);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final AssetFileDescriptor openTypedAssetFile(@NonNull Uri uri,
      @NonNull String filter, @Nullable Bundle opts,
      @Nullable CancellationSignal signal) throws FileNotFoundException {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.openTypedAssetFile(uri, filter, opts, signal) :
        storage.openTypedAssetFile(uri, filter, opts, signal);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final String[] getStreamTypes(@NonNull Uri uri, @NonNull String filter) {
    uri = uncanonicalize(uri); final Provider storage = mProviders.get(uri.getScheme());
    return storage == null ? super.getStreamTypes(uri, filter) :
        storage.getStreamTypes(uri, filter);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  public final Bundle call
  (@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
    final Provider storage = mProviders.get(method);
    return storage == null ? super.call(method, arg, extras) :
        storage.call(method, arg, extras);
  }

  /** {@inheritDoc} */
  @Override
  public final void shutdown() {
    final Collection<Provider> providers = mProviders.values();
    for (final Provider storage : providers) storage.shutdown();
    //SQLiteStudioService.instance().stop();
  }

  /** {@inheritDoc} */
  @Override
  public final void dump(@NonNull FileDescriptor fd,
      @NonNull PrintWriter writer, @Nullable String[] args) {
    final Collection<Provider> providers = mProviders.values();
    for (final Provider storage : providers) storage.dump(fd, writer, args);
  }

  /** Create child providers. */
  private void create(@NonNull Context context, @NonNull String authority,
      @NonNull String name, int version, @NonNull String[] tables, @NonNull Map<String, Provider> map) {
    map.put(Provider.getTag(HttpsProvider.class), new HttpsProvider(context, authority, version));
    map.put(Provider.getTag(FilesProvider.class), new FilesProvider(context, authority, version));
    map.put(Provider.getTag(AssetsProvider.class), new AssetsProvider(context, authority, version));
    map.put(Provider.getTag(TablesProvider.class), new TablesProvider(context, authority, name, version, tables));
    final Collection<Provider> providers = map.values();
    for (final Provider storage : providers) storage.onCreate();
  }

  /** {@inheritDoc} */
  @Nullable @Override public final Uri canonicalize(@NonNull Uri uri) {
    uri = uncanonicalize(uri); if (SCHEME_CONTENT.equals(uri.getScheme())) return super.canonicalize(uri);
    return uri.buildUpon().appendQueryParameter("from", uri.getScheme()).scheme(SCHEME_CONTENT).build();
  }

  /** {@inheritDoc} */
  @SuppressWarnings("ConstantConditions")
  @NonNull @Override public Uri uncanonicalize(@NonNull Uri uri) {
    if (!SCHEME_CONTENT.equals(uri.getScheme())) return super.uncanonicalize(uri);
    final Set<String> keys = new HashSet<>(uri.getQueryParameterNames());
    final String scheme = cutQuery(uri, keys, "from");
    if (TextUtils.isEmpty(scheme)) throw new IllegalArgumentException("scheme query missing!");
    final Uri.Builder builder = uri.buildUpon().clearQuery().scheme(scheme);
    for (final String key : keys) builder.appendQueryParameter(key, uri.getQueryParameter(key));
    return builder.build();
  }

  /**
   * @param uri   uri resource
   * @param keys  query keys
   * @param key   search key
   *
   * @return query value
   */
  @SuppressWarnings("SameParameterValue")
  @Nullable private static String cutQuery
  (@NonNull Uri uri, @NonNull Set<String> keys, @NonNull String key) {
    if (!keys.contains(key)) return null;
    else
      try {return uri.getQueryParameter(key);}
      finally {keys.remove(key);}
  }
}

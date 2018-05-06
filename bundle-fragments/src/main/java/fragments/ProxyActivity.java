package fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Proxy Activity.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 05/05/2018
 */
@Keep
@KeepPublicProtectedClassMembers
public final class ProxyActivity extends Activity {

  /** Intent Action for Request Permissions.*/
  private static final String ACTION_REQUEST_PERMISSIONS =
      "android.content.pm.action.REQUEST_PERMISSIONS";
  /** Intent Extra for Request Permissions (Names).*/
  private static final String EXTRA_REQUEST_PERMISSIONS_NAMES =
      "android.content.pm.extra.REQUEST_PERMISSIONS_NAMES";
  /** Intent Extra for Request Permissions (Results).*/
  private static final String EXTRA_REQUEST_PERMISSIONS_RESULTS =
      "android.content.pm.extra.REQUEST_PERMISSIONS_RESULTS";

  /** Zero constant. */
  private static final int ZERO = 0;

  /** Current clients. */
  @NonNull private ArrayList<IBinder> mBinders = new ArrayList<>();

  /** {@inheritDoc} */
  @Override protected final void onCreate(@Nullable Bundle bundle) {
    setVisible(false);
    super.onCreate(bundle); final Intent intent = getIntent();
    setIntent(null); if (intent != null) handle(intent);
  }

  /** {@inheritDoc} */
  @Override protected final void onNewIntent(@NonNull Intent intent)
  {super.onNewIntent(intent); handle(intent);}

  /** @param intent incoming intent */
  @SuppressWarnings("ConstantConditions")
  private void handle(@NonNull Intent intent) {
    final IBinder binder = Transport.binder(intent);
    final Intent request = Transport.intent(intent);
    final int index = Equalator.search(mBinders, request);
    if (index != -1)
      send(binder, ((Transport)mBinders.get(index)).result.get(), index);
    else
      if (mBinders.add(binder))
        startActivityForResult(request, mBinders.size());
  }

  /** {@inheritDoc} */
  @Override protected final void onActivityResult
      (int request, int result, @Nullable Intent data) {
    if (request > mBinders.size()) return; final int index = request - 1;
    if (data != null && ACTION_REQUEST_PERMISSIONS.equals(data.getAction())) {
      final Bundle extras = requireNonNull(data.getExtras());
      final String[] permissions = requireNonNull((String[])
          extras.get(EXTRA_REQUEST_PERMISSIONS_NAMES));
      final int[] results = requireNonNull((int[])
          extras.get(EXTRA_REQUEST_PERMISSIONS_RESULTS));
      IntStream.range(0, permissions.length).forEach(value -> {
        final int res = results[value];
        results[value] = res == PackageManager.PERMISSION_DENIED &&
            !shouldShowRequestPermissionRationale(permissions[value]) ? -2 : res;
      }); data.putExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS, results);
    } send(mBinders.get(index), new Result(result, data), index);
  }

  /**
   * @param binder target binder
   * @param result data result
   * @param index index for remove
   */
  private void send
  (@NonNull IBinder binder, @NonNull Result result, int index) {
    if (Transport.send(binder, result)) mBinders.remove(index);
    if (mBinders.size() == ZERO) finish();
  }

  /**
   * @param context application context
   * @param intent  target intent
   *
   * @return activity result
   */
  @Nullable
  public static Optional<Result> execute
      (@NonNull Context context, @NonNull Intent intent)
  {return new Transport(context, intent).result;}

  /**
   * @param context application context
   * @param permission target permission
   *
   * @return activity result
   */
  @SuppressWarnings("ConstantConditions")
  public static int permission
  (@NonNull Context context, @NonNull String permission) {
    final Result result =
        new Transport(context, new Intent(ACTION_REQUEST_PERMISSIONS)
            .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, new String[]{permission})
            .setPackage(context.getPackageManager().getInstallerPackageName
                (context.getPackageName()))).result.get();
    return result.data.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS)[0];
  }

  /** Proxy result. */
  @SuppressWarnings("WeakerAccess")
  public static final class Result implements Parcelable {

    /** The Parcelable Creator. */
    @SuppressWarnings("unused")
    public static final Creator CREATOR = new Creator();

    /** Result code. */
    public final int result;

    /** Result data. */
    @Nullable public final Intent data;

    /**
     * Constructs a new {@link Result}.
     *
     * @param result  result code
     * @param data    result data
     */
    private Result(int result, @Nullable Intent data)
    {this.result = result; this.data = data;}

    /** {@inheritDoc} */
    @Override public final String toString()
    {return "Result{" + "result=" + result + ", data=" + data + '}';}

    /** {@inheritDoc} */
    @Override public final int describeContents() {return ZERO;}

    /** {@inheritDoc} */
    @Override public final void writeToParcel(@NonNull Parcel dest, int flags)
    {dest.writeInt(result); dest.writeParcelable(data, flags);}

    /**
     * Constructs a new {@link Result}.
     *
     * @param source parcel
     */
    private Result(@NonNull Parcel source)
    {this.result = source.readInt();
    this.data = source.readParcelable(Intent.class.getClassLoader());}


    /** Parcel Creator. */
    @SuppressWarnings("WeakerAccess, unused")
    public static final class Creator implements
        Parcelable.Creator<Result> {
      /** {@inheritDoc} */
      @Override @NonNull
      public final Result
      createFromParcel(Parcel source)
      {return new Result(source);}
      /** {@inheritDoc} */
      @Override @NonNull
      public final Result[]
      newArray(int size) {return new Result[size];}
    }
  }

  /** Inter process transport. */
  private static final class Transport extends Binder {

    /** Transactions flag. */
    private static final int FLAG = IBinder.FLAG_ONEWAY;

    /** Transport protocol transaction code: "result". */
    private static final int RESULT_TRANSACTION =
        ('_'<<24)|('R'<<16)|('E'<<8)|'S';

    /** Extra keys. */
    private static final String
        EXTRA_INTENT = "intent",
        EXTRA_CALLBACK = "callback";

    /** Lock. */
    private final Object mLock = new Object();

    /** Source intent. */
    private final Intent mIntent;

    /** Result of call */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Nullable Optional<Result> result;

    /** Interrupted state. */
    private boolean mInterrupted = false;

    /**
     * Constructs a new {@link Transport}.
     *
     * @param context application context
     * @param intent source intent
     */
    private Transport(@NonNull Context context, @NonNull Intent intent) {
      final Bundle extras = new Bundle();
      extras.putBinder(EXTRA_CALLBACK, this);
      extras.putParcelable(EXTRA_INTENT, mIntent = intent);
      final Intent wrappedIntent = new Intent(context, ProxyActivity.class)
          .putExtras(extras).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(wrappedIntent); result = get();
    }

    /** Waiting result */
    @SuppressWarnings({ "ConstantConditions", "FinalizeCalledExplicitly" })
    @NonNull
    private Optional<Result> get() {
      synchronized (mLock) {
        while (result == null) {
          try {mLock.wait();}
          catch (InterruptedException e)
          {currentThread().interrupt();
            mInterrupted = true; try {finalize();}
            catch (Throwable ignored) {} break;}
        } return result;
      }
    }

    /** @param parcel result parcel */
    private boolean set(@Nullable Parcel parcel) {
      synchronized (mLock) {
        result = ofNullable(parcel != null ?
                new Result(parcel) : null);
        mLock.notify(); return !mInterrupted;
      }
    }

    /** {@inheritDoc} */
    @Override protected final boolean onTransact(int code, @NonNull Parcel data,
        @Nullable Parcel reply, int flags) throws RemoteException {
      return super.onTransact(code, data, reply, flags) ||
          code == RESULT_TRANSACTION && set(data);
    }

    /**
     * @param binder binder instance for send result
     * @param result result content
     *
     * @return true if sending was successful
     */
    @SuppressWarnings("UnusedReturnValue")
    static boolean send(@NonNull IBinder binder, @NonNull Result result) {
      final Parcel data = Parcel.obtain(), reply = null; result.writeToParcel(data, ZERO);
      try {return binder.transact(RESULT_TRANSACTION, data, reply, FLAG);}
      catch (RemoteException e) {return false;} finally {data.recycle();}
    }

    /**
     * @param intent incoming intent
     * @return target intent
     */
    @NonNull
    static Intent intent(@NonNull Intent intent)
    {return requireNonNull(requireNonNull
        (intent.getExtras()).getParcelable(EXTRA_INTENT));}

    /**
     * @param intent incoming intent
     * @return binder client
     */
    @NonNull
    static IBinder binder(@NonNull Intent intent)
    {return requireNonNull(requireNonNull
        (intent.getExtras()).getBinder(EXTRA_CALLBACK));}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      return mIntent == obj || obj instanceof Intent &&
          mIntent.filterEquals((Intent) obj);
    }

    /** {@inheritDoc} */
    @Override public final int hashCode()
    {return mIntent.filterHashCode();}
  }

  /** {@link Intent} with {@link Transport} equalator. */
  private static final class Equalator {

    /** Search intent. */
    private final Intent mIntent;

    /**
     * Constructs new {@link Equalator}.
     * @param intent search intent
     */
    private Equalator
    (@NonNull Intent intent)
    {mIntent = intent;}

    /** {@inheritDoc} */
    @Override public final boolean equals(Object obj) {
      return mIntent == obj || obj instanceof Transport &&
          ((Transport)obj).mIntent.filterEquals(mIntent);
    }

    /**
     * @param list source list
     * @param intent search intent
     *
     * @return index of element
     */
    static int search(@NonNull List list, @NonNull Intent intent)
    {return list.indexOf(new Equalator(intent));}
  }
}

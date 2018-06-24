package widgets.collections;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 10/05/2018
 */
@Keep
@KeepPublicProtectedClassMembers
public final class CollectionLayoutManagerLinear extends LinearLayoutManager {

  private boolean mWithoutPredictions = false;

  /**
   * Creates a vertical LinearLayoutManager
   *
   * @param context Current context, will be used to access resources.
   */
  public CollectionLayoutManagerLinear(@NonNull Context context)
  {super(context);}

  /**
   * @param context       Current context, will be used to access resources.
   * @param orientation   Layout orientation. Should be {@link #HORIZONTAL} or {@link
   *                      #VERTICAL}.
   * @param reverseLayout When set to true, layouts from end to start.
   */
  public CollectionLayoutManagerLinear
  (@NonNull Context context, int orientation, boolean reverseLayout)
  {super(context, orientation, reverseLayout);}

  /**
   * Constructor used when layout manager is set in XML by RecyclerView attribute
   * "layoutManager". Defaults to vertical orientation.
   */
  public CollectionLayoutManagerLinear
  (@NonNull Context context, @NonNull AttributeSet attrs, int defStyleAttr, int defStyleRes)
  {super(context, attrs, defStyleAttr, defStyleRes);}

  /** Skip layout predictions */
  public final void skipPredictions() {mWithoutPredictions = true;}

  /** {@inheritDoc} */
  @Override public final boolean supportsPredictiveItemAnimations()
  {return !mWithoutPredictions && super.supportsPredictiveItemAnimations();}

  /** {@inheritDoc} */
  @Override public final void onLayoutCompleted(RecyclerView.State state)
  {super.onLayoutCompleted(state); mWithoutPredictions = false;}

}

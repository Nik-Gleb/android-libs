/*
 * 	SimpleViewTarget.java
 * 	ommy-ar
 *
 * 	Copyright (C) 2017, Emoji Apps Inc. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of Emoji Apps Incorporated and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to Emoji Apps Incorporated and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from Emoji Apps Incorporated.
 */

package widgets.glide;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * A base {@link com.bumptech.glide.request.target.Target} for displaying
 * resources in {@link TextView}s.
 *
 * @param <Z> The type of resource that this target will display in the wrapped
 * {@link TextView}.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 25/09/2017
 */
@SuppressWarnings("unused")
@Keep@KeepPublicProtectedClassMembers
public abstract class SimpleViewTarget<Z> extends ViewTarget<View, Z>
    implements Transition.ViewAdapter {

  /** "ME"-reference. */
  @NonNull private final SimpleViewTarget<Z> me = this;


  /** The animatable. */
  @Nullable private Animatable mAnimatable = null;


  /**
   * Constructs a new {@link SimpleViewTarget}
   *
   * @param view the target view instance
   */
  @SuppressWarnings("WeakerAccess")
  public SimpleViewTarget(@NonNull View view) {
    super(view);
  }

  /**
   * @return the current {@link Drawable} being
   * displayed in the view using {@link TextView}.
   */
  @Override
  @Nullable
  public Drawable getCurrentDrawable() {
    return view.getBackground();
  }

  /**
   * Sets the given {@link Drawable} on the view
   * using {@link TextView}.
   *
   * @param drawable {@inheritDoc}
   */
  @Override
  public void setDrawable(@Nullable Drawable drawable) {
    view.setBackground(drawable);
  }

  /**
   * Sets the given {@link Drawable} on the view
   * using {@link TextView}.
   *
   * @param placeholder {@inheritDoc}
   */
  @Override
  public void onLoadStarted(@Nullable Drawable placeholder) {
    super.onLoadStarted(placeholder);
    setResourceInternal(null);
    setDrawable(placeholder);
  }

  /**
   * Sets the given {@link Drawable} on the view
   * using {@link TextView}.
   *
   * @param errorDrawable {@inheritDoc}
   */
  @Override
  public void onLoadFailed(@Nullable Drawable errorDrawable) {
    super.onLoadFailed(errorDrawable);
    setResourceInternal(null);
    setDrawable(errorDrawable);
  }

  /**
   * Sets the given {@link Drawable} on the view
   * using {@link TextView}.
   *
   * @param placeholder {@inheritDoc}
   */
  @Override
  public void onLoadCleared(@Nullable Drawable placeholder) {
    super.onLoadCleared(placeholder);
    setResourceInternal(null);
    setDrawable(placeholder);
  }

  /** {@inheritDoc} */
  @Override public void onResourceReady
  (@Nullable Z res, @Nullable Transition<? super Z> trans) {
    if (trans == null || !trans.transition(res, me)) setResourceInternal(res);
    else maybeUpdateAnimatable(res);
  }

  /** {@inheritDoc} */
  @Override public void onStart()
  {if (mAnimatable != null) mAnimatable.start();}

  /** {@inheritDoc} */
  @Override
  public void onStop()
  {if (mAnimatable != null) mAnimatable.stop();}

  /** @param resource for setup internal */
  private void setResourceInternal(@Nullable Z resource)
  {maybeUpdateAnimatable(resource); setResource(resource);}

  /** @param resource the resource for update animatable */
  private void maybeUpdateAnimatable(@Nullable Z resource) {
    if (resource instanceof Animatable) {
      mAnimatable = (Animatable) resource;
      mAnimatable.start();
    } else mAnimatable = null;
  }

  /** @param resource the resource for setup */
  protected abstract void setResource(@Nullable Z resource);

}

/*
 * 	BitmapSimpleViewTarget.java
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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.bumptech.glide.request.target.Target;

import java.util.function.BiFunction;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * A {@link Target} that can display an
 * {@link Bitmap} in an {@link View}.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 25/09/2017
 */
@SuppressWarnings("unused")
@Keep@KeepPublicProtectedClassMembers
public final class BitmapSimpleViewTarget extends SimpleViewTarget<Bitmap> {

  /** Drawable factory. */
  private final BiFunction<View, Bitmap, Drawable> mFactory;

  /**
   * Constructs a new {@link BitmapSimpleViewTarget}
   *
   * @param view the target view instance
   */
  public BitmapSimpleViewTarget
  (@NonNull View view, @NonNull BiFunction<View, Bitmap, Drawable> factory)
  {super(view); mFactory = factory;}

  /** @param res the resource for setup */
  @Override protected final void setResource(@Nullable Bitmap res)
  {setDrawable(mFactory.apply(view, res));}
}

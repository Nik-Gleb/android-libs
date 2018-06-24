/*
 * 	RecyclerUtils.java
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

package android.support.v7.widget;

import android.support.annotation.NonNull;

import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 28/09/2017
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings("unused")
public class RecyclerUtils {

  public static RecyclerView getRecyclerView(@NonNull RecyclerView.ViewHolder viewHolder) {
    return viewHolder.mOwnerRecyclerView;
  }
}

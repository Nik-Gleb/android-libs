/*
 * 	Analytics.java
 * 	model
 *
 * 	Copyright (C) 2017, OmmyChat ltd. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of OmmyChat limited and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to OmmyChat limited and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from OmmyChat limited.
 */

package clean;

import android.support.annotation.NonNull;

import java.io.Closeable;

/**
 * Base Analytics Interface.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 24/08/2017
 */
public interface Analytics extends Closeable {

  /** Track screen moves. */
  void sendScreenView(@NonNull String screen);

  /** Track exception. */
  void sendException(boolean fatal, @NonNull String description);

  /** {@inheritDoc} */
  void close();
}

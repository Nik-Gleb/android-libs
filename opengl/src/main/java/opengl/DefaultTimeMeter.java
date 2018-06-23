package opengl;

import android.util.Log;

/**
 * Default time meter.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 14/12/2017
 */
public final class DefaultTimeMeter {

  /** One mills const. */
  private final static long ONE_MILLISECOND_NS = 1000000;

  /** How far ahead of time we schedule frames. */
  private static final int[] FRAME_AHEAD = {0, 1, 2, 3};

  /** Frame update patterns. */
  private static final String[] UPDATE_PATTERNS = {
      "4",        // 15 fps
      "32",       // 24 fps
      "32322",    // 25 fps
      "2",        // 30 fps
      "2111",     // 48 fps
      "1",        // 60 fps
      "15"        // erratic, useful for examination with systrace
  };

  private int mUpdatePatternOffset, mHoldFrames;
  private long mPreviousRefreshNs;

  /**
   * These have slightly different names from the equivalents in the Activity
   * to reduce confusion.
   **/
  @SuppressWarnings("unused")
  private int mUpdatePatternIdx = 4, mFramesAheadIdx = 1, mChoreographerSkips, mDroppedFrames;

  /** The refresh time in nanoseconds */
  private final long mRefresh;

  /**
   * Constructs a new {@link DefaultTimeMeter}
   *
   * @param fps the num of frames per second
   */
  public DefaultTimeMeter(float fps)
  {mRefresh = Math.round(1000000000L / fps);
    System.out.println("REFRESH: " + mRefresh);}

  /**
   * @param time the frame time
   *
   * @return advance-status
   */
  public final boolean advance(long time) {
    boolean draw = false;

    if (mHoldFrames > 1) mHoldFrames--;
    else {
      mUpdatePatternOffset =
          (mUpdatePatternOffset + 1) %
              UPDATE_PATTERNS[mUpdatePatternIdx].length();
      // The hold time for the current update pattern index/offset.
      mHoldFrames =
          UPDATE_PATTERNS[mUpdatePatternIdx]
          .charAt(mUpdatePatternOffset) - '0';
      draw = true;
    }

    boolean complain = false;

    /*
     * Watch for Choreographer skipping frames.
     * The current implementation doesn't handle these.
     */
    if (mPreviousRefreshNs != 0 &&
        time - mPreviousRefreshNs > mRefresh + ONE_MILLISECOND_NS) {
      mChoreographerSkips++;
      complain = true;
      Log.d("TAG", mRefresh + ": Choreographer skip: " +
          ((mRefresh - mPreviousRefreshNs) / 1000000.0) + " ms " + ((time -mPreviousRefreshNs) / 1000000));
    }

    mPreviousRefreshNs = time;

    /*
     * Check to see if we're falling behind.
     * We do this by comparing the Choreographer reported-vsync time to the
     * current time, and seeing if we're already into the next refresh.
     *
     * We could drop a frame by changing "draw" from true to false, but as
     * noted elsewhere we don't necessarily want to do that every time we miss
     * our window. For now we just complain and carry on.
     */
    final long diff = System.nanoTime() - time;
    if (diff > mRefresh - ONE_MILLISECOND_NS) {
      mDroppedFrames++;  // more like "should have dropped" frames
      complain = true;
      Log.d("TAG", mRefresh + ": overrun: " + (diff / 1000000.0) + " ms");
    }

    /*if (complain) {
      System.out.println("Dropped: " + mDroppedFrames + "; skip: " + mChoreographerSkips);
    }*/

    return draw;
  }

  /**
   * @param time the frame time
   *
   * @return presentation delay
   */
  public final long presentation(long time) {
    final int framesAhead = FRAME_AHEAD[mFramesAheadIdx];
    if (framesAhead == 0) return framesAhead;
    return time + mRefresh * framesAhead;
  }
}

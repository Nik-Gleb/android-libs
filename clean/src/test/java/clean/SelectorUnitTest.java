package clean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine
 * (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SelectorUnitTest implements Observable.OnChangedListener {

  /** Test arrays */
  private static final String[]
      ARRAY_NULL =  null,
      ARRAY_EMPTY = {},
      ARRAY_NULLS = new String[2],
      ARRAY_A =     {"A"},
      ARRAY_AB =    {"A", "B"},
      ARRAY_ABC =   {"A", "B", "C"},
      ARRAY_BC =    {"B", "C"},
      ARRAY_C =     {"C"},
      ARRAY_XYZ =   {"X", "Y", "Z"};

  /** This instance */
  private final SelectorUnitTest mInstance = this;
  /** Test provider. */
  private Provider mProvider = null;
  /** Test selector. */
  private Selector<String> mSelector = null;
  /** Expected result. */
  private Selector.Selection<String> mExpected = null;
  /** Callback was handled. */
  private boolean mHandled = false;

  /** @throws Exception by error */
  @SuppressWarnings("ConstantConditions")
  @Before public final void setUp() throws Exception {
    mSelector = new Selector<>(mProvider = new Provider());
    mSelector.registerObserver(mInstance);

    // Initial test
    int index = -1; String[] values = null;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); onChanged(); checkHandled();

  }

  /** @throws Exception by error */
  @After public final void tearDown() throws Exception {
    mSelector.unregisterObserver(mInstance);
    mSelector.close(); mProvider.close();
    mSelector = null; mProvider = null;
  }

  /** {@inheritDoc} */
  @Override public final void onChanged() {
    mHandled = true; try {assertEquals(mExpected, mSelector.get());}
    catch (Throwable throwable) {fail(throwable.getMessage());}
  }

  /** Check was handled */
  private void checkHandled()
  {assertTrue(mHandled); mHandled = false;}

  /** Check wasn't handled */
  private void checkNotHandled()
  {assertFalse(mHandled);}


  /** @throws Exception by error */
  @SuppressWarnings("ConstantConditions")
  @Test public final void testNoIndexNullArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_NULL;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexEmptyArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_EMPTY;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexNullsArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_NULLS;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexAArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_A;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexABArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_AB;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexABCArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_ABC;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexBCArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_BC;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void testNoIndexCArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    final int index = -1; final String[] values = ARRAY_C;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);

    if (Objects.equals(prevSelection, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void test0IndexEmptyArray() throws Throwable {
    final Selector.Selection<String> prevSelection = mSelector.get();

    Selector.Selection<String> prev = mSelector.get();
    int index = -1; final String[] values = ARRAY_EMPTY;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get();
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(0);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void test0IndexNullsArray() throws Throwable {

    Selector.Selection<String> prev = mSelector.get();
    int index = -1; final String[] values = ARRAY_NULLS;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); index = 0;
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(index);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();
  }

  /** @throws Exception by error */
  @Test public final void test1IndexNullsArray() throws Throwable {

    Selector.Selection<String> prev = mSelector.get();
    int index = -1; final String[] values = ARRAY_NULLS;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();


    prev = mSelector.get(); index = 1;
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(index);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

  }

  /** @throws Exception by error */
  @Test public final void test2IndexNullsArray() throws Throwable {

    Selector.Selection<String> prev = mSelector.get();
    int index = -1; final String[] values = ARRAY_NULLS;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();


    prev = mSelector.get(); index = 2;
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(index);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

  }

  /** @throws Exception by error */
  @Test public final void testABC() throws Throwable {

    Selector.Selection<String> prev = mSelector.get();
    int index = -1; String[] values = ARRAY_ABC;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    for (int i = -2; i < 4; i++) {
      prev = mSelector.get(); index = 1;
      mExpected = new Selector.Selection<>
          (i == -2 ? -1 : i==3 ? 2: i, prev.values);
      checkNotHandled(); mSelector.select(i);
      if (Objects.equals(prev, mSelector.get()))
        checkNotHandled(); else checkHandled();
    }

    prev = mSelector.get(); index = 1;
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(index);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_AB;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_A; index = 0;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_C; index = 0;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_EMPTY; index = -1;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();


    prev = mSelector.get(); values = ARRAY_NULLS; index = -1;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_XYZ; index = -1;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); index = 2;
    mExpected = new Selector.Selection<>(index, prev.values);
    checkNotHandled(); mSelector.select(index);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_ABC; index = 2;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_BC; index = 1;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

    prev = mSelector.get(); values = ARRAY_A; index = 0;
    mExpected = new Selector.Selection<>(index, values);
    checkNotHandled(); mProvider.setData(values);
    if (Objects.equals(prev, mSelector.get()))
      checkNotHandled(); else checkHandled();

  }



  /** Test Provider */
  private static final class Provider extends Observable<String[]> {

    /** Mock array */
    private String[] mData;


    /** {@inheritDoc} */
    @Override public final String[] get() throws Throwable {return mData;}

    /** @param data mock data */
    final void setData(String[] data) {mData = data; notifyChanged();}
  }


}
package extensions;

import android.support.annotation.NonNull;

import java.io.Closeable;
import java.util.Stack;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 25/06/2018
 */
@SuppressWarnings("unused")
public interface Disposable extends Closeable {

  /** @param closeables for push */
  @Inject default void inject
  (@NonNull @Named("stack")
      Stack<Closeable> closeables)
  {closeables.push(this);}
}

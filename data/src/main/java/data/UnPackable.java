package data;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;

import okhttp3.ResponseBody;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * UnPersistable interface.
 *
 * @param <T> the object type
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/10/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
@FunctionalInterface
public interface UnPackable<T> {

  /**
   * @param input input object
   *
   * @return unpacked object
   *
   * @throws IOException serialize exception
   */
  @NonNull T unpack(@NonNull ObjectInput input) throws IOException;

  /**
   * @param source data source
   *
   * @return result of loaded data
   */
  @NonNull default Optional<T> response
  (@NonNull Supplier<ResponseBody> source) {
    try (final ResponseBody response = source.get()) {
      return
        Optional
          .ofNullable(response).map(ResponseBody::byteStream)
          .flatMap((Function<InputStream, Optional<ObjectInput>>) stream -> {
            try {
              return Optional.of(new ObjectInputStream(stream));
            } catch (IOException exception) {return Optional.empty();}
          })
          .flatMap(input -> {
            try {return Optional.of(unpack(input));}
            catch (IOException exception) {return Optional.empty();}
          });
    } catch (CompletionException completion) {
      if (completion.getCause() instanceof IOException)
        return Optional.empty(); throw completion;//
    }
  }
}

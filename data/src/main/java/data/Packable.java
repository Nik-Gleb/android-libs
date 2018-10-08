package data;

import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

import static java.util.Objects.requireNonNull;
import static okhttp3.MediaType.parse;

/**
 * Persistable object.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 01/10/2018
 */
@SuppressWarnings("unused")
@Keep
@KeepPublicProtectedClassMembers
@FunctionalInterface
public interface Packable {

  /** Media types */
  @NonNull MediaType MEDIA_TYPE = requireNonNull
    (parse("application/java-serialized-object"));


  @NonNull CompletableFuture<Predicate<ObjectOutput>> pack
    (@NonNull JSONObject json, @NonNull Executor executor);

  /**
   * @param source      data source
   * @param resource  data resource
   * @param json      source json
   */
  default CompletableFuture<Void> save
  (@NonNull DataSource source, @NonNull DataResource resource, @NonNull JSONObject json) {
    return pack(json, DataSource.IO)
      .thenApply((Function<Predicate<ObjectOutput>, RequestBody>) predicate ->
        new RequestBody() {
        @Override public MediaType contentType() {return MEDIA_TYPE;}
        @Override public void writeTo(@NonNull BufferedSink sink) throws IOException
        {predicate.test(new ObjectOutputStream(sink.outputStream()));}
      }).thenAcceptAsync(request -> resource.write(source, request), DataSource.IO);
  }

}

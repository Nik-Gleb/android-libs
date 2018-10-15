/*
 * DataUtils.java
 * data
 *
 * Copyright (C) 2018, Gleb Nikitenko. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import proguard.annotation.Keep;
import proguard.annotation.KeepPublicProtectedClassMembers;

/**
 * Common Tools
 *
 * @author Nikitenko Gleb
 * @since 1.0, 03/07/2018
 */
@Keep
@KeepPublicProtectedClassMembers
@SuppressWarnings({ "WeakerAccess", "unused" })
public final class DataUtils {

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private DataUtils() {throw new AssertionError();}

  /**
   * @param value long value
   *
   * @return bytes equivalent
   */
  @NonNull public static byte[] bytes(long value) {
    return new byte[] {
      (byte) value,
      (byte) (value >> 8),
      (byte) (value >> 16),
      (byte) (value >> 24),
      (byte) (value >> 32),
      (byte) (value >> 40),
      (byte) (value >> 48),
      (byte) (value >> 56)};
  }

  /**
   * @param bytes byte array
   *
   * @return long value
   */
  public static long toLong(@NonNull byte[] bytes) {
    return ((long) bytes[7] << 56)
      | ((long) bytes[6] & 0xff) << 48
      | ((long) bytes[5] & 0xff) << 40
      | ((long) bytes[4] & 0xff) << 32
      | ((long) bytes[3] & 0xff) << 24
      | ((long) bytes[2] & 0xff) << 16
      | ((long) bytes[1] & 0xff) << 8
      | ((long) bytes[0] & 0xff);
  }

  /**
   * @param value primitive value
   *
   * @return byte array
   */
  @NonNull public static byte[] bytes(int value) {
    return new byte[] {
      (byte)(value >>> 24),
      (byte)(value >>> 16),
      (byte)(value >>> 8),
      (byte)value
    };
  }

  /**
   * @param bytes byte array
   *
   * @return byte array
   */
  @SuppressWarnings("PointlessBitwiseExpression")
  public static int toInt(@NonNull byte[] bytes) {
    return
      (bytes[0] & 0xFF) << 24 |
        (bytes[1] & 0xFF) << 16 |
        (bytes[2] & 0xFF) <<  8 |
        (bytes[3] & 0xFF) <<  0 ;
  }

  /**
   * @param value primitive value
   *
   * @return byte array
   */
  @NonNull public static byte[] bytes(@NonNull String value)
  {return value.getBytes(StandardCharsets.UTF_8);}

  /**
   * @param bytes byte array
   *
   * @return string
   */
  @NonNull public static String toStr(@NonNull byte[] bytes)
  {return new String(bytes, StandardCharsets.UTF_8);}

  /**
   * @param response source response
   * @return result request
   */
  @NonNull public static RequestBody
  responseToRequest(@NonNull ResponseBody response) {
    return new RequestBody() {
      @Nullable @Override public final MediaType contentType()
      {return response.contentType();}
      @Override public final long contentLength()
      {return response.contentLength();}
      @Override public void writeTo(@NonNull BufferedSink sink) throws IOException
      {sink.writeAll(response.source());}
    };
  }

  /**
   * @param response response body
   * @return string content
   */
  @NonNull public static String toString
  (@NonNull ResponseBody response) {
    try {return response.string();}
    catch (IOException exception)
    {throw new CompletionException(exception);}
  }

  /**
   * @param response response body
   * @return json object content
   */
  @NonNull public static JSONObject toJSONObject
  (@NonNull ResponseBody response) {
    try {return new JSONObject(response.string());}
    catch (JSONException | IOException exception)
    {throw new CompletionException(exception);}
  }

  /**
   * @param response response body
   * @return json array content
   */
  @NonNull public static JSONArray toJSONArray
  (@NonNull ResponseBody response) {
    try {return new JSONArray(response.string());}
    catch (JSONException | IOException exception)
    {throw new CompletionException(exception);}
  }
}

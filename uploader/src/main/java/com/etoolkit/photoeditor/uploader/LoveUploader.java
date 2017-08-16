/*
 * 	LoveUploader.java
 * 	uploader
 *
 * 	Copyright (C) 2017, E-Toolkit ltd. All Rights Reserved.
 *
 * 	NOTICE:  All information contained herein is, and remains the
 * 	property of E-Toolkit limited and its SUPPLIERS, if any.
 *
 * 	The intellectual and technical concepts contained herein are
 * 	proprietary to E-Toolkit limited and its suppliers and
 * 	may be covered by United States and Foreign Patents, patents
 * 	in process, and are protected by trade secret or copyright law.
 *
 * 	Dissemination of this information or reproduction of this material
 * 	is strictly forbidden unless prior written permission is obtained
 * 	from E-Toolkit limited.
 */

package com.etoolkit.photoeditor.uploader;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;

/**
 * Photo crypt and upload.
 *
 * @author AlexS.
 * @since 1.0, 23.01.17
 */
public final class LoveUploader {

  /** The format of image compression. */
  private static final Bitmap.CompressFormat COMPRESS_FORMAT =
      Bitmap.CompressFormat.JPEG;
  /** The compress quality. */
  private static final int COMPRESS_QUALITY = 80;

  /** The name of native lib. */
  private static final String LIB_NAME = "lovehash-lib";
  /* Load native lib. */
  static {System.loadLibrary(LIB_NAME);}

  /** The connection timeout. */
  private static final int CONNECT_TIMEOUT = 10;
  /** The write timeout. */
  private static final int WRITE_TIMEOUT = 30;
  /** The read timeout. */
  private static final int READ_TIMEOUT = 30;

  /** The ok http client instance. */
  private static final OkHttpClient OK_HTTP_CLIENT =
      new OkHttpClient.Builder()
          .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
          .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
          .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
          .cache(null).build();

  /**
   * The caller should be prevented from constructing objects of this class.
   * Also, this prevents even the native class from calling this constructor.
   **/
  private LoveUploader() {throw new AssertionError();}

  /**
   * Upload bitmap to backend.
   *
   * @param bitmap the source bitmap
   * @param filterName the name of filter
   * @param url the base url
   *
   * @throws IOException when something wrong
   */
  public static String upload(Bitmap bitmap, String filterName, String url)
      throws IOException {

    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(COMPRESS_FORMAT, COMPRESS_QUALITY, stream);
    final byte[] byteArray = stream.toByteArray();
    Util.closeQuietly(stream);

    final String theHash =
        Long.toString(Integer.reverseBytes
            ((int)getShortHash(byteArray, byteArray.length)));

    final RequestBody requestBody = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addPart(
            Headers.of (
                "Content-Disposition",
                "form-data; name=\"image\"; filename=\"images.jpg\""
            ),
            RequestBody.create(MediaType.parse("image/jpeg"), byteArray))
        .addFormDataPart("mode", "json")
        .addFormDataPart("hash", theHash)
        .addFormDataPart("filter", filterName)
        .addFormDataPart("submit", "Upload Image")
        .build();

    final ResponseBody responseBody = OK_HTTP_CLIENT
            .newCall(new Request.Builder().url(url).post(requestBody).build())
            .execute().body();
    if (responseBody == null) throw new IOException("Null body!");

    try {return responseBody.toString();}
    finally {Util.closeQuietly(responseBody);}
  }

  /**
   * Hash calculation.
   *
   * @param data the array of bytes
   * @param length length of array
   * @return the hash
   */
  private static native long getShortHash(byte[] data, int length);
}

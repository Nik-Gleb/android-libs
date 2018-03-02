/*
 * HeadersInterceptor.java
 * ok-tools
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

package ok;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Interceptor;
import okhttp3.Response;

import static java.util.Locale.US;

/**
 * Common headers interceptor.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 13/06/2017
 */
@SuppressWarnings("unused")
public final class HeadersInterceptor implements Interceptor {

  /** The http header Accept-Language. */
  private static final String HTTP_HEADER_ACCEPT_LANGUAGE = "Accept-Language";
  /** The http header Host. */
  private static final String HTTP_HEADER_HOST = "Host";
  /** The http header User-Agent. */
  private static final String HTTP_HEADER_USER_AGENT = "User-Agent";
  /** The http header Date. */
  private static final String HTTP_HEADER_DATE = "Date";

  /** Date format index */
  private static final int DATE_FORMAT_IDX = 0;

  /** The date formats. */
  private static final String[] DATE_FORMATS = new String[] {
      "EEE, dd MMM yyyy HH:mm:ss zzz",
      "EEE, dd-MMM-yy HH:mm:ss zzz",
      "EEE MMM dd HH:mm:ss yyyy"
  };

  /** The GMT Time Zone. */
  private static final TimeZone TIME_ZONE_GMT = TimeZone.getTimeZone("GMT");

  /** The date format. */
  private static final SimpleDateFormat DATE_FORMAT = createDateFormat();


  /** {@inheritDoc} */
  @Override
  public final Response intercept(Chain chain) throws IOException {
    if (chain == null)
      throw new NullPointerException("chain == null");

    return chain.proceed (
        chain.request().newBuilder()
            .header(HTTP_HEADER_ACCEPT_LANGUAGE, getLanguage())
            .header(HTTP_HEADER_USER_AGENT, getUserAgent())
            .header(HTTP_HEADER_HOST, chain.request().url().host())
            .header(HTTP_HEADER_DATE, getDate())
            .build()
    );
  }

  /** @return formatted current date */
  private static String getDate() {
    return DATE_FORMAT.format(new Date(System.currentTimeMillis()));
  }

  /** @return current language */
  private static String getLanguage() {
    return Locale.getDefault().getLanguage();
  }

  /** @return common date format */
  private static SimpleDateFormat createDateFormat() {
    final SimpleDateFormat result = new SimpleDateFormat(DATE_FORMATS[DATE_FORMAT_IDX], US);
    result.setTimeZone(TIME_ZONE_GMT); return result;
  }

  /** @return User-Agent header for this http client. */
  private static String getUserAgent() {
    final String agent = System.getProperty("http.agent");
    return agent != null ? agent : ("Java" + System.getProperty("java.version"));
  }
}
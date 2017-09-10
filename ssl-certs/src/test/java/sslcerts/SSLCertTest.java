/*
 * SSLCertTest.java
 * ssl-certs
 *
 * Copyright (C) 2017, Gleb Nikitenko. All Rights Reserved.
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

package sslcerts;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import static sslcerts.SSLSocketSettings.KEYSTORE_TYPE_JKS;
import static sslcerts.SSLSocketSettings.PROTOCOL_SSL;
import static sslcerts.SSLSocketSettings.openKeyStore;

/**
 * @author Nikitenko Gleb
 * @since 1.0, 09/08/2017
 */
@SuppressWarnings("WeakerAccess")
@RunWith(MockitoJUnitRunner.class)
public class SSLCertTest {

  /** Test url-path */
  private static final String TEST_PATH = "https://certs.cac.washington.edu/CAtest/";

  /** Keystore password. */
  private static final String PASSWORD = "4NxUujMz";

  /** Key store file name.*/
  private static final String FILE_NAME =
      SSLCertTest.class.getResource("keystore.jks").getFile();

  /** @throws Exception by some issues */
  @Before
  public final void setUp() throws Exception {}

  /** @throws Exception by some issues */
  @After
  public final void tearDown() throws Exception {}

  /** @throws Exception by some issues */
  @Test
  public final void mainTest() throws Exception {
    try {testMain();/*fail("Exception not was thrown!");*/}
    catch (SSLHandshakeException ignored) {}

    final InputStream keyStoreSrc = new FileInputStream(FILE_NAME);
    final KeyStore keyStore = openKeyStore(PASSWORD, KEYSTORE_TYPE_JKS, keyStoreSrc);
    keyStoreSrc.close();

    final SSLSocketSettings sslSocketSettings =
        SSLSocketSettings.create(PROTOCOL_SSL, keyStore);

    if (sslSocketSettings == null) throw new RuntimeException("sslSocketSettings == null");
    HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketSettings.sslSocketFactory);

    testMain();
  }

  /** Test connection. */
  private static void testMain() throws Exception {
    final InputStream is = new URL(TEST_PATH).openStream();
    copyStreams(is, System.out); is.close();
  }

  /**
   * Copy streams.
   *
   * @param in input stream
   * @param out output stream
   *
   * @throws IOException the I/O Exception
   */
  @SuppressWarnings("SameParameterValue")
  private static void copyStreams(InputStream in, OutputStream out)
      throws IOException {
    final byte[] buffer = new byte[512]; int len;
    while ((len = in.read(buffer)) != -1)
      out.write(buffer, 0, len);
  }
}
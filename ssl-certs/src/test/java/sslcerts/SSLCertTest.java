/*
 * 	CleanTest.java
 * 	clean
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
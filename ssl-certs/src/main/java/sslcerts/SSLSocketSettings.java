/*
 * SSLSocketSettings.java
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

import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * The SSL-Socket Settings.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 13/06/2017
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SSLSocketSettings {

  /** SSL-Protocols. */
  public static final String
      PROTOCOL_DEFAULT    = "Default",
      PROTOCOL_SSL        = "SSL",
      PROTOCOL_SSL_V3     = "SSLv3",
      PROTOCOL_TLS        = "TLS",
      PROTOCOL_TLS_V1     = "TLSv1",
      PROTOCOL_TLS_V11    = "TLSv1.1",
      PROTOCOL_TLS_V12    = "TLSv1.2";

  /** KeyStore Types. */
  public static final String
      KEYSTORE_TYPE_BKS = "bks",    // Java Key Store
      KEYSTORE_TYPE_JKS = "jks",    // Java Key Store
      KEYSTORE_TYPE_JCEKS = "jceks",  //JCE Key Store(Java Cryptography Extension KeyStore)
      KEYSTORE_TYPE_PKCS12 = "pkcs12", //Standard keystore type which can be used in Java and other languages.
      KEYSTORE_TYPE_DKS = "dks";    //Domain KeyStore is a keystore of keystore


  /** The SSL_Socket factory. */
  public final SSLSocketFactory sslSocketFactory;
  /** The thrust manager. */
  public final X509TrustManager x509TrustManager;

  /**
   * Constructs a new {@link SSLSocketSettings} with socket-factory and thrust-manager.
   *
   * @param factory the SSL-Socket Factory
   * @param manager the X509 Thrust Manager
   */
  private SSLSocketSettings(SSLSocketFactory factory, X509TrustManager manager)
  {sslSocketFactory = factory; x509TrustManager = manager;}

  /**
   * Constructs a new {@link SSLSocketSettings} by ssl-protocol and keystore
   * .
   * @param sslProtocol the necessary ssl-protocol
   *                    ("Default", "SSL", "SSLv3", "TLS", "TLSv1", "TLSv1.1", "TLSv1.2")
   * @param keyStore the keystore instance
   *
   * @return the ssl-socket settings
   */
  public static SSLSocketSettings create(String sslProtocol, KeyStore keyStore) {
    if (sslProtocol == null) throw new NullPointerException("sockets == null");
    if (keyStore == null) throw new NullPointerException("keyStore == null");

    final TrustManagerFactory trustManagerFactory = getTrustManagerFactory(keyStore);
    if (trustManagerFactory == null) return null;
    final SSLContext sslContext = getSslContext(trustManagerFactory, sslProtocol);
    if (sslContext == null) return null;
    final X509TrustManager x509TrustManager =
        getFirstX509TrustManager (trustManagerFactory.getTrustManagers());
    if (x509TrustManager == null) return null;

    return new SSLSocketSettings(sslContext.getSocketFactory(), x509TrustManager);
  }

  /**
   * Open KeyStore.
   *
   * @param pass the password of keystore
   * @param type the type of key store
   * @param stream the source key store stream
   *
   * @return keystore instance
   */
  public static KeyStore openKeyStore(String pass, String type, InputStream stream) {

    if (pass == null) throw new NullPointerException("pass == null");
    if (type == null) throw new NullPointerException("type == null");
    if (stream == null) throw new NullPointerException("stream == null");

    try {
      final KeyStore keyStore = KeyStore.getInstance(type);
      keyStore.load(stream, pass.toCharArray());
      return keyStore;
    } catch (Exception exception) {
      return null;
    }
  }


  /**
   * @param keyStore source keystore
   * @return trust manager factory
   */
  private static TrustManagerFactory getTrustManagerFactory(KeyStore keyStore) {
    try {
      final TrustManagerFactory result = TrustManagerFactory.getInstance
          (TrustManagerFactory.getDefaultAlgorithm());
      result.init(keyStore);
      return result;
    } catch (GeneralSecurityException exception) {return null;}
  }

  /**
   * @param factory trust manager factory
   * @return ssl context
   */
  private static SSLContext getSslContext(TrustManagerFactory factory, String protocol) {
    try {
      final SSLContext result = SSLContext.getInstance(protocol);
      result.init(null, factory.getTrustManagers(), new SecureRandom());
      return result;
    } catch (GeneralSecurityException exception) {return null;}
  }

  /**
   * @param trustManagers array of trust managers
   * @return first x509 trust manager
   */
  private static X509TrustManager getFirstX509TrustManager(TrustManager[] trustManagers) {
    for (TrustManager trustManager : trustManagers)
      if (trustManager instanceof X509TrustManager)
        return (X509TrustManager) trustManager;
    return null;
  }
}
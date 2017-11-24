/*
 * NetworkContracts.java
 * network
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

package network;

/**
 * Network Contracts.
 *
 * @author Nikitenko Gleb
 * @since 1.0, 09/09/2017
 */
@SuppressWarnings("unused")
final class NetworkContracts {

  /** The "headers" bundle parameter. */
  static final String BUNDLE_HEADERS = "headers";
  /** The "form-body" bundle parameter. */
  static final String BUNDLE_FORM_BODY = "form_body";
  /** The "multipart-body" bundle parameter. */
  static final String BUNDLE_MULTIPART_BODY = "multipart_body";
  /** The "request-body" bundle parameter. */
  static final String BUNDLE_REQUEST_BODY = "request_body";
  /** The "media-type" bundle parameter. */
  static final String BUNDLE_MEDIA_TYPE = "media_type";
  /** The "string-body" bundle parameter. */
  static final String BUNDLE_STRING_BODY = "string_body";
  /** The "file-body" bundle parameter. */
  static final String BUNDLE_FILE_BODY = "file_body";
  /** The "body-length" bundle parameter. */
  static final String BUNDLE_BODY_LENGTH = "body_length";
}

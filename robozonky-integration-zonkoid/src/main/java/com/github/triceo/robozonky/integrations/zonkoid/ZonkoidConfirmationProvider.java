/*
 * Copyright 2017 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.triceo.robozonky.integrations.zonkoid;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If delegated, this provider will allow investors to fill out CAPTCHA.
 */
public class ZonkoidConfirmationProvider implements ConfirmationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkoidConfirmationProvider.class);
    private static final String PROTOCOL_MAIN = "https", PROTOCOL_FALLBACK = "http", CLIENT_APP = "ROBOZONKY";
    static final String PATH = "/zonkycommander/rest/notifications";

    static String md5(final String secret) throws NoSuchAlgorithmException {
        final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
        mdEnc.update(secret.getBytes(Defaults.CHARSET));
        return new BigInteger(1, mdEnc.digest()).toString(16);
    }

    static String getAuthenticationString(final RequestId requestId, final int loanId) {
        final String auth = new StringJoiner("|")
                .add(String.valueOf(requestId.getPassword()))
                .add(ZonkoidConfirmationProvider.CLIENT_APP)
                .add(requestId.getUserId())
                .add(String.valueOf(loanId))
                .toString();
        try {
            return ZonkoidConfirmationProvider.md5(auth);
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Your Java Runtime Environment does not support MD5!", ex);
        }
    }

    private static HttpEntity getFormData(final RequestId requestId, final int loanId, final int amount)
            throws UnsupportedEncodingException {
        final List<NameValuePair> nvps = Arrays.asList(
                new BasicNameValuePair("clientApp", ZonkoidConfirmationProvider.CLIENT_APP),
                new BasicNameValuePair("username", requestId.getUserId()),
                new BasicNameValuePair("loanId", String.valueOf(loanId)),
                new BasicNameValuePair("preferredAmount", String.valueOf(amount))
        );
        return new UrlEncodedFormEntity(nvps);
    }

    static HttpPost getRequest(final RequestId requestId, final int loanId, final int amount, final String protocol,
                               final String domain) throws UnsupportedEncodingException {
        final String auth = ZonkoidConfirmationProvider.getAuthenticationString(requestId, loanId);
        final HttpPost httpPost = new HttpPost(protocol + "://" + domain + ZonkoidConfirmationProvider.PATH);
        httpPost.addHeader("Accept", "text/plain");
        httpPost.addHeader("Authorization", auth);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.addHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT);
        httpPost.setEntity(ZonkoidConfirmationProvider.getFormData(requestId, loanId, amount));
        return httpPost;
    }

    static boolean handleError(final RequestId requestId, final int loanId, final int amount, final String domain,
                               final String protocol, final Exception ex) {
        switch (protocol) {
            case ZonkoidConfirmationProvider.PROTOCOL_MAIN:
                ZonkoidConfirmationProvider.LOGGER.warn("HTTPS communication with Zonkoid failed, trying HTTP.");
                return ZonkoidConfirmationProvider.requestConfirmation(requestId, loanId, amount, domain,
                                                                       ZonkoidConfirmationProvider.PROTOCOL_FALLBACK);
            case ZonkoidConfirmationProvider.PROTOCOL_FALLBACK:
                ZonkoidConfirmationProvider.LOGGER.error("Communication with Zonkoid failed.", ex);
                return false;
            default:
                throw new IllegalStateException("Can not happen.");
        }
    }

    private static boolean requestConfirmation(final RequestId requestId, final int loanId, final int amount,
                                               final String domain, final String protocol) {
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ZonkoidConfirmationProvider.LOGGER.trace("Sending request.");
            final HttpPost post = ZonkoidConfirmationProvider.getRequest(requestId, loanId, amount, protocol, domain);
            try (final CloseableHttpResponse response = httpclient.execute(post)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    ZonkoidConfirmationProvider.LOGGER.debug("Response: {}", response.getStatusLine());
                    return true;
                } else {
                    ZonkoidConfirmationProvider.LOGGER.error("Unknown response: {}", response.getStatusLine());
                    return false;
                }
            }
        } catch (final Exception ex) {
            return ZonkoidConfirmationProvider.handleError(requestId, loanId, amount, domain, protocol, ex);
        }
    }

    static boolean requestConfirmation(final RequestId requestId, final int loanId, final int amount,
                                       final String domain) {
        return ZonkoidConfirmationProvider.requestConfirmation(requestId, loanId, amount, domain,
                                                               ZonkoidConfirmationProvider.PROTOCOL_MAIN);
    }

    @Override
    public boolean requestConfirmation(final RequestId requestId, final int loanId, final int amount) {
        return ZonkoidConfirmationProvider.requestConfirmation(requestId, loanId, amount, "urbancoders.eu");
    }

    @Override
    public String getId() {
        return "Zonkoid / Zonkios";
    }
}

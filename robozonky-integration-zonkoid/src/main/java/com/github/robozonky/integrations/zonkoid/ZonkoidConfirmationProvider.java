/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.integrations.zonkoid;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.internal.api.Defaults;
import io.vavr.control.Try;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * If delegated, this provider will allow investors to fill out CAPTCHA.
 */
public class ZonkoidConfirmationProvider implements ConfirmationProvider {

    static final String PATH = "/zonkycommander/rest/notifications";
    private static final Logger LOGGER = LogManager.getLogger(ZonkoidConfirmationProvider.class);
    private static final String PROTOCOL_MAIN = "https", PROTOCOL_FALLBACK = "http", CLIENT_APP = "ROBOZONKY";

    private final String rootUrl;

    public ZonkoidConfirmationProvider() {
        this("urbancoders.eu");
    }

    ZonkoidConfirmationProvider(final String rootUrl) {
        this.rootUrl = rootUrl;
    }

    static String md5(final String secret) throws NoSuchAlgorithmException {
        final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
        mdEnc.update(secret.getBytes(Defaults.CHARSET));
        return new BigInteger(1, mdEnc.digest()).toString(16);
    }

    static String getAuthenticationString(final RequestId requestId, final int loanId) {
        final String auth = new StringJoiner("|")
                .add(String.valueOf(requestId.getPassword()))
                .add(CLIENT_APP)
                .add(requestId.getUserId())
                .add(String.valueOf(loanId))
                .toString();
        try {
            return md5(auth);
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Your Java Runtime Environment does not support MD5!", ex);
        }
    }

    private static HttpEntity getFormData(final RequestId requestId, final int loanId, final int amount)
            throws UnsupportedEncodingException {
        final List<NameValuePair> nvps = Arrays.asList(
                new BasicNameValuePair("clientApp", CLIENT_APP),
                new BasicNameValuePair("username", requestId.getUserId()),
                new BasicNameValuePair("loanId", String.valueOf(loanId)),
                new BasicNameValuePair("preferredAmount", String.valueOf(amount))
        );
        return new UrlEncodedFormEntity(nvps);
    }

    static HttpPost getRequest(final RequestId requestId, final int loanId, final int amount, final String protocol,
                               final String rootUrl) throws UnsupportedEncodingException {
        final String auth = getAuthenticationString(requestId, loanId);
        final HttpPost httpPost = new HttpPost(protocol + "://" + rootUrl + PATH);
        httpPost.addHeader("Accept", "text/plain");
        httpPost.addHeader("Authorization", auth);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.addHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT);
        httpPost.setEntity(getFormData(requestId, loanId, amount));
        return httpPost;
    }

    static boolean handleError(final RequestId requestId, final int loanId, final int amount, final String domain,
                               final String protocol, final Throwable t) {
        switch (protocol) {
            case PROTOCOL_MAIN:
                LOGGER.warn("HTTPS communication with Zonkoid failed, trying HTTP.");
                return requestConfirmation(requestId, loanId, amount, domain, PROTOCOL_FALLBACK);
            case PROTOCOL_FALLBACK:
                LOGGER.info("Communication with Zonkoid failed.", t);
                return false;
            default:
                throw new IllegalStateException("Can not happen.");
        }
    }

    private static boolean requestConfirmation(final RequestId requestId, final int loanId, final int amount,
                                               final String rootUrl, final String protocol) {
        return Try.withResources(HttpClients::createDefault)
                .of(httpClient -> {
                    LOGGER.debug("Requesting notification of {} CZK for loan #{}.", amount, loanId);
                    final HttpPost post = getRequest(requestId, loanId, amount, protocol, rootUrl);
                    return httpClient.execute(post, ZonkoidConfirmationProvider::respond);
                })
                .getOrElseGet(t -> handleError(requestId, loanId, amount, rootUrl, protocol, t));
    }

    private static boolean respond(final HttpResponse response) {
        final String body = Util.readEntity(response.getEntity());
        LOGGER.info("Response: '{}' (Body: '{}')", response.getStatusLine(), body);
        return Util.isHttpSuccess(response.getStatusLine().getStatusCode());
    }

    @Override
    public boolean requestConfirmation(final RequestId requestId, final int loanId, final int amount) {
        return requestConfirmation(requestId, loanId, amount, rootUrl, PROTOCOL_MAIN);
    }

    @Override
    public String getId() {
        return "Zonkoid / Zonkios";
    }
}

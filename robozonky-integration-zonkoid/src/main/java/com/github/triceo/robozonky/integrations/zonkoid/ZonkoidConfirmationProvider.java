/*
 * Copyright 2016 Lukáš Petrovický
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
import java.util.Optional;
import java.util.StringJoiner;

import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.confirmations.Confirmation;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.ConfirmationType;
import com.github.triceo.robozonky.api.confirmations.RequestId;
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
 * This provider will never return {@link ConfirmationType#APPROVED}. If delegated, this provider will allow
 * investors to fill out CAPTCHA.
 */
public class ZonkoidConfirmationProvider implements ConfirmationProvider {

    private static final String CLIENT_APP = "ROBOZONKY";
    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkoidConfirmationProvider.class);
    static final String PATH = "/zonkycommander/rest/notifications";

    private static String md5(final String secret) throws NoSuchAlgorithmException {
        final MessageDigest mdEnc = MessageDigest.getInstance("MD5");
        mdEnc.update(secret.getBytes(Defaults.CHARSET), 0, secret.length());
        return new BigInteger(1, mdEnc.digest()).toString(16);
    }

    private static String getAuthenticationString(final RequestId requestId, final int loanId) {
        try {
            final String auth = new StringJoiner("|")
                    .add(String.valueOf(requestId.getPassword()))
                    .add(ZonkoidConfirmationProvider.CLIENT_APP)
                    .add(requestId.getUserId())
                    .add(String.valueOf(loanId))
                    .toString();
            return ZonkoidConfirmationProvider.md5(auth);
        } catch (final NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Your Java Runtime Environment does not support MD5!", ex);
        }
    }

    private static HttpEntity getFormData(final RequestId requestId, final int loanId, final int amount) throws
            UnsupportedEncodingException {
        final List<NameValuePair> nvps = Arrays.asList(
            new BasicNameValuePair("clientApp", ZonkoidConfirmationProvider.CLIENT_APP),
            new BasicNameValuePair("username", requestId.getUserId()),
            new BasicNameValuePair("loanId", String.valueOf(loanId)),
            new BasicNameValuePair("preferredAmount", String.valueOf(amount))
        );
        return new UrlEncodedFormEntity(nvps);
    }

    static Optional<Confirmation> requestConfirmation(final RequestId requestId, final int loanId, final int amount,
                                                      final String domain) {
        final String auth = ZonkoidConfirmationProvider.getAuthenticationString(requestId, loanId);
        ZonkoidConfirmationProvider.LOGGER.trace("Opening Zonkoid connection.");
        try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
            final HttpPost httpPost = new HttpPost(domain + ZonkoidConfirmationProvider.PATH);
            httpPost.addHeader("Accept", "text/plain");
            httpPost.addHeader("Authorization", auth);
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.addHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT);
            httpPost.setEntity(ZonkoidConfirmationProvider.getFormData(requestId, loanId, amount));
            ZonkoidConfirmationProvider.LOGGER.trace("Opening Zonkoid request.");
            try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                final int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    ZonkoidConfirmationProvider.LOGGER.debug("Zonkoid response: {}", response.getStatusLine());
                    return Optional.of(new Confirmation(ConfirmationType.DELEGATED));
                } else if (statusCode == 400 || statusCode == 403){
                    ZonkoidConfirmationProvider.LOGGER.warn("Zonkoid response: {}", response.getStatusLine());
                    return Optional.of(new Confirmation(ConfirmationType.REJECTED));
                } else {
                    ZonkoidConfirmationProvider.LOGGER.error("Unknown Zonkoid response: {}", response.getStatusLine());
                    return Optional.empty();
                }
            } finally {
                ZonkoidConfirmationProvider.LOGGER.trace("Closing Zonkoid request.");
            }
        } catch (final Exception ex) {
            ZonkoidConfirmationProvider.LOGGER.warn("Communication with Zonkoid failed.", ex);
            return Optional.empty();
        } finally {
            ZonkoidConfirmationProvider.LOGGER.trace("Closing Zonkoid connection.");
        }
    }

    @Override
    public Optional<Confirmation> requestConfirmation(final RequestId requestId, final int loanId, final int amount) {
        return requestConfirmation(requestId, loanId, amount, "https://urbancoders.eu");
    }

}

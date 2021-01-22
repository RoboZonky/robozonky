/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.Settings;

/**
 * Decorates the request with User-Agent and adds some simple request logging.
 * If ever a filter is needed for JAX-RS communication, this class should serve as the base class for that filter.
 * This class is not thread-safe. Consider using {@link ThreadLocal}.
 */
class RoboZonkyFilter implements ClientRequestFilter,
        ClientResponseFilter {

    private static final String REQUEST_ID_PROPERTY = "robozonkyRequestId";

    // not static, so that filters extending this one get the proper logger class
    protected final Logger logger = LogManager.getLogger(this.getClass());
    // In case the same filter is used multiple times, we add support for timing multiple requests.
    private final Map<String, Long> requestStartNanotimes = new HashMap<>(1);
    private final Map<String, Object[]> queryParams = new TreeMap<>();
    private final Map<String, String> requestHeaders = new TreeMap<>();
    private Map<String, String> responseHeaders = Collections.emptyMap();

    public RoboZonkyFilter() {
        this.setRequestHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT);
    }

    private static boolean shouldLogEntity(final ClientResponseContext responseCtx) {
        if (!responseCtx.hasEntity()) {
            return false;
        } else if (responseCtx.getStatus() < 400) {
            return Settings.INSTANCE.isDebugHttpResponseLoggingEnabled();
        } else {
            return true;
        }
    }

    static URI addQueryParams(final URI info, final Map<String, Object[]> params) {
        final UriBuilder builder = UriBuilder.fromUri(info);
        builder.uri(info);
        params.forEach(builder::queryParam);
        return builder.build();
    }

    public void setQueryParam(final String key, final Object... values) {
        queryParams.put(key, values);
    }

    public void setRequestHeader(final String key, final String value) {
        requestHeaders.put(key, value);
    }

    public Optional<String> getLastResponseHeader(final String key) {
        return Optional.ofNullable(responseHeaders.get(key));
    }

    private URI rebuild(final URI info) {
        return addQueryParams(info, queryParams);
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) {
        requestHeaders.forEach((k, v) -> clientRequestContext.getHeaders()
            .putSingle(k, v));
        clientRequestContext.setUri(rebuild(clientRequestContext.getUri()));
        var requestId = UUID.randomUUID()
            .toString();
        clientRequestContext.setProperty(REQUEST_ID_PROPERTY, requestId);
        requestStartNanotimes.put(requestId, System.nanoTime());
        logger.trace("Request {} {}.", clientRequestContext.getMethod(), clientRequestContext.getUri());
    }

    private String getResponseEntity(final ClientResponseContext clientResponseContext) throws IOException {
        if (shouldLogEntity(clientResponseContext)) {
            final InterceptingInputStream s = new InterceptingInputStream(clientResponseContext.getEntityStream());
            clientResponseContext.setEntityStream(s);
            logger.debug("Response body is: {}", s.getContents());
            return s.getContents();
        } else {
            return "";
        }
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext,
            final ClientResponseContext clientResponseContext) throws IOException {
        var currentNanotime = System.nanoTime();
        var requestId = (String) clientRequestContext.getProperty(REQUEST_ID_PROPERTY);
        if (requestId == null) { // Happens during testing, where a lot of things are mocked.
            logger.debug("HTTP {} Response from {}: {} {}.", clientRequestContext.getMethod(),
                    clientRequestContext.getUri(), clientResponseContext.getStatus(),
                    clientResponseContext.getStatusInfo()
                        .getReasonPhrase());
        } else {
            var startNanotime = requestStartNanotimes.remove(requestId);
            var millisSpent = (currentNanotime - startNanotime) / 1000 / 1000;
            logger.debug("HTTP {} Response from {}: {} {} ({} ms).", clientRequestContext.getMethod(),
                    clientRequestContext.getUri(), clientResponseContext.getStatus(),
                    clientResponseContext.getStatusInfo()
                        .getReasonPhrase(),
                    millisSpent);
        }
        var responseEntity = getResponseEntity(clientResponseContext);
        if (clientResponseContext.getStatus() == 400 && responseEntity.contains("invalid_token")) {
            // Zonky is dumb and throws 400 when it should throw 401
            clientResponseContext.setStatus(401);
        }
        responseHeaders = clientResponseContext.getHeaders()
            .entrySet()
            .stream()
            .filter(e -> !e.getValue()
                .isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                .get(0), (a, b) -> a, TreeMap::new));
    }
}

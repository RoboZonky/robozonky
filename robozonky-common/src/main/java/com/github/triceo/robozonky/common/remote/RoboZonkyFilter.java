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

package com.github.triceo.robozonky.common.remote;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorates the request with User-Agent and adds some simple request logging.
 *
 * If ever a filter is needed for JAX-RS communication, this class should serve as the base class for that filter.
 */
public class RoboZonkyFilter implements ClientRequestFilter, ClientResponseFilter {

    // not static, so that filters extending this one get the proper logger class
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Map<String, String> headersToSet = new LinkedHashMap<>();
    private Map<String, String> responseHeaders = new LinkedHashMap<>(0);

    public RoboZonkyFilter() {
        this.setRequestHeader("User-Agent", Defaults.ROBOZONKY_USER_AGENT);
    }

    public void setRequestHeader(final String key, final String value) {
        headersToSet.put(key, value);
    }

    public Optional<String> getLastResponseHeader(final String key) {
        return Optional.ofNullable(responseHeaders.get(key));
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) throws IOException {
        headersToSet.forEach((k, v) -> clientRequestContext.getHeaders().putSingle(k, v));
        this.logger.trace("Request {} {}.", clientRequestContext.getMethod(), clientRequestContext.getUri());
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext,
                       final ClientResponseContext clientResponseContext) throws IOException {
        this.logger.debug("HTTP {} Response from {}: {} {}.", clientRequestContext.getMethod(),
                clientRequestContext.getUri(), clientResponseContext.getStatus(),
                clientResponseContext.getStatusInfo().getReasonPhrase());
        responseHeaders = clientResponseContext.getHeaders().entrySet().stream()
                .filter(e -> e.getValue().size() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }

}

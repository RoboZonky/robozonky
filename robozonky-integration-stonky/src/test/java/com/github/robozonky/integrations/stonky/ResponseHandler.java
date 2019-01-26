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

package com.github.robozonky.integrations.stonky;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ResponseHandler implements BiFunction<String, String, Optional<MockLowLevelHttpResponse>> {

    private static final JacksonFactory FACTORY = new JacksonFactory();
    protected final Logger logger = LogManager.getLogger(this.getClass());

    protected static String toJson(final Object object) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            new JsonHttpContent(FACTORY, object).writeTo(baos);
            return baos.toString();
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected static String toJson(final Collection<?> objects) {
        return objects.stream().map(ResponseHandler::toJson).collect(Collectors.joining(", "));
    }

    abstract protected boolean appliesTo(final String method, final String url);

    abstract protected MockLowLevelHttpResponse respond(final String method, final String url);

    @Override
    public Optional<MockLowLevelHttpResponse> apply(final String method, final String url) {
        if (appliesTo(method, url)) {
            logger.debug("Applies to {} {}.", method, url);
            return Optional.ofNullable(respond(method, url));
        } else {
            logger.debug("Does not apply to {} {}.", method, url);
            return Optional.empty();
        }
    }
}

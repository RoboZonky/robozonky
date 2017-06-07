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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;

/**
 * Represents a close-able RESTEasy client proxy. Users should preferably call {@link #close()} after they're
 * done with the API.
 *
 * @param <T> Type of the API to be handled.
 */
public class Api<T> implements ApiBlueprint<T>, AutoCloseable {

    private final AtomicReference<ResteasyClient> client;
    private final T api;

    public Api(final T api) {
        this(api, null);
    }

    public Api(final T api, final ResteasyClient client) {
        this.client = new AtomicReference<>(client);
        this.api = api;
    }

    @Override
    public <S> S execute(final Function<T, S> function) {
        return function.apply(api);
    }

    boolean isClosed() {
        return client.get() == null;
    }

    @Override
    public void close() {
        if (this.isClosed()) {
            return;
        }
        client.getAndSet(null).close();
    }
}

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

package com.github.robozonky.internal.remote;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

class PaginatedApi<S, T> {

    private static final Logger LOGGER = LogManager.getLogger(PaginatedApi.class);

    private final Class<T> api;
    private final String url;
    private final ResteasyClient client;
    private final Supplier<ZonkyApiToken> tokenSupplier;
    private final RequestCounter counter;

    PaginatedApi(final Class<T> api, final String url, final Supplier<ZonkyApiToken> token,
                 final ResteasyClient client) {
        this(api, url, token, client, null);
    }

    public PaginatedApi(final Class<T> api, final String url, final Supplier<ZonkyApiToken> token,
                        final ResteasyClient client, final RequestCounter counter) {
        this.api = api;
        this.url = url;
        this.client = client;
        this.tokenSupplier = token;
        this.counter = counter;
    }

    /**
     * Filters are for one-time use only. They need to be thrown away after being used, as they could otherwise be used
     * to store and transfer stale state, such as request headers etc. For the same reason, they must not be shared
     * among threads.
     * @return
     */
    private RoboZonkyFilter newFilter() {
        return new AuthenticatedFilter(tokenSupplier);
    }

    public <Q> Q execute(final Function<T, Q> function) {
        return this.execute(function, true);
    }

    public <Q> Q execute(final Function<T, Q> function, final boolean trackRequests) {
        return this.execute(function, new Select(), newFilter(), trackRequests);
    }

    <Q> Q execute(final Function<T, Q> function, final Select select, final RoboZonkyFilter filter) {
        return execute(function, select, filter, true);
    }

    <Q> Q execute(final Function<T, Q> function, final Select select, final RoboZonkyFilter filter,
                  final boolean trackRequests) {
        select.accept(filter);
        return execute(function, filter, trackRequests);
    }

    <Q> Q execute(final Function<T, Q> function, final RoboZonkyFilter filter, final boolean trackRequests) {
        final T proxy = ProxyFactory.newProxy(client, filter, api, url);
        return Api.call(function, proxy, trackRequests ? counter : null);
    }

    public PaginatedResult<S> execute(final Function<T, List<S>> function, final Select select, final int pageNo,
                                      final int pageSize) {
        return this.execute(function, select, pageNo, pageSize, newFilter());
    }

    PaginatedResult<S> execute(final Function<T, List<S>> function, final Select select, final int pageNo,
                               final int pageSize, final RoboZonkyFilter filter) {
        filter.setRequestHeader("X-Page", String.valueOf(pageNo));
        filter.setRequestHeader("X-Size", String.valueOf(pageSize));
        LOGGER.trace("Will request page #{} of size {}.", pageNo, pageSize);
        final List<S> result = this.execute(function, select, filter);
        final int totalSize = filter.getLastResponseHeader("X-Total")
                .map(Integer::parseInt)
                .orElse(0);
        LOGGER.trace("Has {} results in total.", totalSize);
        return new PaginatedResult<>(result, totalSize);
    }
}

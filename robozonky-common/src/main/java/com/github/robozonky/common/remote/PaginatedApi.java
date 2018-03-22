/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.common.remote;

import java.util.List;
import java.util.function.Function;

import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PaginatedApi<S, T extends EntityCollectionApi<S>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaginatedApi.class);

    private final AuthenticatedFilter filter;
    private final Class<T> api;
    private final String url;
    private final ResteasyClient client;

    PaginatedApi(final Class<T> api, final String url, final ZonkyApiToken token, final ResteasyClient client) {
        this.api = api;
        this.url = url;
        this.filter = new AuthenticatedFilter(token);
        this.client = client;
    }

    public <Q> Q execute(final Function<T, Q> function) {
        return this.execute(function, new Select(), Sort.unspecified(), filter);
    }

    <Q> Q execute(final Function<T, Q> function, final Select select, final Sort<S> sort,
                  final RoboZonkyFilter filter) {
        select.accept(filter);
        sort.apply(filter);
        return execute(function, filter);
    }

    <Q> Q execute(final Function<T, Q> function, final RoboZonkyFilter filter) {
        final T proxy = ProxyFactory.newProxy(client, filter, api, url);
        return function.apply(proxy);
    }

    public PaginatedResult<S> execute(final Function<T, List<S>> function, final Select select,
                                      final Sort<S> sort, final int pageNo, final int pageSize) {
        return this.execute(function, select, sort, pageNo, pageSize, filter);
    }

    PaginatedResult<S> execute(final Function<T, List<S>> function, final Select select, final Sort<S> sort,
                               final int pageNo, final int pageSize, final RoboZonkyFilter filter) {
        filter.setRequestHeader("X-Page", String.valueOf(pageNo));
        filter.setRequestHeader("X-Size", String.valueOf(pageSize));
        LOGGER.trace("Will request page #{} of size {}.", pageNo, pageSize);
        final List<S> result = this.execute(function, select, sort, filter);
        final int totalSize = filter.getLastResponseHeader("X-Total")
                .map(Integer::parseInt)
                .orElse(-1);
        LOGGER.trace("Total size of {} reported.", totalSize);
        return new PaginatedResult<>(result, totalSize);
    }

    public PaginatedResult<S> execute(final Function<T, List<S>> function, final int pageNo, final int pageSize) {
        return this.execute(function, new Select(), Sort.unspecified(), pageNo, pageSize, filter);
    }
}

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

import java.util.Collection;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.EntityCollectionApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

class PaginatedApi<S, T extends EntityCollectionApi<S>> implements ApiBlueprint<T> {

    private final ZonkyApiToken token;
    private final Class<T> api;
    private final String url;

    PaginatedApi(final Class<T> api, final String url, final ZonkyApiToken token) {
        this.api = api;
        this.url = url;
        this.token = token;
    }

    @Override
    public <Q> Q execute(final Function<T, Q> function) {
        final AuthenticatedFilter filter = new AuthenticatedFilter(this.token);
        return this.execute(function, Sort.unspecified(), filter);
    }

    <Q> Q execute(final Function<T, Q> function, final Sort<S> sort, final RoboZonkyFilter filter) {
        sort.apply(filter);
        final ResteasyClient client = ProxyFactory.newResteasyClient(filter);
        try {
            final T proxy = ProxyFactory.newProxy(client, api, url);
            return function.apply(proxy);
        } finally {
            client.close();
        }
    }

    public PaginatedResult<S> execute(final Function<T, Collection<S>> function, final Sort<S> sort,
                                      final int pageNo, final int pageSize) {
        return this.execute(function, sort, pageNo, pageSize, new AuthenticatedFilter(this.token));
    }

    PaginatedResult<S> execute(final Function<T, Collection<S>> function, final Sort<S> sort,
                               final int pageNo, final int pageSize, final RoboZonkyFilter filter) {
        filter.setRequestHeader("X-Page", String.valueOf(pageNo));
        filter.setRequestHeader("X-Size", String.valueOf(pageSize));
        final Collection<S> result = this.execute(function, sort, filter);
        final int totalSize = filter.getLastResponseHeader("X-Total")
                .map(Integer::parseInt)
                .orElse(-1);
        return new PaginatedResult<>(result, pageNo, totalSize);
    }

}

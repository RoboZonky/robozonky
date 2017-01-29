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

package com.github.triceo.robozonky.app.authentication;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.internal.api.AbstractApiProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

public class ApiProvider extends AbstractApiProvider {

    private static final String ZONKY_URL = "https://api.zonky.cz";

    public ApiProvider(final ResteasyClientBuilder clientBuilder) {
        super(clientBuilder);
    }

    public ApiProvider() {
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZonkyOAuthApi oauth() {
        return this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, new AuthenticationFilter());
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication.
     *
     * @param token The Zonky API token, representing an authenticated user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZonkyApi authenticated(final ZonkyApiToken token) {
        return this.obtain(ZonkyApi.class, ApiProvider.ZONKY_URL, new AuthenticatedFilter(token));
    }

}

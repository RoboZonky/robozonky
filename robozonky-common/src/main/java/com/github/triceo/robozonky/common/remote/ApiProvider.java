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

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.EntityCollectionApi;
import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides instances of APIs for the rest of RoboZonky to use.
 */
public class ApiProvider {

    private static final String ZONKY_URL = "https://api.zonky.cz";
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Instantiate an API as a RESTEasy client proxy.
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param filter Filter to use when communicating with the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <T> Api<T> obtain(final Class<T> api, final String url, final RoboZonkyFilter filter) {
        final ResteasyClient client = ProxyFactory.newResteasyClient(filter);
        final T proxy = ProxyFactory.newProxy(client, api, url);
        return new Api<>(proxy, client);
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <S, T extends EntityCollectionApi<S>> PaginatedApi<S, T> obtainPaginated(final Class<T> api,
                                                                                       final String url,
                                                                                       final ZonkyApiToken token) {
        return new PaginatedApi<>(api, url, token);
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     * @return New API instance.
     */
    public OAuth oauth() {
        return new OAuth(this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, new AuthenticationFilter()));
    }

    protected <T> Collection<T> marketplace(final Class<? extends EntityCollectionApi<T>> target, final String url) {
        try (final Api<? extends EntityCollectionApi<T>> api = this.obtain(target, url, new RoboZonkyFilter())) {
            return api.execute(a -> {
                return a.items();
            });
        }
    }

    /**
     * Retrieve available loans from Zonky marketplace cache, which requires no authentication.
     * @return Loans existing in the marketplace at the time this method was called.
     */
    public Collection<Loan> marketplace() {
        return this.marketplace(LoanApi.class, ApiProvider.ZONKY_URL);
    }

    public Zonky authenticated(final ZonkyApiToken token) {
        return new Zonky(this.control(token), this.marketplace(token), this.portfolio(token),
                         this.wallet(token));
    }

    /**
     * Retrieve user-specific Zonky loan API which requires authentication.
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     */
    private PaginatedApi<Loan, LoanApi> marketplace(final ZonkyApiToken token) {
        return this.obtainPaginated(LoanApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky wallet API which requires authentication.
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     */
    private PaginatedApi<BlockedAmount, WalletApi> wallet(final ZonkyApiToken token) {
        return this.obtainPaginated(WalletApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky portfolio API which requires authentication.
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     */
    private PaginatedApi<Investment, PortfolioApi> portfolio(final ZonkyApiToken token) {
        return this.obtainPaginated(PortfolioApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky control API which requires authentication.
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     */
    private Api<ControlApi> control(final ZonkyApiToken token) {
        return this.obtain(ControlApi.class, ApiProvider.ZONKY_URL, new AuthenticatedFilter(token));
    }
}

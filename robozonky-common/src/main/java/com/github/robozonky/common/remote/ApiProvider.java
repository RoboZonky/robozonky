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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides instances of APIs for the rest of RoboZonky to use.
 */
public class ApiProvider {

    public static final String ZONKY_URL = "https://api.zonky.cz";
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    static <X> Function<X, Void> toFunction(final Consumer<X> f) {
        return (x) -> {
            f.accept(x);
            return null;
        };
    }

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
     * Instantiate an API as a RESTEasy client proxy.
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <S, T extends EntityCollectionApi<S>> PaginatedApi<S, T> obtainPaginated(final Class<T> api,
                                                                                       final String url,
                                                                                       final RoboZonkyFilter filter) {
        return new PaginatedApi<>(api, url, filter);
    }

    private OAuth oauth() {
        return new OAuth(this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, new AuthenticationFilter()));
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     * @param operation Operation to execute over the endpoint.
     * @return Return value of the operation.
     */
    public <T> T oauth(final Function<OAuth, T> operation) {
        try (final OAuth o = oauth()) {
            return operation.apply(o);
        }
    }

    protected <T> Collection<T> marketplace(final Class<? extends EntityCollectionApi<T>> target, final String url) {
        try (final Api<? extends EntityCollectionApi<T>> api = this.obtain(target, url, new RoboZonkyFilter())) {
            return api.execute(a -> {
                return a.items();
            });
        }
    }

    private <T> Collection<T> marketplace(final Class<? extends EntityCollectionApi<T>> target, final Select select,
                                          final String url) {
        final RoboZonkyFilter filter = new RoboZonkyFilter();
        select.accept(filter);
        final PaginatedApi<T, ? extends EntityCollectionApi<T>> api = this.obtainPaginated(target, url, filter);
        return Zonky.getStream(api).collect(Collectors.toList());
    }

    public Collection<Loan> marketplace(final Select select) {
        return marketplace(LoanApi.class, select, ApiProvider.ZONKY_URL);
    }

    /**
     * Retrieve available loans from Zonky marketplace cache, which requires no authentication.
     * @return Loans existing in the marketplace at the time this method was called.
     */
    public Collection<Loan> marketplace() {
        return marketplace(new Select());
    }

    private Zonky authenticated(final ZonkyApiToken token) {
        return new Zonky(this.control(token), this.marketplace(token), this.secondaryMarketplace(token),
                         this.portfolio(token), this.wallet(token));
    }

    public void authenticated(final ZonkyApiToken token, final Consumer<Zonky> operation) {
        authenticated(token, toFunction(operation));
    }

    public <T> T authenticated(final ZonkyApiToken token, final Function<Zonky, T> operation) {
        try (final Zonky z = authenticated(token)) {
            return operation.apply(z);
        }
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
     * Retrieve user-specific Zonky participation API which requires authentication.
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     */
    private PaginatedApi<Participation, ParticipationApi> secondaryMarketplace(final ZonkyApiToken token) {
        return this.obtainPaginated(ParticipationApi.class, ApiProvider.ZONKY_URL, token);
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

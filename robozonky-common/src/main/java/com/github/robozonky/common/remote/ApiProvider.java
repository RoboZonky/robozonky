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
import java.util.function.Supplier;

import com.github.robozonky.api.remote.CollectionsApi;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.TransactionApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawDevelopment;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.util.StreamUtil;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides instances of APIs for the rest of RoboZonky to use.
 */
public class ApiProvider implements AutoCloseable {

    public static final String ZONKY_URL = "https://api.zonky.cz";
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Clients are heavyweight objects where both creation and destruction potentially takes a lot of time. They should
     * be reused as much as possible.
     */
    private final ResteasyClient client;

    public ApiProvider() {
        this(ProxyFactory.newResteasyClient());
    }

    ApiProvider(final ResteasyClient client) {
        this.client = client;
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <S, T> PaginatedApi<S, T> obtainPaginated(final Class<T> api, final String url,
                                                        final Supplier<ZonkyApiToken> token) {
        return new PaginatedApi<>(api, url, token, client);
    }

    private OAuth oauth() {
        final ZonkyOAuthApi proxy = ProxyFactory.newProxy(client, new AuthenticationFilter(), ZonkyOAuthApi.class,
                                                          ApiProvider.ZONKY_URL);
        return new OAuth(new Api<>(proxy));
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     * @param operation Operation to execute over the endpoint.
     * @return Return value of the operation.
     */
    public <T> T oauth(final Function<OAuth, T> operation) {
        return operation.apply(oauth());
    }

    protected <T> Collection<T> marketplace(final Class<? extends EntityCollectionApi<T>> target, final String url) {
        final EntityCollectionApi<T> proxy = ProxyFactory.newProxy(client, new RoboZonkyFilter(), target, url);
        final Api<? extends EntityCollectionApi<T>> api = new Api<>(proxy);
        return api.execute(a -> {
            return a.items();
        });
    }

    /**
     * Retrieve available loans from Zonky marketplace cache, which requires no authentication.
     * @return Loans existing in the marketplace at the time this method was called.
     */
    public Collection<RawLoan> marketplace() {
        return this.marketplace(LoanApi.class, ApiProvider.ZONKY_URL);
    }

    private Zonky authenticated(final Supplier<ZonkyApiToken> token) {
        return new Zonky(this.control(token), this.marketplace(token), this.secondaryMarketplace(token),
                         this.portfolio(token), this.wallet(token), this.transaction(token), this.collections(token));
    }

    public void authenticated(final Supplier<ZonkyApiToken> token, final Consumer<Zonky> operation) {
        authenticated(token, StreamUtil.toFunction(operation));
    }

    public <T> T authenticated(final Supplier<ZonkyApiToken> token, final Function<Zonky, T> operation) {
        return operation.apply(authenticated(token));
    }

    /**
     * Retrieve user-specific Zonky loan API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<RawLoan, LoanApi> marketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(LoanApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky participation API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<Participation, ParticipationApi> secondaryMarketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(ParticipationApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky wallet API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<BlockedAmount, WalletApi> wallet(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(WalletApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky transactions API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<Transaction, TransactionApi> transaction(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(TransactionApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky portfolio API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<RawInvestment, PortfolioApi> portfolio(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(PortfolioApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky control API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private Api<ControlApi> control(final Supplier<ZonkyApiToken> token) {
        final ControlApi proxy = ProxyFactory.newProxy(client, new AuthenticatedFilter(token), ControlApi.class,
                                                       ApiProvider.ZONKY_URL);
        return new Api<>(proxy);
    }

    /**
     * Retrieve user-specific Zonky API which provides information on loan collections.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    private PaginatedApi<RawDevelopment, CollectionsApi> collections(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(CollectionsApi.class, ApiProvider.ZONKY_URL, token);
    }

    @Override
    public void close() {
        client.close();
    }

    public boolean isClosed() {
        return client.isClosed();
    }
}

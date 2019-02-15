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

package com.github.robozonky.common.remote;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.CollectionsApi;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.ReservationApi;
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
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

/**
 * Provides instances of APIs for the rest of RoboZonky to use.
 */
public class ApiProvider implements AutoCloseable {

    public static final String ZONKY_URL = "https://api.zonky.cz";
    private static final Logger LOGGER = LogManager.getLogger(ApiProvider.class);
    /**
     * Instances of the Zonky API are kept for as long as the token supplier is kept by the GC. This guarantees that,
     * for the lifetime of the token supplier, the expensive API-retrieving operations wouldn't be executed twice.
     */
    private final Map<Supplier<ZonkyApiToken>, Zonky> authenticated = new WeakHashMap<>(0);
    /**
     * Clients are heavyweight objects where both creation and destruction potentially takes a lot of time. They should
     * be reused as much as possible.
     */
    private final Lazy<ResteasyClient> client;

    public ApiProvider() {
        this.client = Lazy.of(ProxyFactory::newResteasyClient);
    }

    static <T> Api<T> obtainNormal(final T proxy) {
        return new Api<>(proxy);
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     * @param <S> API return type.
     * @param <T> API type.
     * @param api RESTEasy endpoint.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    <S, T extends EntityCollectionApi<S>> PaginatedApi<S, T> obtainPaginated(final Class<T> api,
                                                                             final Supplier<ZonkyApiToken> token) {
        return new PaginatedApi<>(api, ZONKY_URL, token, client.get());
    }

    <T> Api<T> obtainNormal(final Class<T> api, final Supplier<ZonkyApiToken> token) {
        final T proxy = ProxyFactory.newProxy(client.get(), new AuthenticatedFilter(token), api, ZONKY_URL);
        return obtainNormal(proxy);
    }

    private OAuth oauth() {
        final ZonkyOAuthApi proxy = ProxyFactory.newProxy(client.get(), new AuthenticationFilter(), ZonkyOAuthApi.class,
                                                          ZONKY_URL);
        return new OAuth(obtainNormal(proxy));
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     * @param operation Operation to execute over the endpoint.
     * @param <T> Operation return type.
     * @return Return value of the operation.
     */
    public <T> T oauth(final Function<OAuth, T> operation) {
        return operation.apply(oauth());
    }

    /**
     * Retrieve available loans from Zonky marketplace cache, which requires no authentication. Does not support paging.
     * @return Loans existing in the marketplace at the time this method was called.
     */
    public Collection<RawLoan> marketplace() {
        final EntityCollectionApi<RawLoan> proxy = ProxyFactory.newProxy(client.get(), LoanApi.class, ZONKY_URL);
        final Api<? extends EntityCollectionApi<RawLoan>> api = obtainNormal(proxy);
        return api.call(EntityCollectionApi::items);
    }

    private synchronized Zonky authenticated(final Supplier<ZonkyApiToken> token) {
        return authenticated.computeIfAbsent(token, key -> {
            LOGGER.debug("Creating a new authenticated API for {}.", token);
            return new Zonky(this, token);
        });
    }

    public void run(final Consumer<Zonky> operation, final Supplier<ZonkyApiToken> token) {
        call(StreamUtil.toFunction(operation), token);
    }

    public <T> T call(final Function<Zonky, T> operation, final Supplier<ZonkyApiToken> token) {
        return operation.apply(authenticated(token));
    }

    /**
     * Retrieve user-specific Zonky loan API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<RawLoan, LoanApi> marketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(LoanApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky participation API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<Participation, ParticipationApi> secondaryMarketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(ParticipationApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky wallet API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<BlockedAmount, WalletApi> wallet(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(WalletApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky transactions API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<Transaction, TransactionApi> transactions(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(TransactionApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky portfolio API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<RawInvestment, PortfolioApi> portfolio(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(PortfolioApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky control API which requires authentication.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    Api<ControlApi> control(final Supplier<ZonkyApiToken> token) {
        return obtainNormal(ControlApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication and allows to download various XLS exports.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    Api<ExportApi> exports(final Supplier<ZonkyApiToken> token) {
        return obtainNormal(ExportApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication and allows to retrieve reservations
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    Api<ReservationApi> reservations(final Supplier<ZonkyApiToken> token) {
        return obtainNormal(ReservationApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky API which provides information on loan collections.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<RawDevelopment, CollectionsApi> collections(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(CollectionsApi.class, token);
    }

    @Override
    public void close() {
        client.get().close();
    }

    public boolean isClosed() {
        return client.get().isClosed();
    }
}

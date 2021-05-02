/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.ApiConstants;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.endpoints.ControlApi;
import com.github.robozonky.internal.remote.endpoints.EntityCollectionApi;
import com.github.robozonky.internal.remote.endpoints.LoanApi;
import com.github.robozonky.internal.remote.endpoints.ParticipationApi;
import com.github.robozonky.internal.remote.endpoints.PortfolioApi;
import com.github.robozonky.internal.remote.endpoints.ReservationApi;
import com.github.robozonky.internal.remote.endpoints.ZonkyOAuthApi;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.ParticipationImpl;
import com.github.robozonky.internal.util.StreamUtil;
import com.github.robozonky.internal.util.functional.Memoizer;

import io.micrometer.core.instrument.Timer;

public class ApiProvider implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(ApiProvider.class);

    private final Timer meteredRequestTimer;
    private final Timer unmeteredRequestTimer;
    /**
     * Instances of the Zonky API are kept for as long as the token supplier is kept by the GC. This guarantees that,
     * for the lifetime of the token supplier, the expensive API-retrieving operations wouldn't be executed twice.
     */
    private final Map<Supplier<ZonkyApiToken>, Zonky> authenticated = new WeakHashMap<>(0);
    /**
     * Clients are heavyweight objects where both creation and destruction potentially takes a lot of time. They should
     * be reused as much as possible.
     */
    private final Supplier<ResteasyClient> client;

    public ApiProvider() {
        this(UUID.randomUUID()
            .toString());
    }

    public ApiProvider(String id) {
        this.client = Memoizer.memoize(ProxyFactory::newResteasyClient);
        var clientName = Objects.requireNonNullElse(id, "default");
        meteredRequestTimer = Timer.builder("robozonky.api.requests")
            .tag("metered", "true")
            .tag("client", clientName)
            .description("Rate-limited requests to Zonky API.")
            .register(Defaults.METER_REGISTRY);
        unmeteredRequestTimer = Timer.builder("robozonky.api.requests")
            .tag("metered", "false")
            .tag("client", clientName)
            .description("Non-rate-limited requests to Zonky API.")
            .register(Defaults.METER_REGISTRY);
    }

    static <T> Api<T> actuallyObtainNormal(final T proxy, final Timer timer) {
        return new Api<>(proxy, timer);
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     * 
     * @param <S>   API return type.
     * @param <T>   API type.
     * @param api   RESTEasy endpoint.
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    <S, T extends EntityCollectionApi<S>> PaginatedApi<S, T> obtainPaginated(final Class<T> api,
            final Supplier<ZonkyApiToken> token) {
        return new PaginatedApi<>(api, ApiConstants.ZONKY_API_HOSTNAME, token, client.get(), meteredRequestTimer,
                unmeteredRequestTimer);
    }

    <T> Api<T> obtainNormal(final Class<T> api, final Supplier<ZonkyApiToken> token, Timer timer) {
        var proxy = ProxyFactory.newProxy(client.get(), new AuthenticatedFilter(token), api,
                ApiConstants.ZONKY_API_HOSTNAME);
        return actuallyObtainNormal(proxy, timer);
    }

    private OAuth oauth() {
        var proxy = ProxyFactory.newProxy(client.get(), new AuthenticationFilter(), ZonkyOAuthApi.class,
                ApiConstants.ZONKY_API_HOSTNAME);
        return new OAuth(actuallyObtainNormal(proxy, unmeteredRequestTimer));
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     * 
     * @param operation Operation to execute over the endpoint.
     * @param <T>       Operation return type.
     * @return Return value of the operation.
     */
    public <T> T oauth(final Function<OAuth, T> operation) {
        return operation.apply(oauth());
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
     * 
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<LoanImpl, LoanApi> marketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(LoanApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky participation API which requires authentication.
     * 
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<ParticipationImpl, ParticipationApi> secondaryMarketplace(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(ParticipationApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky portfolio API which requires authentication.
     * 
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    PaginatedApi<InvestmentImpl, PortfolioApi> portfolio(final Supplier<ZonkyApiToken> token) {
        return this.obtainPaginated(PortfolioApi.class, token);
    }

    /**
     * Retrieve user-specific Zonky control API which requires authentication.
     * 
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    Api<ControlApi> control(final Supplier<ZonkyApiToken> token) {
        return obtainNormal(ControlApi.class, token, unmeteredRequestTimer);
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication and allows to retrieve reservations
     * 
     * @param token Supplier of a valid Zonky API token, always representing the active user.
     * @return New API instance.
     */
    Api<ReservationApi> reservations(final Supplier<ZonkyApiToken> token) {
        return obtainNormal(ReservationApi.class, token, meteredRequestTimer);
    }

    public Timer getMeteredRequestTimer() {
        return meteredRequestTimer;
    }

    @Override
    public void close() {
        client.get()
            .close();
    }

    public boolean isClosed() {
        return client.get()
            .isClosed();
    }
}

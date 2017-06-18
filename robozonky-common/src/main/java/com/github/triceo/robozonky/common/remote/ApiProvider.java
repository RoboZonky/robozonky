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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.xml.ws.WebServiceClient;

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
 * Provides instances of APIs for the rest of RoboZonky to use. When no longer needed, the ApiProvider needs to be
 * {@link #close()}ed in order to not leak {@link WebServiceClient}s that weren't already closed by
 * {@link Api#close()}.
 */
public class ApiProvider implements AutoCloseable {

    public static final String ZONKY_URL = "https://api.zonky.cz";

    /**
     * Use weak references so that the clients aren't kept around forever, with all the entities they carry.
     */
    private final Collection<WeakReference<AutoCloseable>> clients = new ArrayList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Instantiate an API as a RESTEasy client proxy.
     *
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param filter Filter to use when communicating with the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <T> Api<T> obtain(final Class<T> api, final String url, final RoboZonkyFilter filter) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
        final ResteasyClient client = ProxyFactory.newResteasyClient(filter);
        final T proxy = ProxyFactory.newProxy(client, api, url);
        final Api<T> wrapper = new Api<>(proxy, client);
        this.clients.add(new WeakReference<>(wrapper));
        return wrapper;
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     *
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <S, T extends EntityCollectionApi<S>> PaginatedApi<S, T> obtainPaginated(final Class<T> api,
                                                                                       final String url,
                                                                                       final ZonkyApiToken token) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
        return new PaginatedApi<>(api, url, token);
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public OAuth oauth() {
        return new OAuth(this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, new AuthenticationFilter()));
    }

    /**
     * Retrieve user-specific Zonky API which does not require authentication.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public Api<LoanApi> marketplace() {
        return this.obtain(LoanApi.class, ApiProvider.ZONKY_URL, new RoboZonkyFilter());
    }

    public Zonky authenticated(final ZonkyApiToken token) {
        return new Zonky(this.control(token), this.marketplace(token), this.portfolio(token),
                this.wallet(token));
    }

    /**
     * Retrieve user-specific Zonky loan API which requires authentication.
     *
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    private PaginatedApi<Loan, LoanApi> marketplace(final ZonkyApiToken token) {
        return this.obtainPaginated(LoanApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky wallet API which requires authentication.
     *
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    private PaginatedApi<BlockedAmount, WalletApi> wallet(final ZonkyApiToken token) {
        return this.obtainPaginated(WalletApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky portfolio API which requires authentication.
     *
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    private PaginatedApi<Investment, PortfolioApi> portfolio(final ZonkyApiToken token) {
        return this.obtainPaginated(PortfolioApi.class, ApiProvider.ZONKY_URL, token);
    }

    /**
     * Retrieve user-specific Zonky control API which requires authentication.
     *
     * @param token The Zonky API token, representing an control user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    private Api<ControlApi> control(final ZonkyApiToken token) {
        return this.obtain(ControlApi.class, ApiProvider.ZONKY_URL, new AuthenticatedFilter(token));
    }

    @Override
    public synchronized void close() {
        if (this.isClosed.get()) {
            return;
        }
        this.clients.stream()
                .flatMap(w -> w.get() == null ? Stream.empty() : Stream.of(w.get()))
                .forEach(c -> {
                    try {
                        c.close();
                    } catch (final Exception ex) {
                        LOGGER.trace("Failed closing client: {}.", c, ex);
                    }
                });
        this.isClosed.set(true);
    }

}

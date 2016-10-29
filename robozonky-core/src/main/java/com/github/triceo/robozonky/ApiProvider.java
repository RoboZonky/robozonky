/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.ws.WebServiceClient;

import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides instances of Zonky and Zotify API for the rest of RoboZonky to use. When no longer needed, the ApiProvider
 * needs to be {@link #close()}ed in order to not leak {@link WebServiceClient}s.
 */
public class ApiProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiProvider.class);
    private static final String ZONKY_URL = "https://api.zonky.cz";
    private static final String ZOTIFY_URL = "http://zotify.cz";

    private static final ResteasyProviderFactory RESTEASY;
    static {
        ApiProvider.LOGGER.trace("Initializing RESTEasy.");
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(ApiProvider.RESTEASY);
        final Class<?> jsonProvider = ResteasyJackson2Provider.class;
        if (!ApiProvider.RESTEASY.isRegistered(jsonProvider)) { // https://github.com/triceo/robozonky/issues/56
            ApiProvider.RESTEASY.registerProvider(jsonProvider);
        }
        ApiProvider.LOGGER.trace("RESTEasy initialized.");
    }

    private static ResteasyClientBuilder newResteasyClientBuilder() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        final CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setConnectionManager(cm).build();
        final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(closeableHttpClient);
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder().httpEngine(engine);
        clientBuilder.providerFactory(ApiProvider.RESTEASY);
        return clientBuilder;

    }

    private final boolean isDryRun;
    private final ClientBuilder clientBuilder;
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private final Collection<Client> clients = new ArrayList<>();

    /**
     * Create a new instance of the API provider that will use a given instance of {@link ResteasyClientBuilder}. It is
     * the responsibility of the caller to make sure that the builder and all clients are thread-safe and can process
     * parallel HTTP requests.
     *
     * @param clientBuilder Client builder to use to instantiate all the APIs.
     */
    public ApiProvider(final boolean isDryRun, final ResteasyClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
        this.isDryRun = isDryRun;
    }

    /**
     * Create a new instance of the API provider that will use a fresh instance of {@link ResteasyClientBuilder}. This
     * client will be thread-safe.
     */
    public ApiProvider(final boolean isDryRun) {
        this(isDryRun, ApiProvider.newResteasyClientBuilder());
    }

    private synchronized Client newClient() {
        final Client client = this.clientBuilder.build();
        ApiProvider.LOGGER.trace("Registering new RESTEasy client: {}.", client);
        this.clients.add(client);
        return client;
    }

    private synchronized void ensureNotDestroyed() {
        if (this.isDestroyed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
    }

    private <T> T obtain(final Class<T> api, final String apiUrl, final CommonFilter filter) {
        this.ensureNotDestroyed();
        final ResteasyWebTarget target = (ResteasyWebTarget)this.newClient().register(filter).target(apiUrl);
        target.register(new BrowserCacheFeature()); // honor server-sent cache-related headers
        return target.proxy(api);
    }

    /**
     * Retrieve Zotify's marketplace cache.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZotifyApi cache() {
        return this.obtain(ZotifyApi.class, ApiProvider.ZOTIFY_URL, new ZotifyFilter());
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     *
     * @param filter Filter that will decorate the requests with OAuth headers.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZonkyOAuthApi oauth(final CommonFilter filter) {
        return this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, filter);
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication.
     *
     * @param filter Filter that will decorate the requests with OAuth headers.
     * @return New API instance. If {@link #isDryRun()} is false, returns instance of {@link InvestingZonkyApi}.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ZonkyApi authenticated(final CommonFilter filter) {
        if (this.isDryRun) {
            return this.obtain(ZonkyApi.class, ApiProvider.ZONKY_URL, filter);
        } else {
            return this.obtain(InvestingZonkyApi.class, ApiProvider.ZONKY_URL, filter);
        }
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    @Override
    public synchronized void close() {
        this.ensureNotDestroyed();
        this.clients.forEach(c -> {
            ApiProvider.LOGGER.trace("Destroying RESTEasy client: {}.", c);
            c.close();
        });
        this.clients.clear();
        this.isDestroyed.set(true);
    }

}

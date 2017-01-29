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

package com.github.triceo.robozonky.internal.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.ws.WebServiceClient;

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
 * Provides instances of APIs for the rest of RoboZonky to use. When no longer needed, the ApiProvider needs to be
 * {@link #close()}ed in order to not leak {@link WebServiceClient}s.
 */
public abstract class AbstractApiProvider implements AutoCloseable {

    private static final ResteasyProviderFactory RESTEASY;
    static {
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(AbstractApiProvider.RESTEASY);
        final Class<?> jsonProvider = ResteasyJackson2Provider.class;
        if (!AbstractApiProvider.RESTEASY.isRegistered(jsonProvider)) { // https://github.com/triceo/robozonky/issues/56
            AbstractApiProvider.RESTEASY.registerProvider(jsonProvider);
        }
    }

    private static ResteasyClientBuilder newResteasyClientBuilder() {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        final CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().setConnectionManager(cm).build();
        final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(closeableHttpClient);
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder().httpEngine(engine);
        clientBuilder.providerFactory(AbstractApiProvider.RESTEASY);
        return clientBuilder;
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ClientBuilder clientBuilder;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Collection<Client> clients = new ArrayList<>();

    /**
     * Create a new instance of the API provider that will use a given instance of {@link ResteasyClientBuilder}. It is
     * the responsibility of the caller to make sure that the builder and all clients are thread-safe and can process
     * parallel HTTP requests.
     *
     * @param clientBuilder Client builder to use to instantiate all the APIs.
     */
    public AbstractApiProvider(final ResteasyClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    /**
     * Create a new instance of the API provider that will use a fresh instance of {@link ResteasyClientBuilder}. This
     * client will be thread-safe.
     */
    public AbstractApiProvider() {
        this(AbstractApiProvider.newResteasyClientBuilder());
    }

    private synchronized Client newClient() {
        final Client client = this.clientBuilder.build();
        LOGGER.trace("Registering new RESTEasy client: {}.", client);
        this.clients.add(client);
        return client;
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     *
     * @param api RESTEasy endpoint.
     * @param apiUrl URL to the web API represented by the endpoint.
     * @param filter Filter to use when communicating with the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <T> T obtain(final Class<T> api, final String apiUrl, final RoboZonkyFilter filter) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
        final ResteasyWebTarget target = (ResteasyWebTarget)this.newClient().register(filter).target(apiUrl);
        target.register(new BrowserCacheFeature()); // honor server-sent cache-related headers
        return target.proxy(api);
    }

    @Override
    public synchronized void close() {
        if (this.isClosed.get()) {
            return;
        }
        this.clients.forEach(c -> {
            LOGGER.trace("Destroying RESTEasy client: {}.", c);
            c.close();
        });
        this.isClosed.set(true);
    }

}

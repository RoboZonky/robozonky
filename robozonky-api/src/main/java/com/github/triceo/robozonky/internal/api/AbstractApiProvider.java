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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.xml.ws.WebServiceClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
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
 * {@link #close()}ed in order to not leak {@link WebServiceClient}s that weren't already closed by
 * {@link AbstractApiProvider.ApiWrapper#close()}.
 */
public abstract class AbstractApiProvider implements AutoCloseable {

    /**
     * Represents a close-able RESTEasy client proxy. Users should preferably call {@link #close()} after they're
     * done with the API.
     *
     * @param <T> Type of the API to be handled.
     */
    public static class ApiWrapper<T> implements AutoCloseable {

        private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiProvider.ApiWrapper.class);

        private final ResteasyClient client;
        private final T api;

        public ApiWrapper(final T api) {
            this(api, null);
        }

        public ApiWrapper(final T api, final ResteasyClient client) {
            AbstractApiProvider.ApiWrapper.LOGGER.trace("Registering new RESTEasy client: {}.", client);
            this.client = client;
            this.api = api;
        }

        public <S> S execute(final Function<T, S> function) {
            return function.apply(api);
        }

        public void execute(final Consumer<T> function) {
            function.accept(api);
        }

        boolean isClosed() {
            return client == null || client.isClosed();
        }

        @Override
        public synchronized void close() {
            if (client != null && !client.isClosed()) {
                AbstractApiProvider.ApiWrapper.LOGGER.trace("Destroying RESTEasy client: {}.", client);
                client.close();
            }
        }
    }

    private static final ResteasyProviderFactory RESTEASY;
    static {
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(AbstractApiProvider.RESTEASY);
        final Class<?> jsonProvider = ResteasyJackson2Provider.class;
        if (!AbstractApiProvider.RESTEASY.isRegistered(jsonProvider)) { // https://github.com/triceo/robozonky/issues/56
            AbstractApiProvider.RESTEASY.registerProvider(jsonProvider);
        }
    }

    private static final HttpClientBuilder HTTP_CLIENT_BUILDER =
            HttpClientBuilder.create().setConnectionManager(new PoolingHttpClientConnectionManager());

    private static ResteasyClientBuilder newResteasyClientBuilder() {
        final CloseableHttpClient closeableHttpClient = AbstractApiProvider.HTTP_CLIENT_BUILDER.build();
        final ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(closeableHttpClient);
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder().httpEngine(engine);
        clientBuilder.providerFactory(AbstractApiProvider.RESTEASY);
        return clientBuilder;
    }

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final ResteasyClientBuilder clientBuilder;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    /**
     * Use weak references so that the clients aren't kept around forever, with all the entities they carry.
     */
    private final Collection<WeakReference<AutoCloseable>> clients = new ArrayList<>();

    /**
     * Create a new instance of the API provider that will use a given instance of {@link ResteasyClientBuilder}. It is
     * the responsibility of the caller to make sure that the builder and all clients are thread-safe and can process
     * parallel HTTP requests.
     *
     * @param clientBuilder Client builder to use to instantiate all the APIs.
     */
    protected AbstractApiProvider(final ResteasyClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    /**
     * Create a new instance of the API provider that will use a fresh instance of {@link ResteasyClientBuilder}. This
     * client will be thread-safe.
     */
    public AbstractApiProvider() {
        this(AbstractApiProvider.newResteasyClientBuilder());
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
    protected <T> AbstractApiProvider.ApiWrapper<T> obtain(final Class<T> api, final String apiUrl,
                                                           final RoboZonkyFilter filter) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
        final ResteasyClient client = this.clientBuilder.build();
        final ResteasyWebTarget target = client.register(filter).target(apiUrl);
        target.register(new BrowserCacheFeature()); // honor server-sent cache-related headers
        final AbstractApiProvider.ApiWrapper<T> wrapper = new AbstractApiProvider.ApiWrapper<>(target.proxy(api), client);
        this.clients.add(new WeakReference<>(wrapper));
        return wrapper;
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

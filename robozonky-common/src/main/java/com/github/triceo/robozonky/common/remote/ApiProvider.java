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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.xml.ws.WebServiceClient;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.ZonkyOAuthApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.internal.api.Settings;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides instances of APIs for the rest of RoboZonky to use. When no longer needed, the ApiProvider needs to be
 * {@link #close()}ed in order to not leak {@link WebServiceClient}s that weren't already closed by
 * {@link ApiProvider.ApiWrapper#close()}.
 */
public class ApiProvider implements AutoCloseable {

    static class RedirectingHttpClient extends ApacheHttpClient43Engine {

        public RedirectingHttpClient(final HttpClient httpClient) {
            super(httpClient);
        }

        @Override
        protected void loadHttpMethod(final ClientInvocation request, final HttpRequestBase httpMethod)
                throws Exception {
            super.loadHttpMethod(request, httpMethod);
            httpMethod.setConfig(RequestConfig.copy(httpMethod.getConfig()).setRedirectsEnabled(true).build());
        }

    }

    /**
     * Represents a close-able RESTEasy client proxy. Users should preferably call {@link #close()} after they're
     * done with the API.
     *
     * @param <T> Type of the API to be handled.
     */
    public static class ApiWrapper<T> implements AutoCloseable {

        private final ResteasyClient client;
        private final T api;

        public ApiWrapper(final T api) {
            this(api, null);
        }

        public ApiWrapper(final T api, final ResteasyClient client) {
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
                client.close();
            }
        }
    }

    private static final ResteasyProviderFactory RESTEASY = ResteasyProviderFactory.getInstance();
    static {
        RegisterBuiltin.register(ApiProvider.RESTEASY);
        final Class<?> jsonProvider = ResteasyJackson2Provider.class;
        if (!ApiProvider.RESTEASY.isRegistered(jsonProvider)) {
            ApiProvider.RESTEASY.registerProvider(jsonProvider);
        }
    }
    private static final HttpClientConnectionManager CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();
    private static final String ZONKY_URL = "https://api.zonky.cz";

    /**
     * Use weak references so that the clients aren't kept around forever, with all the entities they carry.
     */
    private final Collection<WeakReference<AutoCloseable>> clients = new ArrayList<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private static ResteasyClient newResteasyClient() {
        final int socketTimeout = (int)(Settings.INSTANCE.getSocketTimeout().get(ChronoUnit.SECONDS)) * 1000;
        final int connectionTimeout = (int)(Settings.INSTANCE.getConnectionTimeout().get(ChronoUnit.SECONDS)) * 1000;
        final RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
        final CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig) // avoid "sudden death" (marketplace forever blocking on socket)
                .setConnectionManager(ApiProvider.CONNECTION_MANAGER)
                .build();
        final ClientHttpEngine httpEngine = new ApiProvider.RedirectingHttpClient(httpClient);
        return new ResteasyClientBuilder()
                .httpEngine(httpEngine)
                .providerFactory(ApiProvider.RESTEASY)
                .build();
    }

    /**
     * Instantiate an API as a RESTEasy client proxy.
     *
     * @param api RESTEasy endpoint.
     * @param url URL to the web API represented by the endpoint.
     * @param filter Filter to use when communicating with the endpoint.
     * @param <T> API type.
     * @return RESTEasy client proxy for the API, ready to be called.
     */
    protected <T> ApiProvider.ApiWrapper<T> obtain(final Class<T> api, final String url, final RoboZonkyFilter filter) {
        if (this.isClosed.get()) {
            throw new IllegalStateException("Attempting to use an already destroyed ApiProvider.");
        }
        final ResteasyClient client = ApiProvider.newResteasyClient();
        final T proxy = client.register(filter)
                .target(url)
                .register(new BrowserCacheFeature())
                .proxy(api);
        final ApiProvider.ApiWrapper<T> wrapper = new ApiProvider.ApiWrapper<>(proxy, client);
        this.clients.add(new WeakReference<>(wrapper));
        return wrapper;
    }

    /**
     * Retrieve Zonky's OAuth endpoint.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ApiProvider.ApiWrapper<ZonkyOAuthApi> oauth() {
        return this.obtain(ZonkyOAuthApi.class, ApiProvider.ZONKY_URL, new AuthenticationFilter());
    }

    /**
     * Retrieve user-specific Zonky API which does not require authentication.
     *
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ApiProvider.ApiWrapper<ZonkyApi> anonymous() {
        return this.obtain(ZonkyApi.class, ApiProvider.ZONKY_URL, new RoboZonkyFilter());
    }

    /**
     * Retrieve user-specific Zonky API which requires authentication.
     *
     * @param token The Zonky API token, representing an authenticated user.
     * @return New API instance.
     * @throws IllegalStateException If {@link #close()} already called.
     */
    public ApiProvider.ApiWrapper<ZonkyApi> authenticated(final ZonkyApiToken token) {
        return this.obtain(ZonkyApi.class, ApiProvider.ZONKY_URL, new AuthenticatedFilter(token));
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
        ApiProvider.CONNECTION_MANAGER.closeExpiredConnections();
        ApiProvider.CONNECTION_MANAGER.closeIdleConnections(5, TimeUnit.MINUTES);
    }

}

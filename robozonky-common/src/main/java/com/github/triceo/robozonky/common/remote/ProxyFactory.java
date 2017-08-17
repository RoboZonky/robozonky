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

package com.github.triceo.robozonky.common.remote;

import java.time.temporal.ChronoUnit;
import javax.ws.rs.core.Feature;

import com.github.triceo.robozonky.internal.api.Settings;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

final class ProxyFactory {

    private static final Feature CACHE = new BrowserCacheFeature();
    private static final ResteasyProviderFactory RESTEASY = ResteasyProviderFactory.getInstance();

    static {
        RegisterBuiltin.register(ProxyFactory.RESTEASY);
        final Class<?> jsonProvider = ResteasyJackson2Provider.class;
        if (!ProxyFactory.RESTEASY.isRegistered(jsonProvider)) {
            ProxyFactory.RESTEASY.registerProvider(jsonProvider);
        }
    }

    private static final SocketConfig SOCKET_CONFIG = SocketConfig.copy(SocketConfig.DEFAULT)
            .setSoTimeout((int) (Settings.INSTANCE.getSocketTimeout().get(ChronoUnit.SECONDS)) * 1000)
            .build();
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.copy(RequestConfig.DEFAULT)
            .setRedirectsEnabled(true)
            .setRelativeRedirectsAllowed(true)
            .setConnectTimeout((int) (Settings.INSTANCE.getConnectionTimeout().get(ChronoUnit.SECONDS)) * 1000)
            .setConnectionRequestTimeout(
                    (int) (Settings.INSTANCE.getConnectionTimeout().get(ChronoUnit.SECONDS)) * 1000)
            .setSocketTimeout(ProxyFactory.SOCKET_CONFIG.getSoTimeout())
            .build();
    private static final HttpClientBuilder CLIENT_BUILDER = HttpClientBuilder.create()
            .setRedirectStrategy(LaxRedirectStrategy.INSTANCE) // be tolerant of unexpected situations
            .setDefaultSocketConfig(ProxyFactory.SOCKET_CONFIG)
            .setDefaultRequestConfig(ProxyFactory.REQUEST_CONFIG); // no "sudden death" (marketplace blocking on socket)

    public static ResteasyClient newResteasyClient(final RoboZonkyFilter filter) {
        final ResteasyClient client = ProxyFactory.newResteasyClient();
        client.register(filter);
        return client;
    }

    public static ResteasyClient newResteasyClient() {
        final CloseableHttpClient httpClient = ProxyFactory.CLIENT_BUILDER.build();
        // FYI the redirecting properties above will be ignored; see RedirectingHttpClient's Javadoc
        final ClientHttpEngine httpEngine = new RedirectingHttpClient(httpClient);
        return new ResteasyClientBuilder()
                .httpEngine(httpEngine)
                .providerFactory(ProxyFactory.RESTEASY)
                .build();
    }

    public static <T> T newProxy(final ResteasyClient client, final Class<T> api, final String url) {
        return client.target(url).register(ProxyFactory.CACHE).proxy(api);
    }
}

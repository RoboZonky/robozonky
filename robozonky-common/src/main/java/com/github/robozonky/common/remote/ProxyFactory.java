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

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Feature;

import com.github.robozonky.internal.api.Settings;
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

    public static ResteasyClient newResteasyClient(final RoboZonkyFilter filter) {
        final ResteasyClient client = ProxyFactory.newResteasyClient();
        client.register(filter);
        return client;
    }

    public static ResteasyClient newResteasyClient() {
        return new ResteasyClientBuilder()
                .socketTimeout(Settings.INSTANCE.getSocketTimeout().get(ChronoUnit.SECONDS), TimeUnit.SECONDS)
                .establishConnectionTimeout(Settings.INSTANCE.getConnectionTimeout().get(ChronoUnit.SECONDS),
                                            TimeUnit.SECONDS)
                .providerFactory(ProxyFactory.RESTEASY)
                .build();
    }

    public static <T> T newProxy(final ResteasyClient client, final Class<T> api, final String url) {
        return client.target(url).register(ProxyFactory.CACHE).proxy(api);
    }
}

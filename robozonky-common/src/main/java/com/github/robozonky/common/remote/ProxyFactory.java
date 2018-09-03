/*
 * Copyright 2018 The RoboZonky Project
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

import java.util.concurrent.TimeUnit;

import com.github.robozonky.internal.api.Settings;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFactory.class);

    private ProxyFactory() {
        // no instances
    }

    public static ResteasyClient newResteasyClient() {
        LOGGER.debug("Creating RESTEasy client.");
        final Settings settings = Settings.INSTANCE;
        final long socketTimeout = settings.getSocketTimeout().toMillis();
        LOGGER.debug("Set socket timeout to {} ms.", socketTimeout);
        final long connectionTimeout = settings.getConnectionTimeout().toMillis();
        LOGGER.debug("Set connection timeout to {} ms.", connectionTimeout);
        // Supply the provider factory singleton, as otherwise RESTEasy would create a new instance every time.
        final ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .providerFactory(ResteasyProviderFactory.getInstance())
                .readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
        // setup HTTP proxy when required
        settings.getHttpsProxyHostname().ifPresent(host -> {
            final int port = settings.getHttpsProxyPort();
            builder.defaultProxy(host, port);
            LOGGER.debug("Set HTTP proxy to {}:{}.", host, port);
        });
        return builder.build();
    }

    public static <T> T newProxy(final ResteasyClient client, final RoboZonkyFilter filter, final Class<T> api,
                                 final String url) {
        return client.target(url).register(filter).proxy(api);
    }

    public static <T> T newProxy(final ResteasyClient client, final Class<T> api, final String url) {
        return client.target(url).proxy(api);
    }
}

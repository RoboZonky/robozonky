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

package com.github.robozonky.internal.remote;

import com.github.robozonky.internal.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

import javax.ws.rs.client.ClientBuilder;
import java.util.concurrent.TimeUnit;

final class ProxyFactory {

    private static final Logger LOGGER = LogManager.getLogger(ProxyFactory.class);

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
        final ResteasyClientBuilder builder = ((ResteasyClientBuilder)ClientBuilder.newBuilder())
                .useAsyncHttpEngine()
                .readTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
        /*
         * setup HTTP proxy when required (see
         * http://docs.jboss.org/resteasy/docs/4.0.0.Final/userguide/html/RESTEasy_Client_Framework.html#http_proxy)
         */
        settings.getHttpsProxyHostname().ifPresent(host -> {
            final int port = settings.getHttpsProxyPort();
            builder.property("org.jboss.resteasy.jaxrs.client.proxy.host", host)
                    .property("org.jboss.resteasy.jaxrs.client.proxy.port", port);
            LOGGER.debug("Set HTTP proxy to {}:{}.", host, port);
        });
        return builder.build();
    }

    public static <T> T newProxy(final ResteasyClient client, final RoboZonkyFilter filter, final Class<T> api,
                                 final String url) {
        return client.target(url).register(filter).proxy(api);
    }

}

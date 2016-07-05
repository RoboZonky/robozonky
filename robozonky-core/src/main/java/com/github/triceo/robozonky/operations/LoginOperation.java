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

package com.github.triceo.robozonky.operations;

import com.github.triceo.robozonky.Util;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.authentication.Authenticator;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginOperation extends Operation<Authenticator, Authentication> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginOperation.class);

    private static final ResteasyProviderFactory RESTEASY;
    static {
        LoginOperation.LOGGER.trace("Initializing RESTEasy.");
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(LoginOperation.RESTEASY);
        RESTEASY.registerProvider(ResteasyJackson2Provider.class);
        LoginOperation.LOGGER.trace("RESTEasy initialized.");
    }

    private static final String ZONKY_URL = "https://api.zonky.cz";
    private static final String ZOTIFY_URL = "http://zotify.cz";

    @Override
    protected Logger getLogger() {
        return LoginOperation.LOGGER;
    }

    @Override
    protected Authentication perform(final Authenticator input) {
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
        clientBuilder.providerFactory(LoginOperation.RESTEASY);
        final Authentication auth = input.authenticate(LoginOperation.ZONKY_URL, LoginOperation.ZOTIFY_URL,
                Util.getRoboZonkyVersion(), clientBuilder);
        return auth;

    }
}

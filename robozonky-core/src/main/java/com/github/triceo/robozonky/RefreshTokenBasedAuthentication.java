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

import com.github.triceo.robozonky.remote.Authorization;
import com.github.triceo.robozonky.remote.Token;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RefreshTokenBasedAuthentication extends Authentication {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshTokenBasedAuthentication.class);

    private final String username;
    private final Token token;

    RefreshTokenBasedAuthentication(final String username, final Token token) {
        this.username = username;
        this.token = token;
    }

    @Override
    public Token authenticate(final ResteasyClientBuilder clientBuilder) {
        final ResteasyClient client = clientBuilder.build();
        client.register(new AuthorizationFilter());
        final Authorization auth = client.target(Operations.ZONKY_URL).proxy(Authorization.class);
        final Token token = auth.refresh(this.token.getRefreshToken(), "refresh_token", "SCOPE_APP_WEB");
        RefreshTokenBasedAuthentication.LOGGER.info("Logged in with Zonky as user '{}' using refresh token.", username);
        client.close();
        return token;
    }
}

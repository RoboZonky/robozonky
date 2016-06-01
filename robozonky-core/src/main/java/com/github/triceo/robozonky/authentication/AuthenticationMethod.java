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

package com.github.triceo.robozonky.authentication;

import java.util.function.Function;

import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthenticationMethod {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationMethod.class);
    private static final String TARGET_SCOPE = "SCOPE_APP_WEB";

    public static AuthenticationMethod withCredentials(final String username, final String password) {
        return new AuthenticationMethod((ZonkyApi api) -> {
            final ZonkyApiToken token = api.login(username, password, "password", AuthenticationMethod.TARGET_SCOPE);
            AuthenticationMethod.LOGGER.info("Logged in with Zonky as user '{}' using password.", username);
            return token;
        });
    }

    public static AuthenticationMethod withRefreshToken(final String username, final ZonkyApiToken token) {
        return new AuthenticationMethod((ZonkyApi api) -> {
            final String tokenId = token.getRefreshToken();
            final ZonkyApiToken newToken = api.refresh(tokenId, "refresh_token", AuthenticationMethod.TARGET_SCOPE);
            AuthenticationMethod.LOGGER.info("Logged in with Zonky as user '{}' using refresh token.", username);
            return newToken;
        });
    }

    private static Authenticated newAuthenticatedApi(final ZonkyApiToken token, final String zonkyApiUrl,
                                                          final ResteasyClientBuilder clientBuilder) {
        final ZonkyApi api = AuthenticationMethod.newApi(zonkyApiUrl, clientBuilder, new AuthenticatedFilter(token));
        return new Authenticated(api, token);
    }

    private static ZonkyApi newApi(final String zonkyApiUrl, final ResteasyClientBuilder clientBuilder,
                                   final CommonFilter filter) { // FIXME clients are never closed
        final ResteasyClient client = clientBuilder.build();
        client.register(filter);
        return client.target(zonkyApiUrl).proxy(ZonkyApi.class);
    }

    private final Function<ZonkyApi, ZonkyApiToken> authenticationMethod;

    public AuthenticationMethod(final Function<ZonkyApi, ZonkyApiToken> authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public Authenticated authenticate(final String zonkyApiUrl, final ResteasyClientBuilder clientBuilder) {
        final ZonkyApi api = AuthenticationMethod.newApi(zonkyApiUrl, clientBuilder, new AuthenticationFilter());
        final ZonkyApiToken token = authenticationMethod.apply(api);
        return AuthenticationMethod.newAuthenticatedApi(token, zonkyApiUrl, clientBuilder);

    }

}

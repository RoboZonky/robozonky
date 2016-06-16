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

import com.github.triceo.robozonky.remote.Api;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);
    private static final String TARGET_SCOPE = "SCOPE_APP_WEB";

    public static Authenticator withCredentials(final String username, final String password) {
        return new Authenticator((ZonkyApi api) -> {
            final ZonkyApiToken token = api.login(username, password, "password", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' using password.", username);
            return token;
        });
    }

    public static Authenticator withAccessToken(final String username, final ZonkyApiToken token) {
        return new Authenticator((ZonkyApi api) -> {
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' with existing access token.", username);
            return token;
        });
    }

    public static Authenticator withAccessTokenAndRefresh(final String username, final ZonkyApiToken token) {
        return new Authenticator((ZonkyApi api) -> {
            final String tokenId = token.getRefreshToken();
            final ZonkyApiToken newToken = api.refresh(tokenId, "refresh_token", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}', refreshing existing access token.",
                    username);
            return newToken;
        });
    }

    private static Authentication newAuthenticatedApi(final ZonkyApiToken token, final String zonkyApiUrl,
                                                      final String zotifyApiUrl, final String roboZonkyVersion,
                                                      final ResteasyClientBuilder clientBuilder) {
        final ZonkyApi api = Authenticator.newApi(zonkyApiUrl, clientBuilder,
                new AuthenticatedFilter(token, roboZonkyVersion), ZonkyApi.class);
        final ZotifyApi zotifyApi =
                Authenticator.newApi(zotifyApiUrl, clientBuilder, new ZotifyFilter(roboZonkyVersion), ZotifyApi.class);
        return new Authentication(api, token, zotifyApi);
    }

    private static <T extends Api> T newApi(final String zonkyApiUrl, final ResteasyClientBuilder clientBuilder,
                                            final CommonFilter filter, Class<T> api) { // FIXME clients are never closed
        final ResteasyClient client = clientBuilder.build();
        client.register(filter);
        return client.target(zonkyApiUrl).proxy(api);
    }

    private final Function<ZonkyApi, ZonkyApiToken> authenticationMethod;

    private Authenticator(final Function<ZonkyApi, ZonkyApiToken> authenticationMethod) {
        if (authenticationMethod == null) {
            throw new IllegalArgumentException("Authentication method must be provided.");
        }
        this.authenticationMethod = authenticationMethod;
    }

    public Authentication authenticate(final String zonkyApiUrl, final String zotifyApiUrl,
                                       final String roboZonkyVersion, final ResteasyClientBuilder clientBuilder) {
        final ZonkyApi api = Authenticator.newApi(zonkyApiUrl, clientBuilder,
                new AuthenticationFilter(roboZonkyVersion), ZonkyApi.class);
        final ZonkyApiToken token = authenticationMethod.apply(api);
        return Authenticator.newAuthenticatedApi(token, zonkyApiUrl, zotifyApiUrl, roboZonkyVersion, clientBuilder);
    }

}

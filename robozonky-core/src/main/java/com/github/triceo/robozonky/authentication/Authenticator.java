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
import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZonkyApiToken;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to authenticate to the Zonky API. Use either {@link #withAccessToken(String, ZonkyApiToken, boolean)},
 * {@link #withAccessTokenAndRefresh(String, ZonkyApiToken, boolean)} or
 * {@link #withCredentials(String, String, boolean)} to log in.
 */
public class Authenticator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);
    private static final String TARGET_SCOPE = "SCOPE_APP_WEB";

    private static final ResteasyProviderFactory RESTEASY;
    static {
        Authenticator.LOGGER.trace("Initializing RESTEasy.");
        RESTEASY = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register(Authenticator.RESTEASY);
        RESTEASY.registerProvider(ResteasyJackson2Provider.class);
        Authenticator.LOGGER.trace("RESTEasy initialized.");
    }

    /**
     * Prepare for authentication using username and password.
     *
     * @param username Zonky username.
     * @param password Zonky password.
     * @param isDryRun Whether or not we are authenticating for a dry run.
     * @return Instance ready for authentication.
     */
    public static Authenticator withCredentials(final String username, final String password, final boolean isDryRun) {
        return new Authenticator((ZonkyApi api) -> {
            final ZonkyApiToken token = api.login(username, password, "password", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' using password.", username);
            return token;
        }, false, isDryRun);
    }

    /**
     * Prepare for authentication using the Zonky OAuth token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @param isDryRun Whether or not we are authenticating for a dry run.
     * @return Instance ready for authentication.
     */
    public static Authenticator withAccessToken(final String username, final ZonkyApiToken token,
                                                final boolean isDryRun) {
        return new Authenticator((ZonkyApi api) -> {
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}' with existing access token.", username);
            return token;
        }, true, isDryRun);
    }

    /**
     * Prepare for authentication using the Zonky OAuth token which will also refresh the token.
     *
     * @param username Zonky username.
     * @param token OAuth token.
     * @param isDryRun Whether or not we are authenticating for a dry run.
     * @return Instance ready for authentication.
     */
    public static Authenticator withAccessTokenAndRefresh(final String username, final ZonkyApiToken token,
                                                          final boolean isDryRun) {
        return new Authenticator((ZonkyApi api) -> {
            final String tokenId = token.getRefreshToken();
            final ZonkyApiToken newToken = api.refresh(tokenId, "refresh_token", Authenticator.TARGET_SCOPE);
            Authenticator.LOGGER.info("Logged in with Zonky as user '{}', refreshing existing access token.", username);
            return newToken;
        }, true, isDryRun);
    }

    private static Authentication newAuthenticatedApi(final ZonkyApiToken token, final String zonkyApiUrl,
                                                      final String zotifyApiUrl,
                                                      final ResteasyClientBuilder clientBuilder,
                                                      final boolean isDryRun) {
        final AuthenticatedFilter f = new AuthenticatedFilter(token);
        final ZonkyApi api = isDryRun ?
                Authenticator.newApi(zonkyApiUrl, clientBuilder, f, ZonkyApi.class):
                Authenticator.newApi(zonkyApiUrl, clientBuilder, f, InvestingZonkyApi.class);
        final ZotifyApi zotifyApi =
                Authenticator.newApi(zotifyApiUrl, clientBuilder, new ZotifyFilter(), ZotifyApi.class);
        return new Authentication(api, token, zotifyApi);
    }

    private static <T extends Api> T newApi(final String zonkyApiUrl, final ResteasyClientBuilder clientBuilder,
                                            final CommonFilter filter, final Class<T> api) {
        // FIXME clients are never closed
        final ResteasyClient client = clientBuilder.build();
        client.register(filter);
        return client.target(zonkyApiUrl).proxy(api);
    }

    private final Function<ZonkyApi, ZonkyApiToken> authenticationMethod;
    private final boolean tokenBased, isDryRun;

    private Authenticator(final Function<ZonkyApi, ZonkyApiToken> authenticationMethod, final boolean tokenBased,
                          final boolean isDryRun) {
        if (authenticationMethod == null) {
            throw new IllegalArgumentException("Authentication method must be provided.");
        }
        this.authenticationMethod = authenticationMethod;
        this.tokenBased = tokenBased;
        this.isDryRun = isDryRun;
    }

    /**
     * Whether or not this particular authentication will use the Zonky OAuth access token.
     * @return True if token is being used.
     */
    public boolean isTokenBased() {
        return tokenBased;
    }

    /**
     * Perform the actual authentication.
     * @param zonkyApiUrl URL to use to connect to Zonky.
     * @param zotifyApiUrl URL to use to connect to the Zotify marketplace cache.
     * @param clientBuilder The builder to use for the RESTEasy client proxy.
     * @return Information about the authentication.
     */
    Authentication authenticate(final String zonkyApiUrl, final String zotifyApiUrl,
                                final ResteasyClientBuilder clientBuilder) {
        final ZonkyApi api = Authenticator.newApi(zonkyApiUrl, clientBuilder, new AuthenticationFilter(),
                ZonkyApi.class);
        final ZonkyApiToken token = authenticationMethod.apply(api);
        return Authenticator.newAuthenticatedApi(token, zonkyApiUrl, zotifyApiUrl, clientBuilder, isDryRun);
    }

    /**
     * Perform the actual authentication. Will throw an unchecked exception in case authentication failed.
     * @param zonkyApiUrl URL to use to connect to Zonky.
     * @param zotifyApiUrl URL to use to connect to the Zotify marketplace cache.
     * @return Information about the authentication.
     */
    public Authentication authenticate(final String zonkyApiUrl, final String zotifyApiUrl) {
        final ResteasyClientBuilder clientBuilder = new ResteasyClientBuilder();
        clientBuilder.providerFactory(Authenticator.RESTEASY);
        return this.authenticate(zonkyApiUrl, zotifyApiUrl, clientBuilder);
    }

}

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

import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.enums.OAuthScope;

public class OAuth {

    private final Api<ZonkyOAuthApi> api;

    OAuth(final Api<ZonkyOAuthApi> api) {
        this.api = api;
    }

    public ZonkyApiToken login(final String username, final char[] password) {
        return login(OAuthScope.SCOPE_APP_WEB, username, password);
    }

    public ZonkyApiToken login(final OAuthScope scope, final String username, final char[] password) {
        return api.call(a -> a.login(username, String.valueOf(password), "password", scope));
    }

    public ZonkyApiToken refresh(final ZonkyApiToken token) {
        final OAuthScope scope = token.getScope().getPrimaryScope().orElse(OAuthScope.SCOPE_APP_WEB);
        return api.call(a -> a.refresh(String.valueOf(token.getRefreshToken()), ZonkyApiToken.REFRESH_TOKEN_STRING,
                                       scope));
    }
}

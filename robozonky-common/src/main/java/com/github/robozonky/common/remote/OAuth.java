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

import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;

public class OAuth {

    private final Api<ZonkyOAuthApi> api;

    OAuth(final Api<ZonkyOAuthApi> api) {
        this.api = api;
    }

    public ZonkyApiToken login(final String username, final char[] password) {
        return api.execute(a -> {
            return a.login(username, String.valueOf(password), "password", "SCOPE_APP_WEB");
        });
    }

    public ZonkyApiToken refresh(final ZonkyApiToken token) {
        return api.execute(a -> {
            return a.refresh(String.valueOf(token.getRefreshToken()), "refresh_token", token.getScope());
        });
    }
}

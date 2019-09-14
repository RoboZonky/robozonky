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

import com.github.robozonky.api.remote.ZonkyOAuthApi;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;

import static com.github.robozonky.api.remote.entities.ZonkyApiToken.REFRESH_TOKEN_STRING;

public class OAuth {

    private final Api<ZonkyOAuthApi> api;

    OAuth(final Api<ZonkyOAuthApi> api) {
        this.api = api;
    }

    private static String toString(final char... code) {
        return String.valueOf(code);
    }

    public ZonkyApiToken login(final char[] code) {
        return api.call(a -> a.login(toString(code), "https://app.zonky.cz/api/oauth/code", "authorization_code"));
    }

    public ZonkyApiToken refresh(final ZonkyApiToken token) {
        return api.call(a -> a.refresh(toString(token.getRefreshToken()), REFRESH_TOKEN_STRING));
    }
}

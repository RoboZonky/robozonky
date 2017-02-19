/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.authentication;

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;

import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.internal.api.RoboZonkyFilter;

final class AuthenticatedFilter extends RoboZonkyFilter {

    private final char[] accessToken; // treat the access token as if it were a password

    public AuthenticatedFilter(final ZonkyApiToken token) {
        this.accessToken = token.getAccessToken();
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().add("Authorization", "Bearer " + String.valueOf(accessToken));
        super.filter(clientRequestContext);
    }
}

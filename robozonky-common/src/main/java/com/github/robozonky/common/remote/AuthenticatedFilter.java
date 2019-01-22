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

import java.util.function.Supplier;
import javax.ws.rs.client.ClientRequestContext;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;

class AuthenticatedFilter extends RoboZonkyFilter {

    private static final char[] EMPTY_TOKEN = new char[0];
    private final Supplier<ZonkyApiToken> token;

    public AuthenticatedFilter(final Supplier<ZonkyApiToken> token) {
        // null token = no token
        this.token = token;
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) {
        final ZonkyApiToken supplied = token == null ? null : token.get();
        logger.trace("Using token #{}.", supplied);
        final char[] t = supplied == null ? AuthenticatedFilter.EMPTY_TOKEN : supplied.getAccessToken();
        this.setRequestHeader("Authorization", "Bearer " + String.valueOf(t));
        super.filter(clientRequestContext);
    }
}

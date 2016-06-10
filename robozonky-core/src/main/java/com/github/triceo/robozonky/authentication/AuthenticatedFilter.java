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

import java.io.IOException;
import javax.ws.rs.client.ClientRequestContext;

import com.github.triceo.robozonky.remote.ZonkyApiToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticatedFilter extends CommonFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticatedFilter.class);

    private final ZonkyApiToken authorization;

    public AuthenticatedFilter(final ZonkyApiToken token, final String roboZonkyVersion) {
        super(roboZonkyVersion);
        this.authorization = token;
    }

    @Override
    protected Logger getLogger() {
        return AuthenticatedFilter.LOGGER;
    }

    @Override
    public void filter(final ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().add("Authorization", "Bearer " + this.authorization.getAccessToken());
        super.filter(clientRequestContext);
    }
}

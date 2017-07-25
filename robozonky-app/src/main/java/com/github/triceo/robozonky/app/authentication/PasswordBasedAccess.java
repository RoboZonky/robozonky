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

import java.util.Collection;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PasswordBasedAccess implements Authenticated {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordBasedAccess.class);

    private final SecretProvider secrets;
    private final ApiProvider apis;

    PasswordBasedAccess(final ApiProvider apis, final SecretProvider secrets) {
        this.secrets = secrets;
        this.apis = apis;
    }

    @Override
    public Collection<Investment> execute(final Function<Zonky, Collection<Investment>> op) {
        try (final OAuth oauth = apis.oauth()) {
            final ZonkyApiToken token = oauth.login(secrets.getUsername(), secrets.getPassword());
            try (final Zonky zonky = apis.authenticated(token)) {
                try {
                    return op.apply(zonky);
                } finally { // attempt to log out no matter what happens
                    LOGGER.info("Logging out.");
                    zonky.logout();
                }
            }
        }
    }

    @Override
    public SecretProvider getSecretProvider() {
        return secrets;
    }
}

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

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.common.secrets.SecretProvider;

public interface Authenticated {

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, but the session may expire if not refreshed regularly. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
     * <p>
     * The token will only be refreshed if RoboZonky is launched between token expiration and X second before token
     * expiration, where X comes from the arguments of this method.
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @param refreshAfter Access token will be refreshed after expiration minus this.
     * @return This.
     */
    static Authenticated tokenBased(final SecretProvider data, final TemporalAmount refreshAfter) {
        return Authenticated.tokenBased(new ApiProvider(), data, refreshAfter);
    }

    static Authenticated tokenBased(final ApiProvider apis, final SecretProvider data,
                                    final TemporalAmount refreshAfter) {
        return new TokenBasedAccess(apis, data, refreshAfter);
    }

    /**
     * Build authentication mechanism that will log out at the end of RoboZonky's operations. This will ignore the
     * access tokens.
     * @param data Provider for the sensitive information, such as passwords and tokens.
     * @return The desired authentication method.
     */
    static Authenticated passwordBased(final SecretProvider data) {
        return Authenticated.passwordBased(new ApiProvider(), data);
    }

    static Authenticated passwordBased(final ApiProvider apis, final SecretProvider data) {
        return new PasswordBasedAccess(apis, data);
    }

    Collection<Investment> execute(Function<Zonky, Collection<Investment>> operation);

    SecretProvider getSecretProvider();
}

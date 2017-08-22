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

package com.github.robozonky.app.authentication;

import java.time.temporal.TemporalAmount;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;

public interface Authenticated {

    /**
     * Build authentication mechanism that will keep the session alive via the use of session token. The mechanism will
     * never log out, constantly refreshing the session in the background. This is potentially unsafe, as it will
     * eventually store a plain-text access token on the hard drive, for everyone to see.
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

    <T> T call(Function<Zonky, T> operation);

    default void run(final Consumer<Zonky> operation) {
        call(z -> {
            operation.accept(z);
            return null;
        });
    }

    SecretProvider getSecretProvider();
}

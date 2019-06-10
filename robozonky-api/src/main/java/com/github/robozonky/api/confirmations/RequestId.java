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

package com.github.robozonky.api.confirmations;

import java.util.Arrays;
import java.util.Objects;

/**
 * Identification of this instance of RoboZonky to the remote confirmation endpoint.
 */
@Deprecated(forRemoval = true, since = "5.3.0")
public final class RequestId {

    private static char[] makeDefensiveCopy(final char... password) {
        if (password.length == 0) { // empty arrays are immutable, no need to copy
            return password;
        }
        return Arrays.copyOf(password, password.length);
    }

    private final String userId;
    private final char[] password;

    public RequestId(final String userId, final char... password) {
        if (userId == null) {
            throw new IllegalArgumentException("Username must not be null.");
        }
        this.userId = userId;
        this.password = RequestId.makeDefensiveCopy(password);
    }

    public String getUserId() {
        return userId;
    }

    public char[] getPassword() {
        return RequestId.makeDefensiveCopy(password);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof RequestId)) {
            return false;
        }
        final RequestId that = (RequestId) o;
        return Objects.equals(userId, that.userId) && Arrays.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, password);
    }

    @Override
    public String toString() {
        return "Authentication{" +
                "userId='" + userId + '\'' +
                '}';
    }
}

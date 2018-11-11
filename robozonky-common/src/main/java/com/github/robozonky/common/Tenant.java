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

package com.github.robozonky.common;

import java.io.Closeable;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.util.StreamUtil;

public interface Tenant extends Closeable {

    /**
     * Execute an operation using on the Zonky server, using the default scope.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param <T> Return type of the operation.
     * @return Whatever the operation returned.
     */
    default <T> T call(final Function<Zonky, T> operation) {
        return call(operation, ZonkyScope.getDefault());
    }

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param scope The scope of access to request with the Zonky server.
     * @param <T> Return type of the operation.
     * @return Whatever the operation returned.
     */
    <T> T call(Function<Zonky, T> operation, ZonkyScope scope);

    /**
     * Execute an operation using on the Zonky server, using the default scope.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     */
    default void run(final Consumer<Zonky> operation) {
        run(operation, ZonkyScope.getDefault());
    }

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param scope The scope of access to request with the Zonky server.
     */
    default void run(final Consumer<Zonky> operation, final ZonkyScope scope) {
        call(StreamUtil.toFunction(operation), scope);
    }

    /**
     * Check that the tenant can be operated on, using the default scope.
     * @return False in cases such as when the user's authentication credentials are being refreshed and therefore
     * the present authentication may already be invalid, without the new one being available yet.
     */
    default boolean isAvailable() {
        return isAvailable(ZonkyScope.getDefault());
    }

    /**
     * Check that the tenant can be operated on.
     * @param scope The scope of access with the Zonky server.
     * @return False in cases such as when the user's authentication credentials are being refreshed and therefore
     * the present authentication may already be invalid, without the new one being available yet.
     */
    boolean isAvailable(ZonkyScope scope);

    Restrictions getRestrictions();

    SessionInfo getSessionInfo();

    SecretProvider getSecrets();

    default <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo()).in(clz);
    }
}

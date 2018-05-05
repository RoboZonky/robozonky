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

import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.util.StreamUtil;

public interface Tenant {

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be as stateless as possible, since it may be executed repeatedly
     * in response to stale auth token issues.
     * @param <T> Return type of the operation.
     * @return Whatever the operation returned.
     */
    <T> T call(Function<Zonky, T> operation);

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be as stateless as possible, since it may be executed repeatedly
     * in response to stale auth token issues.
     */
    default void run(final Consumer<Zonky> operation) {
        call(StreamUtil.toFunction(operation));
    }

    Restrictions getRestrictions();

    SessionInfo getSessionInfo();

    default <T> InstanceState<T> getState(final Class<T> clz) {
        return TenantState.of(getSessionInfo()).in(clz);
    }
}

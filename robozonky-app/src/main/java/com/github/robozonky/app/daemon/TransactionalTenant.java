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

package com.github.robozonky.app.daemon;

import java.io.IOException;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.state.InstanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link #getState(Class)} returns {@link TransactionalInstanceState} instead of the default {@link InstanceState}
 * implementation. Every other method delegated to the default {@link Tenant} implementation.
 */
final class TransactionalTenant implements Tenant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalTenant.class);
    private final Tenant parent;
    private final Transactional transactional;

    public TransactionalTenant(final Transactional transactional, final Tenant parent) {
        this.transactional = transactional;
        this.parent = parent;
    }

    @Override
    public <T> T call(final Function<Zonky, T> operation, final ZonkyScope scope) {
        return parent.call(operation, scope);
    }

    @Override
    public boolean isAvailable(final ZonkyScope scope) {
        return parent.isAvailable(scope);
    }

    @Override
    public Restrictions getRestrictions() {
        return parent.getRestrictions();
    }

    @Override
    public SessionInfo getSessionInfo() {
        return parent.getSessionInfo();
    }

    @Override
    public SecretProvider getSecrets() {
        return parent.getSecrets();
    }

    @Override
    public <T> InstanceState<T> getState(final Class<T> clz) {
        LOGGER.trace("Creating transactional instance state for {}.", clz);
        return new TransactionalInstanceState<>(transactional, parent.getState(clz));
    }

    @Override
    public void close() throws IOException {
        parent.close();
    }
}

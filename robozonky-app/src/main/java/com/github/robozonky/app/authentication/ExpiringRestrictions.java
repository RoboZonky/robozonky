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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.util.Expiring;

final class ExpiringRestrictions extends Expiring<Restrictions> {

    private final Tenant tenant;

    public ExpiringRestrictions(final Tenant tenant) {
        super(Duration.ofHours(1));
        this.tenant = tenant;
    }

    @Override
    protected Optional<Restrictions> retrieve() {
        try {
            return Optional.of(tenant.call(Zonky::getRestrictions));
        } catch (final Exception ex) {
            LOGGER.debug("Failed reading Restrictions from Zonky.", ex);
            return Optional.empty();
        }
    }
}

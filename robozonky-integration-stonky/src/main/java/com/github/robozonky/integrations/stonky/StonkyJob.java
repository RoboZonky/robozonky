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

package com.github.robozonky.integrations.stonky;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.jobs.TenantJob;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.internal.api.Defaults;

final class StonkyJob implements TenantJob {

    private final Consumer<Tenant> stonky;

    public StonkyJob() {
        this(arg -> new Stonky().apply(arg));
    }

    StonkyJob(final Consumer<Tenant> provider) {
        this.stonky = provider;
    }

    /**
     * @return When added to the current time, will result in some random time shortly after midnight tomorrow.
     */
    @Override
    public Duration startIn() {
        final Duration random = TenantJob.super.startIn();
        final ZonedDateTime triggerOn = LocalDate.now().plusDays(1).atStartOfDay(Defaults.ZONE_ID).plus(random);
        return Duration.between(ZonedDateTime.now(), triggerOn);
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofHours(24);
    }

    @Override
    public Duration killIn() {
        return Duration.ofMinutes(30);
    }

    @Override
    public TenantPayload payload() {
        return new StonkyPayload();
    }

    private final class StonkyPayload implements TenantPayload {

        @Override
        public void accept(final Tenant secretProvider) {
            stonky.accept(secretProvider);
        }
    }
}

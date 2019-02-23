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

package com.github.robozonky.app.daemon;

import java.time.Duration;

import com.github.robozonky.common.jobs.TenantJob;
import com.github.robozonky.common.jobs.TenantPayload;

final class ReservationsProcessingJob implements TenantJob {

    @Override
    public TenantPayload payload() {
        return new ReservationsProcessing();
    }

    @Override
    public Duration startIn() {
        return Duration.ofHours(2);
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofHours(4);
    }

    @Override
    public boolean prioritize() {
        return false; // this is a time-sensitive and infrequent feature, so prioritize
    }
}

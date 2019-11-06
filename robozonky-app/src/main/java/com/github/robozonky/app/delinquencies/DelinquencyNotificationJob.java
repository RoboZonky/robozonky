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

package com.github.robozonky.app.delinquencies;

import com.github.robozonky.internal.jobs.TenantJob;
import com.github.robozonky.internal.jobs.TenantPayload;

import java.time.Duration;

class DelinquencyNotificationJob implements TenantJob {

    private final TenantPayload payload = new DelinquencyNotificationPayload();

    @Override
    public Duration repeatEvery() {
        return Duration.ofHours(12);
    }

    @Override
    public TenantPayload payload() {
        return payload;
    }

    @Override
    public Duration killIn() {
        return Duration.ofMinutes(5); // possibly a lot of investments, and the endpoint itself is slow
    }

}

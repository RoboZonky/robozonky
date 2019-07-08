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

import com.github.robozonky.internal.jobs.TenantJob;
import com.github.robozonky.internal.jobs.TenantPayload;

final class SellingJob implements TenantJob {

    @Override
    public TenantPayload payload() {
        return new Selling();
    }

    @Override
    public Duration killIn() {
        return Duration.ofMinutes(10);
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofHours(2); // to match the remote Zonky portfolio recalc
    }

    @Override
    public boolean prioritize() {
        return true; // this is a key feature of the application and only happens occasionally, so prioritize
    }
}

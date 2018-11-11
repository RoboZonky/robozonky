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

package com.github.robozonky.app.version;

import java.time.Duration;

import com.github.robozonky.common.jobs.SimpleJob;
import com.github.robozonky.common.jobs.SimplePayload;

public class VersionDetectionJob implements SimpleJob {

    private final SimplePayload payload = new UpdateMonitor();

    @Override
    public Duration startIn() {
        return Duration.ZERO;
    }

    @Override
    public Duration repeatEvery() {
        return Duration.ofDays(1);
    }

    @Override
    public SimplePayload payload() {
        return payload;
    }
}

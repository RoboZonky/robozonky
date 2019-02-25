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

package com.github.robozonky.app.events;

import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.common.jobs.JobService;
import com.github.robozonky.common.jobs.SimpleJob;
import com.github.robozonky.common.jobs.TenantJob;

/**
 * Reads {@link EventFiringQueue} in regular intervals and fires the stored {@link Event}s using {@link EventFiring}.
 */
public final class EventFiringJobService implements JobService {

    private static final SimpleJob INSTANCE = new EventFiringJob();

    @Override
    public Collection<SimpleJob> getSimpleJobs() {
        return Collections.singleton(INSTANCE);
    }

    @Override
    public Collection<TenantJob> getTenantJobs() {
        return Collections.emptyList();
    }
}

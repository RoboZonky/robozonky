/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.jobs;

import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * Unlike {@link TenantJob}, this {@link Job} does not require a {@link Tenant}. Therefore, operations within this job
 * are not subject to {@link Availability} and will not report to the user in case they fail. It is therefore ideal for
 * inward-facing operations of the robot.
 */
public interface SimpleJob extends Job {

    /**
     * The task to run.
     * 
     * @return never null
     */
    SimplePayload payload();
}

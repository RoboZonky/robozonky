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

package com.github.robozonky.app.daemon;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.github.robozonky.internal.jobs.JobService;
import com.github.robozonky.internal.jobs.SimpleJob;
import com.github.robozonky.internal.jobs.TenantJob;

public final class SellingJobService implements JobService {

    private static final SellingJob SELLING_INSTANCE = new SellingJob();
    private static final SaleCheckJob CHECKING_INSTANCE = new SaleCheckJob();

    @Override
    public Collection<SimpleJob> getSimpleJobs() {
        return Collections.emptyList();
    }

    @Override
    public Collection<TenantJob> getTenantJobs() {
        return Arrays.asList(SELLING_INSTANCE, CHECKING_INSTANCE);
    }

}

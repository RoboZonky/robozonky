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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.test.TestingTenant;

class TestingEventTenant extends TestingTenant implements PowerTenant {

    public TestingEventTenant(final SessionInfo sessionInfo, final Zonky zonky) {
        super(sessionInfo, zonky);
    }

    @Override
    public Runnable fire(final SessionEvent event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public Runnable fire(final LazyEvent<? extends SessionEvent> event) {
        return Events.forSession(this).fire(event);
    }
}

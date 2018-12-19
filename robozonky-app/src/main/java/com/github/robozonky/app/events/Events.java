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

package com.github.robozonky.app.events;

import com.github.robozonky.api.notifications.GlobalEvent;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.tenant.Tenant;

public interface Events {

    /**
     * Used to fire {@link GlobalEvent}s, will reach all {@link Tenant}s in the system.
     * @return
     */
    static GlobalEvents global() {
        return GlobalEvents.get();
    }

    /**
     * Used to request {@link SessionEvent}s, will reach only the given {@link Tenant}. You should use
     * {@link PowerTenant} to fire those.
     * @param tenant The {@link Tenant} to reach. If you don't have the instance, you have no business firing the event.
     * @return
     */
    static SessionEvents forSession(final PowerTenant tenant) {
        return SessionEvents.forSession(tenant.getSessionInfo());
    }
}

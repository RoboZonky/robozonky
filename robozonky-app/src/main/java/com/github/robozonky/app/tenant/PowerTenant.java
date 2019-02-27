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

package com.github.robozonky.app.tenant;

import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.common.tenant.Tenant;

/**
 * This is a {@link Tenant} extension which allows to easily fire {@link SessionEvent}s. Events are fired when requested
 * via methods {@link #fire(SessionEvent)} or {@link #fire(LazyEvent)} and can not be rolled back.
 * <p>
 * Instances of this interface should never get to users outside of the application, otherwise they would be able to
 * fire events. All user-facing code should see just the plain {@link Tenant}.
 */
public interface PowerTenant extends Tenant {

    /**
     * Upgrade {@link Tenant} from the semantics of {@link PowerTenant} to the semantics of
     * {@link TransactionalPowerTenant}.
     * @param tenant Tenant to convert.
     * @return If the instance already is a {@link TransactionalPowerTenant}, the same instance is returned.
     */
    static TransactionalPowerTenant transactional(final PowerTenant tenant) {
        if (tenant instanceof TransactionalPowerTenant) {
            return (TransactionalPowerTenant) tenant;
        }
        return new TransactionalPowerTenantImpl(tenant);
    }

    /**
     * See {@link SessionEvents#fire(SessionEvent)} for the semantics of this method.
     *
     * @param event
     * @return
     */
    Runnable fire(SessionEvent event);

    /**
     * See {@link SessionEvents#fire(LazyEvent)} for the semantics of this method.
     * @param event
     * @return
     */
    Runnable fire(LazyEvent<? extends SessionEvent> event);
}

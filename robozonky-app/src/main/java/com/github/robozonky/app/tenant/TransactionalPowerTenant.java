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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.common.tenant.TransactionalTenant;

/**
 * This add the semantics of {@link TransactionalTenant} to firing {@link Event}s as well. Unless {@link #commit()} is
 * called, no events are fired and no state is persisted. All unpersisted changes can be rolled back via
 * {@link #abort()}. {@link #close()} will throw an {@link IllegalStateException} unless {@link #commit()} or
 * {@link #abort()} is called beforehand - this is to prevent the applications from leaving uncommitted data in there.
 * <p>
 * Every method not related to events or state will be delegated to the underlying {@link Tenant}.
 * <p>
 * Instances of this interface should never get to users outside of the application, otherwise they would be able to
 * fire events. All user-facing code should see just the plain {@link TransactionalTenant}.
 */
public interface TransactionalPowerTenant extends TransactionalTenant,
                                                  PowerTenant {

    /**
     * Do not block on the return value of this method, unless some other thread is still able to call
     * {@link #commit()}. Otherwise this is a self-inflicted. deadlock.
     * @param event
     * @return
     */
    @Override
    Runnable fire(SessionEvent event);

    /**
     * Do not block on the return value of this method, unless some other thread is still able to call
     * {@link #commit()}. Otherwise this is a self-inflicted. deadlock.
     * @param event
     * @return
     */
    @Override
    Runnable fire(LazyEvent<? extends SessionEvent> event);

}

/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.notifications.email;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.util.Scheduler;

public final class EmailListenerService implements ListenerService {

    private static final RefreshableNotificationProperties PROPERTIES = new RefreshableNotificationProperties();
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    @Override
    public <T extends Event> EventListenerSupplier<T> findListener(final Class<T> eventType) {
        if (IS_INITIALIZED.getAndSet(true)) {
            Scheduler.inBackground().submit(PROPERTIES, Settings.INSTANCE.getRemoteResourceRefreshInterval());
        }
        final EmailingEventListenerSupplier<T> l = new EmailingEventListenerSupplier<>(eventType);
        PROPERTIES.registerListener(l);
        return l;
    }
}

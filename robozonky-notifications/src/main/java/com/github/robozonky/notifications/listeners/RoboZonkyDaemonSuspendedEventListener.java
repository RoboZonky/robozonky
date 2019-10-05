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

package com.github.robozonky.notifications.listeners;

import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

import java.util.Collections;
import java.util.Map;

public class RoboZonkyDaemonSuspendedEventListener extends AbstractListener<RoboZonkyDaemonSuspendedEvent> {

    public RoboZonkyDaemonSuspendedEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    public String getSubject(final RoboZonkyDaemonSuspendedEvent event) {
        return "Uvnitř RoboZonky došlo k chybě!";
    }

    @Override
    public String getTemplateFileName() {
        return "daemon-suspended.ftl";
    }

    @Override
    protected Map<String, Object> getData(final RoboZonkyDaemonSuspendedEvent event) {
        return Collections.singletonMap("cause", Util.stackTraceToString(event.getCause()));
    }
}

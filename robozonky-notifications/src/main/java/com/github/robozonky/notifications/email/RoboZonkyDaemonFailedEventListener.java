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

import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;

class RoboZonkyDaemonFailedEventListener extends AbstractEmailingListener<RoboZonkyDaemonFailedEvent> {

    public RoboZonkyDaemonFailedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final RoboZonkyDaemonFailedEvent event) {
        return "Uvnitř RoboZonky došlo k chybě!";
    }

    @Override
    String getTemplateFileName() {
        return "daemon-failed.ftl";
    }

    @Override
    protected Map<String, Object> getData(final RoboZonkyDaemonFailedEvent event) {
        return Collections.singletonMap("cause", Util.stackTraceToString(event.getCause()));
    }
}

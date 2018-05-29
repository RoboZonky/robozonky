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

import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.notifications.configuration.ListenerSpecificNotificationProperties;

class RoboZonkyTestingEventListener extends AbstractEmailingListener<RoboZonkyTestingEvent> {

    public RoboZonkyTestingEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final RoboZonkyTestingEvent event) {
        return "Zkušební notifikace z RoboZonky!";
    }

    @Override
    String getTemplateFileName() {
        return "testing.ftl";
    }
}

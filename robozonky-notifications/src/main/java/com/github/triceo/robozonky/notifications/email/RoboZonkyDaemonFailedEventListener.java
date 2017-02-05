/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import com.github.triceo.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;

class RoboZonkyDaemonFailedEventListener extends AbstractEmailingListener<RoboZonkyDaemonFailedEvent> {

    public RoboZonkyDaemonFailedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    boolean shouldSendEmail(final RoboZonkyDaemonFailedEvent event) {
        return true;
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
    Map<String, Object> getData(final RoboZonkyDaemonFailedEvent event) {
        final Map<String, Object> result = new HashMap<>();
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        event.getCause().printStackTrace(pw);
        result.put("cause", sw.toString());
        return result;
    }
}

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

import java.util.HashMap;
import java.util.Map;

import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;

class RoboZonkyCrashedEventListener extends AbstractEmailingListener<RoboZonkyCrashedEvent> {

    public RoboZonkyCrashedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final RoboZonkyCrashedEvent event) {
        return "RoboZonky selhal!";
    }

    @Override
    String getTemplateFileName() {
        return "crashed.ftl";
    }

    @Override
    protected Map<String, Object> getData(final RoboZonkyCrashedEvent event) {
        final Map<String, Object> result = new HashMap<>();
        result.put("returnCodeName", event.getReturnCode().name());
        result.put("returnCodeId", event.getReturnCode().getCode());
        result.put("isCauseKnown", event.getCause().isPresent());
        event.getCause().ifPresent(cause -> result.put("cause", Util.stackTraceToString(cause)));
        return result;
    }
}

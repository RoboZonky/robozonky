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

import java.util.Map;

import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.notifications.configuration.ListenerSpecificNotificationProperties;
import com.github.robozonky.notifications.util.TemplateUtil;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

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
        final Map<String, Object> result = new UnifiedMap<String, Object>() {{
            put("returnCodeName", event.getReturnCode().name());
            put("returnCodeId", event.getReturnCode().getCode());
            put("isCauseKnown", event.getCause().isPresent());
        }};
        event.getCause().ifPresent(cause -> result.put("cause", TemplateUtil.stackTraceToString(cause)));
        return result;
    }
}

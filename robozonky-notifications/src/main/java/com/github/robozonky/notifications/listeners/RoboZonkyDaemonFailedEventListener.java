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

package com.github.robozonky.notifications.listeners;

import java.util.Collections;
import java.util.Map;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class RoboZonkyDaemonFailedEventListener extends AbstractListener<RoboZonkyDaemonFailedEvent> {

    public RoboZonkyDaemonFailedEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    String getSubject(final RoboZonkyDaemonFailedEvent event) {
        return "Uvnitř RoboZonky došlo k chybě!";
    }

    @Override
    String getTemplateFileName() {
        return "daemon-failed.ftl";
    }

    /**
     * Won't bother users with network connection errors, since in those cases the e-mail probably won't be sent anyway.
     * @param event
     * @param sessionInfo
     * @return
     */
    @Override
    boolean shouldNotify(final RoboZonkyDaemonFailedEvent event, final SessionInfo sessionInfo) {
        return super.shouldNotify(event, sessionInfo) && !Util.isNetworkProblem(event.getCause());
    }

    @Override
    protected Map<String, Object> getData(final RoboZonkyDaemonFailedEvent event) {
        return Collections.singletonMap("cause", Util.stackTraceToString(event.getCause()));
    }
}

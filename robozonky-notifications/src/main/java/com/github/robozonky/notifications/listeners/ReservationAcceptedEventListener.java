/*
 * Copyright 2020 The RoboZonky Project
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

import java.util.HashMap;
import java.util.Map;

import com.github.robozonky.api.notifications.ReservationAcceptedEvent;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

public class ReservationAcceptedEventListener extends AbstractListener<ReservationAcceptedEvent> {

    public ReservationAcceptedEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    public String getSubject(final ReservationAcceptedEvent event) {
        return "Rezervace potvrzena - " + event.getInvestedAmount()
            .getValue()
            .intValue() + ",- Kč, půjčka " +
                Util.identifyLoan(event);
    }

    @Override
    protected Map<String, Object> getData(ReservationAcceptedEvent event) {
        final Map<String, Object> result = new HashMap<>(super.getData(event));
        result.put("amountHeld", event.getInvestedAmount()
            .getValue());
        return result;
    }

    @Override
    public String getTemplateFileName() {
        return "reservation-accepted.ftl";
    }

}

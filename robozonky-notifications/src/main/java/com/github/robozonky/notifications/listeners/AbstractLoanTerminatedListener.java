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

package com.github.robozonky.notifications.listeners;

import java.util.Map;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.DelinquencyBased;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

abstract class AbstractLoanTerminatedListener<T extends Event & DelinquencyBased> extends
                                                                                  AbstractListener<T> {

    protected AbstractLoanTerminatedListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
        registerFinisher((event, sessionInfo) -> delinquencyTracker.unsetDelinquent(sessionInfo,
                                                                                    event.getInvestment()));
    }

    @Override
    boolean shouldNotify(final T event, final SessionInfo sessionInfo) {
        return super.shouldNotify(event, sessionInfo) && delinquencyTracker.isDelinquent(sessionInfo,
                                                                                         event.getInvestment());
    }

    @Override
    protected Map<String, Object> getData(final T event) {
        return Util.getDelinquentData(event.getInvestment(), event.getLoan(), event.getCollectionActions(),
                                      event.getDelinquentSince());
    }
}

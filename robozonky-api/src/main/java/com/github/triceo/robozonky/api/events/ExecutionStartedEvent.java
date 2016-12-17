/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.events;

import java.util.Collection;

import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

/**
 * Fired immediately before the loans are submitted for evaluation by strategy. May be followed by
 * {@link StrategyStartedEvent}, will eventually be followed by {@link ExecutionCompleteEvent}.
 */
public interface ExecutionStartedEvent extends Event {

    /**
     * @return Loans found on the marketplace that are available for robotic investment, not protected by CAPTCHA.
     */
    Collection<LoanDescriptor> getLoanDescriptors();
}

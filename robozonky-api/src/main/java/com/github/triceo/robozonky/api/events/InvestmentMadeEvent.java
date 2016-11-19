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

import com.github.triceo.robozonky.api.remote.entities.Investment;

/**
 * Fired immediately after an investment was submitted to the API or, in case of dry run, immediately after
 * {@link InvestmentRequestedEvent} was fired.
 */
public interface InvestmentMadeEvent extends Event {

    /**
     * @return The investment that was made.
     */
    Investment getInvestment();
}

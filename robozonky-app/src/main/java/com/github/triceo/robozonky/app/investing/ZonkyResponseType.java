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

package com.github.triceo.robozonky.app.investing;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.remote.ControlApi;

public enum ZonkyResponseType {

    /**
     * Already seen and handled in a previous run, no action is being performed.
     */
    SEEN_BEFORE,
    /**
     * Investment confirmed by {@link ControlApi}.
     */
    INVESTED,
    /**
     * Investment taken over by the selected {@link ConfirmationProvider}.
     */
    DELEGATED,
    /**
     * Investment rejected by the selected {@link ConfirmationProvider}, or required CAPTCHA.
     */
    REJECTED

}

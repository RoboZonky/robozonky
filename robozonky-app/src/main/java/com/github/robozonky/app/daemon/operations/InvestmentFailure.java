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

package com.github.robozonky.app.daemon.operations;

import com.github.robozonky.api.confirmations.ConfirmationProvider;

enum InvestmentFailure {

    /**
     * Already seen and handled in a previous run, no action is being performed.
     */
    SEEN_BEFORE,
    /**
     * Investment taken over by the selected {@link ConfirmationProvider}.
     */
    DELEGATED,
    /**
     * Investment rejected by the selected {@link ConfirmationProvider}, or required CAPTCHA.
     */
    REJECTED,
    /**
     * When the investment was triggered but Zonky responded with an error. Most likely caused by the loan becoming
     * fully invested by other investors since last checked.
     */
    FAILED

}

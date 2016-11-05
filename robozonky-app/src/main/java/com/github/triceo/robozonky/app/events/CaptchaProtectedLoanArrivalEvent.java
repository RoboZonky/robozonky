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

package com.github.triceo.robozonky.app.events;

import java.time.OffsetDateTime;

import com.github.triceo.robozonky.events.Event;
import com.github.triceo.robozonky.remote.Loan;

/**
 * Immediately after @{@link MarketplaceCheckStartedEvent}, when a loan is present that can be invested into provided
 * we can pass CAPTCHA.
 */
public class CaptchaProtectedLoanArrivalEvent implements Event {

    private final OffsetDateTime captchaProtectionEnds;
    private final Loan loan;

    public CaptchaProtectedLoanArrivalEvent(final Loan loan, final OffsetDateTime captchaProtectionEnds) {
        this.loan = loan;
        this.captchaProtectionEnds = captchaProtectionEnds;
    }

    /**
     * @return The first instant when the loan is no longer protected by CAPTCHA.
     */
    public OffsetDateTime getCaptchaProtectionEnds() {
        return captchaProtectionEnds;
    }

    /**
     * @return The loan in question.
     */
    public Loan getLoan() {
        return loan;
    }
}

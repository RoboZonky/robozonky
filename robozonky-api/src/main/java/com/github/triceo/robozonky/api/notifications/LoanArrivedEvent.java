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

package com.github.triceo.robozonky.api.notifications;

import com.github.triceo.robozonky.api.strategies.LoanDescriptor;

/**
 * Fired immediately after a new loan is received from the marketplace.
 */
public final class LoanArrivedEvent extends Event {

    private final LoanDescriptor loanDescriptor;

    public LoanArrivedEvent(final LoanDescriptor loanDescriptor) {
        this.loanDescriptor = loanDescriptor;
    }

    /**
     * @return The loan in question.
     */
    public LoanDescriptor getLoanDescriptor() {
        return this.loanDescriptor;
    }
}

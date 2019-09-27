/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.events.impl;

import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.remote.entities.Development;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;

import java.time.LocalDate;
import java.util.Collection;

final class LoanNowDelinquentEventImpl extends AbstractLoanDelinquentEventImpl implements LoanNowDelinquentEvent {

    public LoanNowDelinquentEventImpl(final Investment investment, final Loan loan, final LocalDate since,
                                      final Collection<Development> collectionActions) {
        super(investment, loan, since, collectionActions);
    }

    @Override
    public int getThresholdInDays() {
        return 0;
    }
}

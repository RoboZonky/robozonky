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

package com.github.robozonky.app.events;

import java.time.LocalDate;
import java.util.Collection;

import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;

final class LoanDelinquent90DaysOrMoreEventImpl extends AbstractLoanDelinquentEventImpl implements LoanDelinquent90DaysOrMoreEvent {

    public LoanDelinquent90DaysOrMoreEventImpl(final Investment investment, final Loan loan, final LocalDate since,
                                               final Collection<Development> collectionActions) {
        super(investment, loan, since, collectionActions);
    }

    @Override
    public int getThresholdInDays() {
        return 90;
    }
}

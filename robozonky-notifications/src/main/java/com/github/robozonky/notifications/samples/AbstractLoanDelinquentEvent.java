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

package com.github.robozonky.notifications.samples;

import com.github.robozonky.api.notifications.LoanDelinquentEvent;
import com.github.robozonky.api.remote.entities.Development;
import com.github.robozonky.internal.test.DateUtil;

import java.time.LocalDate;
import java.util.Collection;

abstract class AbstractLoanDelinquentEvent extends AbstractInvestmentBasedEvent
        implements LoanDelinquentEvent {

    private final int thresholdInDays;
    private final LocalDate delinquentSince;
    private final Collection<Development> collectionActions;

    protected AbstractLoanDelinquentEvent(final int threshold) {
        this.thresholdInDays = threshold;
        this.delinquentSince = DateUtil.offsetNow().minusDays(thresholdInDays).toLocalDate();
        this.collectionActions = Util.randomizeDevelopments(getLoan());
    }

    @Override
    public int getThresholdInDays() {
        return thresholdInDays;
    }

    @Override
    public LocalDate getDelinquentSince() {
        return delinquentSince;
    }

    @Override
    public Collection<Development> getCollectionActions() {
        return collectionActions;
    }
}

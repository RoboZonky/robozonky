/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app;

import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Random;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.MyInvestmentImpl;
import com.github.robozonky.test.mock.MockLoanBuilder;

public abstract class AbstractZonkyLeveragingTest extends AbstractEventLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static MyInvestmentImpl mockMyInvestment() {
        return mockMyInvestment(OffsetDateTime.now());
    }

    private static MyInvestmentImpl mockMyInvestment(final OffsetDateTime creationDate) {
        final MyInvestmentImpl m = mock(MyInvestmentImpl.class);
        when(m.getId()).thenReturn(RANDOM.nextLong());
        when(m.getTimeCreated()).thenReturn(Optional.of(creationDate));
        return m;
    }

    protected static LoanDescriptor mockLoanDescriptor() {
        final MockLoanBuilder b = new MockLoanBuilder()
            .set(LoanImpl::setAmount, Money.from(Integer.MAX_VALUE))
            .set(LoanImpl::setRemainingInvestment, Money.from(Integer.MAX_VALUE))
            .set(LoanImpl::setReservedAmount, Money.from(0))
            .set(LoanImpl::setDatePublished, OffsetDateTime.now())
            .set(LoanImpl::setRating, Rating.AAAAA);
        return new LoanDescriptor(b.build());
    }

}

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

package com.github.robozonky.app;

import java.time.OffsetDateTime;
import java.util.Random;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.LoanBuilder;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import com.github.robozonky.internal.api.Settings;

import static org.mockito.Mockito.*;

public abstract class AbstractZonkyLeveragingTest extends AbstractEventLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static MyInvestment mockMyInvestment() {
        return mockMyInvestment(OffsetDateTime.now());
    }

    private static MyInvestment mockMyInvestment(final OffsetDateTime creationDate) {
        final MyInvestment m = mock(MyInvestment.class);
        when(m.getId()).thenReturn(RANDOM.nextLong());
        when(m.getTimeCreated()).thenReturn(creationDate);
        return m;
    }

    protected static LoanDescriptor mockLoanDescriptor() {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(AbstractZonkyLeveragingTest.RANDOM.nextInt());
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId) {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId, true);
    }

    protected static LoanDescriptor mockLoanDescriptorWithoutCaptcha() {
        return AbstractZonkyLeveragingTest.mockLoanDescriptor(AbstractZonkyLeveragingTest.RANDOM.nextInt(), false);
    }

    private static LoanDescriptor mockLoanDescriptor(final int loanId, final boolean withCaptcha) {
        final LoanBuilder b = Loan.custom()
                .setId(loanId)
                .setNonReservedRemainingInvestment(Integer.MAX_VALUE)
                .setDatePublished(OffsetDateTime.now());
        if (withCaptcha) {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_D.getName(), "120"); // enable CAPTCHA for the rating
            b.setRating(Rating.D);
        } else {
            System.setProperty(Settings.Key.CAPTCHA_DELAY_AAAAA.getName(), "0"); // disable CAPTCHA for the rating
            b.setRating(Rating.AAAAA);
        }
        return new LoanDescriptor(b.build());
    }

}

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

package com.github.robozonky.test.mock;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.test.DateUtil;

public class MockLoanBuilder extends BaseLoanMockBuilder<LoanImpl, MockLoanBuilder> {

    public MockLoanBuilder() {
        super(LoanImpl.class);
        set(LoanImpl::setId, RANDOM.nextInt());
        set(LoanImpl::setDatePublished, DateUtil.zonedNow()
            .toOffsetDateTime());
        set(LoanImpl::setRating, Rating.A);
        set(LoanImpl::setInterestRate, Rating.A.getInterestRate());
        set(LoanImpl::setRevenueRate, Rating.A.getMaximalRevenueRate());
    }

    public static LoanImpl fresh() {
        return new MockLoanBuilder()
            .build();
    }

}

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

import static org.mockito.Mockito.*;

import java.net.URL;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.internal.remote.entities.LoanImpl;

public class MockLoanBuilder extends BaseLoanMockBuilder<LoanImpl, MockLoanBuilder> {

    public static LoanImpl fresh() {
        return new MockLoanBuilder().build();
    }

    public MockLoanBuilder() {
        super(LoanImpl.class);
    }

    public MockLoanBuilder setUrl(final URL url) {
        when(mock.getUrl()).thenReturn(url.toString());
        return this;
    }

    public MockLoanBuilder setMyInvestment(final MyInvestment myInvestment) {
        when(mock.getMyInvestment()).thenReturn(Optional.ofNullable(myInvestment));
        return this;
    }

}

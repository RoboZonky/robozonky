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

package com.github.robozonky.internal.remote.entities;

import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.LoanInvestmentData;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

/**
 * This class carries several enumeration-based fields. Some of the enums are extremely important to the core function
 * (such as {@link Rating}), while others ({@link Region}, {@link MainIncomeType}, {@link Purpose}) are only providing
 * additional metadata. If the important enums change, we need RoboZonky to fail. However, in case of the others, we
 * provide non-failing deserializers which handle the missing values gracefully and provide a message warning users that
 * something needs an upgrade.
 */
public class LoanImpl extends BaseLoanImpl implements Loan {

    protected String url;
    @JsonbProperty(nillable = true)
    protected LoanInvestmentDataImpl myInvestment;

    public LoanImpl() {
        // For JSON-B.
    }

    @Override
    public Optional<LoanInvestmentData> getMyInvestment() {
        return Optional.ofNullable(myInvestment);
    }

    public void setMyInvestment(final LoanInvestmentDataImpl myInvestment) {
        this.myInvestment = myInvestment;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoanImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("myInvestment=" + myInvestment)
            .add("url='" + url + "'")
            .toString();
    }
}

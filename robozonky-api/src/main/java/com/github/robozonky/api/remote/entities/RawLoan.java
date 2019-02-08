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

package com.github.robozonky.api.remote.entities;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
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
 * <p>
 * It is not recommended to use this class directly as Zonky will return various null references for fields at various
 * points in the investment lifecycle. Please use {@link Loan} and {@link MarketplaceLoan} as a null-safe alternative.
 * Instances may be created with static methods such as {@link Loan#sanitized(RawLoan)}.
 */
public class RawLoan extends BaseLoan {

    private String url;
    private MyInvestment myInvestment;

    protected RawLoan() {
        // for JAXB
    }

    /**
     * @return Null if the loan doesn't have an investment by the current user.
     */
    @XmlElement
    public MyInvestment getMyInvestment() {
        return myInvestment;
    }

    @XmlElement
    public String getUrl() {
        return url;
    }

}

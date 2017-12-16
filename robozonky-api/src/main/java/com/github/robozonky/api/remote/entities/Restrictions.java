/*
 * Copyright 2017 The RoboZonky Project
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

import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.internal.api.Defaults;

public class Restrictions extends BaseEntity {

    private OffsetDateTime requestDate, withdrawalDate;
    private boolean cannotInvest = false, cannotAccessSmp = false;
    private int maximumInvestmentAmount = Defaults.MINIMAL_MAXIMUM_INVESTMENT_IN_CZK;

    @XmlElement
    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    @XmlElement
    public OffsetDateTime getWithdrawalDate() {
        return withdrawalDate;
    }

    @XmlElement
    public boolean isCannotInvest() {
        return cannotInvest;
    }

    @XmlElement
    public boolean isCannotAccessSmp() {
        return cannotAccessSmp;
    }

    @XmlElement
    public int getMaximumInvestmentAmount() {
        return maximumInvestmentAmount;
    }
}

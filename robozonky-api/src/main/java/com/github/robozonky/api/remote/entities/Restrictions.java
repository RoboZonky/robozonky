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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

public class Restrictions extends BaseEntity {

    private OffsetDateTime requestDate, withdrawalDate;
    private boolean cannotInvest, cannotAccessSmp;
    private int minimumInvestmentAmount = 200, maximumInvestmentAmount = 5_000, investmentStep = 200;

    public Restrictions(final boolean permissive) {
        this.cannotAccessSmp = !permissive;
        this.cannotInvest = !permissive;
    }

    public Restrictions() {
        this(false);
    }

    /**
     * Date of Zonky receiving the investor-initiated contract termination.
     * @return
     */
    @XmlElement
    public OffsetDateTime getRequestDate() {
        return requestDate;
    }

    /**
     * Date of investor's contract termination. Will be later than {@link #getRequestDate()}.
     * @return
     */
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
    public int getMinimumInvestmentAmount() {
        return minimumInvestmentAmount;
    }

    @XmlElement
    public int getInvestmentStep() {
        return investmentStep;
    }

    /**
     * Biggest amount that a user is allowed to invest into a single loan.
     * @return
     */
    @XmlElement
    public int getMaximumInvestmentAmount() {
        return maximumInvestmentAmount;
    }
}

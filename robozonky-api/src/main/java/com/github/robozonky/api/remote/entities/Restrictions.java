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

package com.github.robozonky.api.remote.entities;

import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;

public class Restrictions extends BaseEntity {

    @XmlElement
    private boolean cannotInvest;
    @XmlElement
    private boolean cannotAccessSmp;
    @XmlElement
    private int minimumInvestmentAmount = 200;
    @XmlElement
    private int maximumInvestmentAmount = 5_000;
    @XmlElement
    private int investmentStep = 200;

    public Restrictions(final boolean permissive) {
        this.cannotAccessSmp = !permissive;
        this.cannotInvest = !permissive;
    }

    public Restrictions() {
        this(false);
    }

    public boolean isCannotInvest() {
        return cannotInvest;
    }

    public boolean isCannotAccessSmp() {
        return cannotAccessSmp;
    }

    @XmlTransient
    public Money getMinimumInvestmentAmount() {
        return Money.from(minimumInvestmentAmount);
    }

    public Money getInvestmentStep() {
        return Money.from(investmentStep);
    }

    public Money getMaximumInvestmentAmount() {
        return Money.from(maximumInvestmentAmount);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Restrictions.class.getSimpleName() + "[", "]")
            .add("cannotAccessSmp=" + cannotAccessSmp)
            .add("cannotInvest=" + cannotInvest)
            .add("investmentStep=" + investmentStep)
            .add("maximumInvestmentAmount=" + maximumInvestmentAmount)
            .add("minimumInvestmentAmount=" + minimumInvestmentAmount)
            .toString();
    }
}

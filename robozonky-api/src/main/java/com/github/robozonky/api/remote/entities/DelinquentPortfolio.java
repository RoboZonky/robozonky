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

import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;

public class DelinquentPortfolio extends BaseEntity {

    private final InvestmentSummary dpd1To15 = InvestmentSummary.EMPTY;
    private final InvestmentSummary dpd16To30 = InvestmentSummary.EMPTY;
    private final InvestmentSummary dpd31To60 = InvestmentSummary.EMPTY;
    private final InvestmentSummary dpd61To90 = InvestmentSummary.EMPTY;
    private final InvestmentSummary dpd91Plus = InvestmentSummary.EMPTY;

    DelinquentPortfolio() {
        // fox JAXB
        super();
    }

    @XmlElement
    public InvestmentSummary getDpd1To15() {
        return dpd1To15;
    }

    @XmlElement
    public InvestmentSummary getDpd16To30() {
        return dpd16To30;
    }

    @XmlElement
    public InvestmentSummary getDpd31To60() {
        return dpd31To60;
    }

    @XmlElement
    public InvestmentSummary getDpd61To90() {
        return dpd61To90;
    }

    @XmlElement
    public InvestmentSummary getDpd91Plus() {
        return dpd91Plus;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DelinquentPortfolio.class.getSimpleName() + "[", "]")
                .add("dpd1To15=" + dpd1To15)
                .add("dpd16To30=" + dpd16To30)
                .add("dpd31To60=" + dpd31To60)
                .add("dpd61To90=" + dpd61To90)
                .add("dpd91Plus=" + dpd91Plus)
                .toString();
    }
}

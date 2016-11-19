/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.remote.entities;

import javax.xml.bind.annotation.XmlElement;

public class OverallOverview extends BaseOverview {

    private int feesAmount, netIncome, principalLost;

    OverallOverview() {
        // for JAXB
    }

    @XmlElement
    public int getFeesAmount() {
        return feesAmount;
    }

    @XmlElement
    public int getNetIncome() {
        return netIncome;
    }

    @XmlElement
    public int getPrincipalLost() {
        return principalLost;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OverallOverview{");
        sb.append("feesAmount=").append(feesAmount);
        sb.append(", netIncome=").append(netIncome);
        sb.append(", principalLost=").append(principalLost);
        sb.append("} extends ");
        sb.append(super.toString());
        return sb.toString();
    }
}

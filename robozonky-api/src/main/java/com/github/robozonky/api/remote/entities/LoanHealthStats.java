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

import java.util.OptionalInt;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.LoanHealth;

public class LoanHealthStats extends BaseEntity {

    @XmlElement
    private int paidInstalments;
    @XmlElement
    private int dueInstalments;
    @XmlElement
    private Integer instalmentsCurrentlyInDue;
    @XmlElement
    private int longestDaysDue;
    @XmlElement
    private int daysSinceLastInDue;
    @XmlElement
    private LoanHealth loanHealthInfo = LoanHealth.HISTORICALLY_IN_DUE;

    LoanHealthStats() {
        // For JAXB.
    }

    public int getPaidInstalments() {
        return paidInstalments;
    }

    public int getDueInstalments() {
        return dueInstalments;
    }

    public OptionalInt getInstalmentsCurrentlyInDue() {
        return instalmentsCurrentlyInDue == null ? OptionalInt.empty() : OptionalInt.of(instalmentsCurrentlyInDue);
    }

    public int getLongestDaysDue() {
        return longestDaysDue;
    }

    public int getDaysSinceLastInDue() {
        return daysSinceLastInDue;
    }

    public LoanHealth getLoanHealthInfo() {
        return loanHealthInfo;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoanHealthStats.class.getSimpleName() + "[", "]")
            .add("daysSinceLastInDue=" + daysSinceLastInDue)
            .add("dueInstalments=" + dueInstalments)
            .add("instalmentsCurrentlyInDue=" + instalmentsCurrentlyInDue)
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("longestDaysDue=" + longestDaysDue)
            .add("paidInstalments=" + paidInstalments)
            .toString();
    }
}

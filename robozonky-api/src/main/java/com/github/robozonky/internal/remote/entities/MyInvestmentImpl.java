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

import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.MyInvestment;

/**
 * Used to represent a participation in the authenticated bit of the API. Even
 * though the confusing name, this participation is also used to represent
 * participations of other users for a given loan.
 *
 * Beware, though, since selling a participation on the secondary marketplace
 * will result in that participation being replaced in the list of participations
 * under any given loan with a new one. So, while most of the participations will
 * have a creation date close to loan publishing date, occasionally some will be
 * much newer, indicating SMP transaction.
 *
 * To the best of our knowledge, there is no other way to detect such transactions
 * made by other users.
 */
public class MyInvestmentImpl extends BaseInvestmentImpl implements MyInvestment {

    private int investorId;
    private String investorNickname;

    MyInvestmentImpl() {
        // for JAXB
    }

    @Override
    @XmlElement
    public int getInvestorId() {
        return investorId;
    }

    @Override
    @XmlElement
    public String getInvestorNickname() {
        return investorNickname;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MyInvestmentImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("investorId=" + investorId)
            .add("investorNickname='" + investorNickname + "'")
            .toString();
    }
}

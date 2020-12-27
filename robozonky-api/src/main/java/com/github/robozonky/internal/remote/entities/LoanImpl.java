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

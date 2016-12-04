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

package com.github.triceo.robozonky;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;

final class InvestmentTracker {

    private final List<Loan> loansStillAvailable;
    private final Collection<Investment> investmentsMade = new LinkedHashSet<>();
    private final Collection<Investment> investmentsPreviouslyMade = new HashSet<>();

    public InvestmentTracker(final Collection<Loan> availableLoans) {
        this.loansStillAvailable = new ArrayList<>(availableLoans); // defensive copy
    }

    public void makeInvestment(final Investment investment) {
        this.loansStillAvailable.removeIf(l -> investment.getLoanId() == l.getId());
        this.investmentsMade.add(investment);
    }

    public void registerExistingInvestments(final Collection<Investment> investments) {
        investments.forEach(this::makeInvestment);
        investmentsPreviouslyMade.addAll(investments);
    }

    public Collection<Investment> getInvestmentsMade() {
        return Collections.unmodifiableCollection(this.investmentsMade.stream()
                .filter(i -> !investmentsPreviouslyMade.contains(i))
                .collect(Collectors.toList()));
    }

    public Collection<Investment> getAllInvestments() {
        return Collections.unmodifiableCollection(this.investmentsMade);
    }

    public List<Loan> getAvailableLoans() {
        return Collections.unmodifiableList(this.loansStillAvailable);
    }

}

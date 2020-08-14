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

import java.util.Optional;
import java.util.Set;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.Purpose;

public interface InvestmentLoanData {

    int getId();

    int getActiveLoanOrdinal();

    String getTitle();

    String getStory();

    Money getAnnuity();

    Optional<Label> getLabel();

    Set<DetailLabel> getDetailLabels();

    Borrower getBorrower();

    LoanHealthStats getHealthStats();

    Purpose getPurpose();

    Instalments getPayments();

    Ratio getRevenueRate();

    Ratio getInterestRate();

}

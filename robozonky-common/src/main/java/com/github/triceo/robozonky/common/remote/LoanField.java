/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.common.remote;

import com.github.triceo.robozonky.api.remote.entities.Loan;

public enum LoanField implements Field<Loan> {

    ID("id"),
    TERM_IN_MONTHS("termInMonths"),
    INVESTMENTS_COUNT("investmentsCount"),
    QUESTIONS_COUNT("questionsCount"),
    USER_ID ("userId"), AMOUNT("amount"),
    REMAINING_INVESTMENT("remainingInvestment"),
    NAME("name"),
    NICK_NAME("nickName"),
    INTEREST_RATE("interestRate"),
    DATE_PUBLISHED("datePublished"),
    DEADLINE("deadline"),
    RATING("rating"),
    INVESTMENT_RATE("investmentRate"),
    MAIN_INCOME_TYPE("mainIncomeType"),
    REGION("region"),
    PURPOSE("purpose");

    private final String id;

    LoanField(final String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

}

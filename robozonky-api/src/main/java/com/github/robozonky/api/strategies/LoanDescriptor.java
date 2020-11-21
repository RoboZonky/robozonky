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

package com.github.robozonky.api.strategies;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.remote.entities.Loan;

/**
 * Carries metadata regarding a {@link Loan}.
 */
public final class LoanDescriptor implements Descriptor<Loan> {

    private static final Logger LOGGER = LogManager.getLogger(LoanDescriptor.class);

    private final Loan loan;

    public LoanDescriptor(final Loan loan) {
        this.loan = loan;
    }

    @Override
    public String toString() {
        return "LoanDescriptor{" +
                "loan=" + loan +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LoanDescriptor)) {
            return false;
        }
        final LoanDescriptor that = (LoanDescriptor) o;
        return Objects.equals(loan, that.loan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loan);
    }

    @Override
    public Loan item() {
        return loan;
    }

    @Override
    public Loan related() {
        return loan;
    }

}

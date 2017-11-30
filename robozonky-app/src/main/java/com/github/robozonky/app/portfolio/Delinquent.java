/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.portfolio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Represents a loan that, either now or at some point in the past, had at least one overdue instalment.
 */
public final class Delinquent {

    private final int loanId;
    private final SortedMap<LocalDate, Delinquency> delinquencies = new TreeMap<>();

    /**
     * New delinquent loan with one active delinquent instalment.
     * @param loanId ID of the loan in question.
     * @param since The day that an instalment was first noticed overdue.
     */
    public Delinquent(final int loanId, final LocalDate since) {
        this.loanId = loanId;
        this.delinquencies.put(since, new Delinquency(this, since));
    }

    /**
     * New delinquent loan with no delinquent instalments.
     * @param loanId ID of the loan in question.
     */
    public Delinquent(final int loanId) {
        this.loanId = loanId;
    }

    /**
     * @return ID of the delinquent loan.
     */
    public int getLoanId() {
        return loanId;
    }

    /**
     * @return Present if there is currently an overdue instalment for the loan in question.
     */
    public Optional<Delinquency> getActiveDelinquency() {
        if (delinquencies.isEmpty()) {
            return Optional.empty();
        }
        return getLatestDelinquency().flatMap(d -> d.getFixedOn().isPresent() ? Optional.empty() : Optional.of(d));
    }

    /**
     * Retrieve the latest delinquency recorded for this delinquent, be it active or not.
     * @return Empty when no delinquencies at all.
     */
    public Optional<Delinquency> getLatestDelinquency() {
        if (delinquencies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(delinquencies.get(delinquencies.lastKey()));
    }

    /**
     * @return True if {@link #getActiveDelinquency()} is present.
     */
    public boolean hasActiveDelinquency() {
        return getActiveDelinquency().isPresent();
    }

    /**
     * @return All registered delinquent instalments, both present and past.
     */
    public Stream<Delinquency> getDelinquencies() {
        return delinquencies.values().stream();
    }

    /**
     * Add active delinquency.
     * @param since The day that an instalment was first noticed overdue.
     * @return New instance or, if such delinquency already existed, the original instance. See
     * {@link Delinquency#equals(Object)}.
     */
    Delinquency addDelinquency(final LocalDate since) {
        return delinquencies.computeIfAbsent(since, this::newDelinquency);
    }

    private Delinquency newDelinquency(final LocalDate since) {
        return new Delinquency(this, since);
    }

    /**
     * Add inactive delinquency.
     * @param since The day that an instalment was first noticed overdue.
     * @param until The day that the loan was first noticed as no longer delinquent.
     * @return New instance or, if such delinquency already existed, the original instance. See
     * {@link Delinquency#equals(Object)}.
     */
    Delinquency addDelinquency(final LocalDate since, final LocalDate until) {
        final Delinquency newDelinquency = addDelinquency(since);
        newDelinquency.setFixedOn(until);
        return newDelinquency;
    }

    /**
     * See {@link Object#equals(Object)}.
     * @param o Another delinquent loan.
     * @return Delinquent loans are considered equal when their {@link #getLoanId()}s are equal.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Delinquent that = (Delinquent) o;
        return loanId == that.loanId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId);
    }

    @Override
    public String toString() {
        return "Delinquent{" +
                "loanId=" + loanId +
                ", delinquencies=" + delinquencies +
                '}';
    }
}

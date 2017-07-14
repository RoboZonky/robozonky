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

package com.github.triceo.robozonky.strategy.natural;

import java.util.Optional;
import java.util.function.Predicate;

import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class MarketplaceFilterCondition implements Predicate<Loan> {

    // not static as we want to have the specific impl class in the logs
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    /**
     * Describe the condition using eg. range boundaries.
     *
     * @return If present, is a whole sentence. (Starting with capital letter, ending with a full stop.)
     */
    protected Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * Determine whether or not the loan in question matches the condition represented by this class.
     *
     * @param loan Loan in question.
     * @return True if loan matches the condition.
     */
    public boolean test(final Loan loan) {
        LOGGER.warn("Default condition implementation never matches the condition.");
        return false;
    }

    @Override
    public String toString() {
        final String description = getDescription().orElse("N/A.");
        return this.getClass().getSimpleName() + " (" + description + ")";
    }

}

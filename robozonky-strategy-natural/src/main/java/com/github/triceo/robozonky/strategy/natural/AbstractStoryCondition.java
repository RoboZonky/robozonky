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

import java.util.function.Predicate;

import com.github.triceo.robozonky.api.remote.entities.Loan;

class AbstractStoryCondition extends MarketplaceFilterCondition {

    // these values were the first and third quartile of story length in all loans between 2016-10-01 and 2017-05-20
    protected static final int SHORT_STORY_THRESHOLD = 200, LONG_STORY_THRESHOLD = 600;

    private final Predicate<String> storyLength;

    protected AbstractStoryCondition(final Predicate<String> storyLength) {
        LOGGER.debug("Story length condition registered.");
        this.storyLength = storyLength;
    }

    @Override
    public boolean test(final Loan loan) {
        final String story = loan.getStory();
        final boolean isStoryProvided = (story == null);
        return storyLength.test(isStoryProvided ? "" : loan.getStory().trim());
    }

}

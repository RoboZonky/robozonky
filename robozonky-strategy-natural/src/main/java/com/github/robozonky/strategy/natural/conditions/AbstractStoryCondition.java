/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.strategy.natural.conditions;

import java.util.function.Predicate;

import com.github.robozonky.strategy.natural.Wrapper;

class AbstractStoryCondition extends MarketplaceFilterConditionImpl {

    // these values were the first and third quartile of story length in all loans between 2016-10-01 and 2017-05-20
    static final int SHORT_STORY_THRESHOLD = 200, LONG_STORY_THRESHOLD = 600;
    static final int VERY_SHORT_STORY_THRESHOLD = AbstractStoryCondition.SHORT_STORY_THRESHOLD / 2;

    private final Predicate<String> storyLength;

    protected AbstractStoryCondition(final Predicate<String> storyLength) {
        this.storyLength = storyLength;
    }

    @Override
    public boolean test(final Wrapper<?> loan) {
        final String story = loan.getStory();
        final boolean isStoryProvided = (story == null);
        return storyLength.test(isStoryProvided ? "" : loan.getStory().trim());
    }
}

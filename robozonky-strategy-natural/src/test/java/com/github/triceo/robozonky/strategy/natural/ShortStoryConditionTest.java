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

import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ShortStoryConditionTest {

    @Test
    public void longerNotOk() {
        final Loan l = Mockito.mock(Loan.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD, '*');
        Mockito.when(l.getStory()).thenReturn(story);
        Assertions.assertThat(new ShortStoryCondition().test(l)).isFalse();
    }

    @Test
    public void shorterOk() {
        final Loan l = Mockito.mock(Loan.class);
        final String story = StringUtils.leftPad("", AbstractStoryCondition.SHORT_STORY_THRESHOLD - 1, '*');
        Mockito.when(l.getStory()).thenReturn(story);
        Assertions.assertThat(new ShortStoryCondition().test(l)).isTrue();
    }

}

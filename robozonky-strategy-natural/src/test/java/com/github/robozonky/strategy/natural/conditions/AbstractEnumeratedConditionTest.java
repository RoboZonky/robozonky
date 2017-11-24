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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Arrays;

import com.github.robozonky.strategy.natural.Wrapper;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public abstract class AbstractEnumeratedConditionTest<T> {

    protected abstract AbstractEnumeratedCondition<T> getSUT();

    protected abstract Wrapper getMocked();

    protected abstract T getTriggerItem();

    protected abstract T getNotTriggerItem();

    @Test
    public void properAsCollection() {
        final Wrapper i = this.getMocked();
        final AbstractEnumeratedCondition<T> sut = this.getSUT();
        Assertions.assertThat(sut.test(i)).isFalse();
        sut.add(Arrays.asList(this.getTriggerItem(), this.getNotTriggerItem()));
        Assertions.assertThat(sut.test(i)).isTrue();
        Assertions.assertThat(sut.getDescription()).isPresent();
    }

    @Test
    public void properAsOne() {
        final Wrapper i = this.getMocked();
        final AbstractEnumeratedCondition<T> sut = this.getSUT();
        Assertions.assertThat(sut.test(i)).isFalse();
        sut.add(this.getTriggerItem());
        sut.add(this.getNotTriggerItem());
        Assertions.assertThat(sut.test(i)).isTrue();
        Assertions.assertThat(sut.getDescription()).isPresent();
    }

    @Test
    public void nonEmptyDescription() {
        Assertions.assertThat(getSUT().getDescription()).isPresent();
    }
}

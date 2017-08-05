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

import java.util.Collections;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public abstract class AbstractEnumeratedConditionTest<S, T> {

    protected abstract AbstractEnumeratedCondition<S, T> getSUT();

    protected abstract S getMocked();

    protected abstract T getTriggerItem();

    @Test
    public void proper() {
        final S i = this.getMocked();
        final AbstractEnumeratedCondition<S, T> sut = this.getSUT();
        Assertions.assertThat(sut.test(i)).isFalse();
        sut.add(Collections.singleton(this.getTriggerItem()));
        Assertions.assertThat(sut.test(i)).isTrue();
    }
}

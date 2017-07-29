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

package com.github.triceo.robozonky.internal.api;

import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Test;

public class StateTest {

    private final State.ClassSpecificState state = State.forClass(this.getClass());

    @After
    public void reset() {
        state.newBatch(true).call();
    }

    @Test
    public void empty() {
        Assertions.assertThat(state.getKeys()).isEmpty();
    }

    @Test
    public void store() {
        final State.ClassSpecificState state = State.forClass(this.getClass());
        // store
        final State.Batch b = state.newBatch();
        final String key = UUID.randomUUID().toString(), value = UUID.randomUUID().toString(),
                key2 = UUID.randomUUID().toString(), value2 = UUID.randomUUID().toString();
        b.set(key, value).set(key2, value2);
        b.call();
        // read stored
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(state.getValue(key)).contains(value);
            softly.assertThat(state.getValue(key2)).contains(value2);
        });
        // unset something
        state.newBatch().unset(key).set(key2, value).call();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(state.getValue(key)).isEmpty();
            softly.assertThat(state.getValue(key2)).contains(value);
        });
        // and perform plain reset
        state.newBatch(true).set(key, value2).call();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(state.getKeys()).containsExactly(key);
            softly.assertThat(state.getValue(key)).contains(value2);
        });
    }
}

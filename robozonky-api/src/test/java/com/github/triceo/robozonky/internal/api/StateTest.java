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
import org.junit.Assume;
import org.junit.Test;

public class StateTest {

    @After
    public void deleteState() {
        State.getStateLocation().delete();
    }

    @Test
    public void persistentStorage() {
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        // store
        final State.ClassSpecificState old = State.INSTANCE.forClass(StateTest.class);
        old.setValue(key, value);
        Assertions.assertThat(old.getValue(key)).isPresent().contains(value);
        // retrieve from a new instance
        final State.ClassSpecificState current = State.INSTANCE.forClass(StateTest.class);
        Assertions.assertThat(current.getValue(key)).isPresent().contains(value);
        // reset and check if both empty, since backed by the same storage
        Assertions.assertThat(current.reset()).isTrue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(old.getValue(key)).isEmpty();
            softly.assertThat(current.getValue(key)).isEmpty();
        });
    }

    @Test
    public void isolatedStorage() {
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        // store
        final State.ClassSpecificState first = State.INSTANCE.forClass(StateTest.class);
        first.setValue(key, value);
        Assume.assumeTrue(first.getValue(key).isPresent());
        final State.ClassSpecificState second = State.INSTANCE.forClass(State.class);
        second.setValue(key, value);
        Assume.assumeTrue(second.getValue(key).isPresent());
        // delete one, make sure second is unaffected
        Assertions.assertThat(first.unsetValue(key)).isTrue();
        Assertions.assertThat(second.getValue(key)).isPresent().contains(value);
        // delete again, make sure fails
        Assertions.assertThat(first.unsetValue(key)).isFalse();
    }

    @Test
    public void gracefullyHandleDeletedState() {
        final State.ClassSpecificState first = State.INSTANCE.forClass(StateTest.class);
        Assertions.assertThat(first.setValue("aaaa", "bbbb")).isTrue();
        this.deleteState();
        Assertions.assertThat(first.setValue("bbbb", "cccc")).isTrue();
    }

}

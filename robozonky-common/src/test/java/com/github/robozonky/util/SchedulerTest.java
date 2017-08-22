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

package com.github.robozonky.util;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Refreshable;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class SchedulerTest {

    private static final Refreshable<String> REFRESHABLE = new RefreshableString();

    @Test
    public void lifecycle() {
        final Scheduler s = new Scheduler();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.isShutdown()).isFalse();
            softly.assertThat(s.isSubmitted(REFRESHABLE)).isFalse();
        });
        s.submit(REFRESHABLE);
        Assertions.assertThat(s.isSubmitted(REFRESHABLE)).isTrue();
        s.shutdown();
        Assertions.assertThat(s.isShutdown()).isTrue();
    }

    @Test
    public void reinitBeforeShutdown() {
        final Refreshable<Void> r = Refreshable.createImmutable();
        final Scheduler s = new Scheduler();
        s.submit(r);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.isSubmitted(r)).isTrue();
            softly.assertThat(s.isShutdown()).isFalse();
            softly.assertThat(s.reinit()).isFalse();
        });
        s.shutdown();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.isShutdown()).isTrue();
            softly.assertThat(s.reinit()).isTrue();
        });
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(s.isSubmitted(r)).isFalse();
            softly.assertThat(s.isShutdown()).isFalse();
        });
    }

    private static final class RefreshableString extends Refreshable<String> {

        @Override
        protected Supplier<Optional<String>> getLatestSource() {
            return () -> Optional.of("");
        }

        @Override
        protected Optional<String> transform(final String source) {
            return Optional.of(source);
        }
    }
}

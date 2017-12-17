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
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefreshableTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshableTest.class);
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    private static String transform(final String original) {
        return "Transformed " + original;
    }

    private static final class TestingRefreshable extends Refreshable<String> {

        private String latestSource = null;

        public TestingRefreshable(final String intialSource) {
            this.latestSource = intialSource;
        }

        public void setLatestSource(final String latestSource) {
            this.latestSource = latestSource;
        }

        @Override
        protected Optional<String> getLatestSource() {
            return Optional.ofNullable(latestSource);
        }

        @Override
        protected Optional<String> transform(final String source) {
            return Optional.of(RefreshableTest.transform(source));
        }
    }

    @Test
    public void immutable() {
        final Refreshable<Void> r = Refreshable.createImmutable();
        r.run();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.get()).isEmpty();
            softly.assertThat(r.isPaused()).isFalse();
        });
    }

    @Test
    public void mutableNoRefresh() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        Assertions.assertThat(r.get()).isPresent().contains(RefreshableTest.transform(initial));
        final String original = r.get().get();
        r.run();
        Assertions.assertThat(r.get()).isPresent().contains(original);
        Assertions.assertThat(r.get().get()).isSameAs(original);
    }

    @Test
    public void mutableRefreshing() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        Assertions.assertThat(r.get()).isPresent().contains(RefreshableTest.transform(initial));
        r.setLatestSource(null); // make sure latest will get reset
        r.run();
        Assertions.assertThat(r.get()).isEmpty();
    }

    @Test
    public void registersListeners() {
        final String s = UUID.randomUUID().toString();
        final Refreshable<String> r = Refreshable.createImmutable(s);
        r.run();
        final Refreshable.RefreshListener<String> l = Mockito.mock(Refreshable.RefreshListener.class);
        Assertions.assertThat(r.registerListener(l)).isTrue();
        Mockito.verify(l).valueSet(ArgumentMatchers.eq(s));
        Assertions.assertThat(r.registerListener(l)).isFalse(); // repeat registration
        Mockito.verify(l, Mockito.times(1)).valueSet(ArgumentMatchers.eq(s));
        Assertions.assertThat(r.unregisterListener(l)).isTrue();
        Mockito.verify(l).valueUnset(ArgumentMatchers.eq(s));
        Assertions.assertThat(r.unregisterListener(l)).isFalse(); // repeat unregistration
        Mockito.verify(l, Mockito.times(1)).valueUnset(ArgumentMatchers.eq(s));
        Assertions.assertThat(r.registerListener(l)).isTrue(); // re-registration
        Mockito.verify(l, Mockito.times(2)).valueSet(ArgumentMatchers.eq(s));
    }

    private static class RefreshableString extends Refreshable<String> {

        public RefreshableString() {
        }

        @Override
        protected Optional<String> getLatestSource() {
            return Optional.of(UUID.randomUUID().toString());
        }

        @Override
        protected Optional<String> transform(final String source) {
            return Optional.of(source);
        }
    }

    @Test
    public void checkListeners() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        final Refreshable.RefreshListener<String> l = Mockito.mock(Refreshable.RefreshListener.class);
        r.registerListener(l);
        r.setLatestSource(initial);
        r.run();
        Mockito.verify(l, Mockito.times(1)).valueSet(RefreshableTest.transform(initial));
        final String otherValue = "other";
        r.setLatestSource(otherValue);
        r.run();
        Mockito.verify(l, Mockito.times(1))
                .valueChanged(RefreshableTest.transform(initial), RefreshableTest.transform(otherValue));
        r.setLatestSource(null);
        r.run();
        Mockito.verify(l, Mockito.times(1))
                .valueUnset(RefreshableTest.transform(otherValue));
    }

    @Test
    public void refreshAfterUnpaused() {
        final Refreshable<String> parent = new RefreshableString();
        parent.run();
        final Optional<String> before = parent.get();
        parent.pauseFor((r) -> {
            Assertions.assertThat(parent.isPaused()).isTrue();
            parent.run(); // this will not be executed
            final Optional<String> after = parent.get();
            Assertions.assertThat(before).isEqualTo(after);
            return null;
        });
        // pause is over, now the refresh should be run automatically
        final Optional<String> after = parent.get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(parent.isPaused()).isFalse();
            softly.assertThat(before).isNotEqualTo(after);
        });
    }
}

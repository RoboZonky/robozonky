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

package com.github.triceo.robozonky.api;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RefreshableTest {

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
        public Optional<Refreshable<?>> getDependedOn() {
            return Optional.empty();
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
        Assertions.assertThat(r.getDependedOn()).isEmpty();
        Assertions.assertThat(r.getLatest()).isEmpty();
        r.run(); // register no change
        Assertions.assertThat(r.getLatest()).isEmpty();
    }

    @Test
    public void mutableNoRefresh() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        Assertions.assertThat(r.getLatest()).isEmpty(); // before run() is called, there is nothing
        r.run();
        Assertions.assertThat(r.getLatest()).isPresent().contains(RefreshableTest.transform(initial));
        final String original = r.getLatest().get();
        r.run();
        Assertions.assertThat(r.getLatest()).isPresent().contains(original);
        Assertions.assertThat(r.getLatest().get()).isSameAs(original);
    }

}

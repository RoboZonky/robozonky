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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.Test;
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
        public Optional<Refreshable<?>> getDependedOn() {
            return Optional.empty();
        }

        @Override
        protected Supplier<Optional<String>> getLatestSource() {
            return () -> Optional.ofNullable(latestSource);
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
        r.run();
        Assertions.assertThat(r.getLatest()).isEmpty();
    }

    @Test
    public void mutableNoRefresh() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        Assertions.assertThat(r.getLatest()).isPresent().contains(RefreshableTest.transform(initial));
        final String original = r.getLatest().get();
        r.run();
        Assertions.assertThat(r.getLatest()).isPresent().contains(original);
        Assertions.assertThat(r.getLatest().get()).isSameAs(original);
    }

    @Test
    public void mutableRefreshing() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        Assertions.assertThat(r.getLatest()).isPresent().contains(RefreshableTest.transform(initial));
        r.setLatestSource(null); // make sure latest will get reset
        r.run();
        Assertions.assertThat(r.getLatest()).isEmpty();
    }

    @Test(timeout = 5000)
    public void waitsForInitial() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        RefreshableTest.EXECUTOR.schedule(r, 1, TimeUnit.SECONDS); // execute only after the assertion is called
        RefreshableTest.LOGGER.info("Blocking until value is found.");
        Assertions.assertThat(r.getLatest()).isNotEmpty();
    }

    @Test(timeout = 5000)
    public void waitsForValue() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        // set and remove value
        r.run();
        r.setLatestSource(null);
        r.run();
        // wait for new value
        RefreshableTest.LOGGER.info("Scheduling.");
        RefreshableTest.EXECUTOR.schedule(() -> {
            RefreshableTest.LOGGER.info("Executing.");
            r.setLatestSource("something");
            r.run();
            RefreshableTest.LOGGER.info("Executed.");
        }, 1, TimeUnit.SECONDS); // execute only after the assertion is called
        RefreshableTest.LOGGER.info("Blocking until value is found.");
        Assertions.assertThat(r.getLatestBlocking()).isNotEmpty();
        RefreshableTest.LOGGER.info("Found.");
    }

}

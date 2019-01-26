/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.common.async;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshableTest {

    private static String transform(final String original) {
        return "Transformed " + original;
    }

    @Mock
    private Refreshable.RefreshListener<String> l;

    @Test
    void mutableNoRefresh() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        assertThat(r.get()).isPresent().contains(RefreshableTest.transform(initial));
        final String original = r.get().get();
        r.run();
        assertThat(r.get()).isPresent().contains(original);
        assertThat(r.get().get()).isSameAs(original);
    }

    @Test
    void mutableRefreshing() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.run();
        assertThat(r.get()).isPresent().contains(RefreshableTest.transform(initial));
        r.setLatestSource(null); // make sure latest will get reset
        r.run();
        assertThat(r.get()).isEmpty();
    }

    @Test
    void registersListeners() {
        final String s = UUID.randomUUID().toString();
        final String transformed = transform(s);
        final Refreshable<String> r = new TestingRefreshable(s);
        r.run();
        assertThat(r.registerListener(l)).isTrue();
        verify(l).valueSet(eq(transformed));
        assertThat(r.registerListener(l)).isFalse(); // repeat registration
        verify(l, times(1)).valueSet(eq(transformed));
        assertThat(r.unregisterListener(l)).isTrue();
        verify(l).valueUnset(eq(transformed));
        assertThat(r.unregisterListener(l)).isFalse(); // repeat unregistration
        verify(l, times(1)).valueUnset(eq(transformed));
        assertThat(r.registerListener(l)).isTrue(); // re-registration
        verify(l, times(2)).valueSet(eq(transformed));
    }

    @Test
    void checkListeners() {
        final String initial = "initial";
        final RefreshableTest.TestingRefreshable r = new RefreshableTest.TestingRefreshable(initial);
        r.registerListener(l);
        r.setLatestSource(initial);
        r.run();
        verify(l, times(1)).valueSet(RefreshableTest.transform(initial));
        final String otherValue = "other";
        r.setLatestSource(otherValue);
        r.run();
        verify(l, times(1))
                .valueChanged(RefreshableTest.transform(initial), RefreshableTest.transform(otherValue));
        r.setLatestSource(null);
        r.run();
        verify(l, times(1))
                .valueUnset(RefreshableTest.transform(otherValue));
    }

    @Test
    void refreshListenerDefaultMethod() {
        final Refreshable.RefreshListener<String> l = spy(Refreshable.RefreshListener.class);
        l.valueChanged("a", "b");
        verify(l).valueSet(eq("b"));
    }

    private static final class TestingRefreshable extends Refreshable<String> {

        private String latestSource;

        TestingRefreshable(final String intialSource) {
            this.latestSource = intialSource;
        }

        @Override
        protected String getLatestSource() {
            return latestSource;
        }

        void setLatestSource(final String latestSource) {
            this.latestSource = latestSource;
        }

        @Override
        protected Optional<String> transform(final String source) {
            return Optional.of(RefreshableTest.transform(source));
        }
    }
}

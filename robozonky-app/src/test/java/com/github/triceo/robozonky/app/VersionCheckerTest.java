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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.app.version.VersionIdentifier;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class VersionCheckerTest {

    @Test
    public void checkShutdownHook() {
        final VersionChecker v = new VersionChecker();
        final Optional<Consumer<ReturnCode>> hook = v.get();
        Assertions.assertThat(hook).isPresent();
        hook.get().accept(ReturnCode.OK);
    }

    @Test
    public void versionCheckHasNewer() {
        final String currentVersion = "1.0.0";
        final String newerVersion = "1.0.1";
        final VersionIdentifier v = Mockito.mock(VersionIdentifier.class);
        Mockito.when(v.getLatestStable()).thenReturn(newerVersion);
        Assertions.assertThat(VersionChecker.newerRoboZonkyVersionExists(v, currentVersion)).isTrue();
        Mockito.when(v.getLatestStable()).thenReturn(currentVersion);
        Mockito.when(v.getLatestUnstable()).thenReturn(Optional.of(newerVersion));
        Assertions.assertThat(VersionChecker.newerRoboZonkyVersionExists(v, currentVersion)).isTrue();
    }

    @Test
    public void versionCheckUpToDate() {
        final String currentVersion = "1.0.0";
        final VersionIdentifier v = Mockito.mock(VersionIdentifier.class);
        Mockito.when(v.getLatestStable()).thenReturn(currentVersion);
        Assertions.assertThat(VersionChecker.newerRoboZonkyVersionExists(v, currentVersion)).isFalse();
    }

    @Test
    public void versionCheckUpToDateFromRefreshable() {
        final VersionIdentifier v = Mockito.mock(VersionIdentifier.class);
        Mockito.when(v.getLatestStable()).thenReturn("1.0.0");
        final Refreshable<VersionIdentifier> r = Refreshable.createImmutable(v);
        r.run();
        Assertions.assertThat(VersionChecker.newerRoboZonkyVersionExists(r)).isFalse();
    }
}


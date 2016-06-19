/*
 * Copyright 2016 Lukáš Petrovický
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import com.github.triceo.robozonky.remote.Investment;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class AppTest extends AbstractNonExitingTest {

    @Test(expected = RoboZonkyTestingExitException.class)
    public void unreadableStrategyFile() {
        App.processCommandLine("-s", "something", "-u", "user", "-p", "pass");
    }

    @Test(expected = RoboZonkyTestingExitException.class)
    public void nothingOnCommandLine() {
        Assertions.assertThat(App.processCommandLine());
    }

    @Test
    public void storeInvestmentData() throws IOException {
        Assertions.assertThat(App.storeInvestmentsMade(null, Collections.emptySet())).isEmpty();
        File f = File.createTempFile("robozonky-", ".investments");
        f.delete();
        Optional<File> result = App.storeInvestmentsMade(f, Collections.singleton(Mockito.mock(Investment.class)));
        Assertions.assertThat(result).contains(f);
    }

    @Test
    public void storeInvestmentDataWithDryRun() throws IOException {
        Optional<File> result = App.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), true);
        Assertions.assertThat(result).isPresent();
        Optional<File> result2 = App.storeInvestmentsMade(Collections.singleton(Mockito.mock(Investment.class)), false);
        Assertions.assertThat(result2).isPresent();
        Assertions.assertThat(result2.get().getAbsolutePath()).isNotEqualTo(result.get().getAbsolutePath());
    }

}

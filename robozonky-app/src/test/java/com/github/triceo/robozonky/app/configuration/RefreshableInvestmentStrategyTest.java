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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.net.MalformedURLException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RefreshableInvestmentStrategyTest {

    private static String getRoot() {
        return IoTestUtil.findMainSource("assembly", "resources", "examples", "strategies");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        final String[] files = new String[] {
                "robozonky-balanced.cfg", "robozonky-conservative.cfg", "robozonky-dynamic.cfg"
        };
        return Stream.of(files)
                .map(f -> new File[] {new File(RefreshableInvestmentStrategyTest.getRoot(), f)})
                .collect(Collectors.toList())
                .toArray(new Object[files.length][1]);
    }

    @Parameterized.Parameter
    public File strategy;

    @Test
    public void loadStrategyAsFile() throws InterruptedException {
        final Refreshable<InvestmentStrategy> r = RefreshableInvestmentStrategy.create(strategy.getAbsolutePath());
        Assertions.assertThat(r.getLatest()).isPresent();
    }

    @Test
    public void loadStrategyAsUrl() throws MalformedURLException, InterruptedException {
        final String url = strategy.toURI().toURL().toString();
        final Refreshable<InvestmentStrategy> r = RefreshableInvestmentStrategy.create(url);
        Assertions.assertThat(r.getLatest()).isPresent();
    }

}

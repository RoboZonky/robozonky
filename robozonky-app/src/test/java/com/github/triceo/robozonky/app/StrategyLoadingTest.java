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

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class StrategyLoadingTest {

    private static final String root = "src/main/assembly/resources/";

    @Parameterized.Parameters
    public static Object[][] getParameters() {
        return new File[][] {
            new File[] {new File(root, "robozonky-balanced.cfg")},
                new File[] {new File(root, "robozonky-conservative.cfg")},
                new File[] {new File(root, "robozonky-dynamic.cfg")}
        };
    }

    @Parameterized.Parameter
    public File strategy;

    @Test
    public void loadStrategy() throws ConfigurationException {
        StrategyParser.parse(this.strategy);
    }

}

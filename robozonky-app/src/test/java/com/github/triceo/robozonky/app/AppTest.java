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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

// FIXME this "integration test" sucks; break down into multiple actual unit tests
public class AppTest {

    @BeforeClass
    public static void disableSystemExit() {
        App.PERFORM_SYSTEM_EXIT = false;
    }

    @AfterClass
    public static void enableSystemExit() {
        App.PERFORM_SYSTEM_EXIT = true;
    }

    @Test
    public void tokenizedDryRun() {
        App.main("-s", "src/main/assembly/resources/robozonky-dynamic.cfg", "-d", "0", "-u", "someone",
                "-p", "somepassword", "-r");
    }

    @Test
    public void tokenLessDryRun() {
        App.main("-s", "src/main/assembly/resources/robozonky-conservative.cfg", "-d", "2000",
                "-u", "someone", "-p", "somepassword");
    }

    @Test
    public void strategyLessRun() {
        App.main("-a", "400", "-l", "66666", "-d", "2000", "-u", "someone", "-p", "somepassword");
    }

    @Test
    public void simpleHelp() {
        App.processCommandLine("-h");
    }

}

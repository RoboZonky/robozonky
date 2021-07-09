/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.cli;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;

abstract class AbstractFeature implements Feature {

    protected final Logger logger = LogManager.getLogger(this.getClass());
    private final AtomicInteger exitCode = new AtomicInteger(CommandLine.ExitCode.OK);

    @Override
    public int getExitCode() {
        return exitCode.get();
    }

    @Override
    public void run() {
        System.out.println("Welcome to the RoboZonky command-line configuration and validation tool.");
        System.out.println("This is a tool for the brave. Create a backup copy of RoboZonky " +
                "or use RoboZonky installer instead.");
        try {
            final String description = describe();
            System.out.println("--- Press any key to run: '" + description + "'");
            System.in.read();
            setup();
            System.out.println("--- Executed, running test of the new setup.");
            test();
            System.out.println("--- Success.");
        } catch (final SetupFailedException | IOException e) {
            System.err.println("Could not perform setup, configuration may have been corrupted.");
            e.printStackTrace(System.err);
            exitCode.set(CommandLine.ExitCode.SOFTWARE);
        } catch (final TestFailedException e) {
            System.err.println("Could not test setup, configuration may have been corrupted.");
            e.printStackTrace(System.err);
            exitCode.set(CommandLine.ExitCode.SOFTWARE + 1);
        } finally {
            System.out.println("--- Terminating.");
        }
    }
}

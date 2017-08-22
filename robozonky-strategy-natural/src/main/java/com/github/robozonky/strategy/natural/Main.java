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

package com.github.robozonky.strategy.natural;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Simplifies testing of strategies by enabling parser to be launched directly from the command line.
 */
public class Main {

    public static void main(final String... args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("The application expects exactly one argument: the strategy file.");
        }
        final File f = new File(args[0]);
        if (!f.exists() || !f.canRead()) {
            throw new IllegalArgumentException("Can not read strategy file: " + f.getAbsolutePath());
        }
        try (final InputStream s = new BufferedInputStream(new FileInputStream(f))) {
            System.out.println("Will read: " + f.getAbsolutePath());
            NaturalLanguageStrategyService.parseWithAntlr(s);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed parsing strategy.", ex);
        }
    }
}

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

package com.github.triceo.robozonky.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.stream.Collectors;

/**
 * Utility method to ease working with input/output operations in Java.
 */
public class IOUtils {

    /**
     * Convert a Reader to a String.
     * @param reader Reader to convert. Will be closed by the end.
     * @return Contents of the Reader. Newlines will be replaced by {@link System#lineSeparator()}.
     */
    public static String toString(final Reader reader) {
        try (final BufferedReader r = new BufferedReader(reader)) {
            return r.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed converting Reader to String.", ex);
        }
    }
}

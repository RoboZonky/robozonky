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

package com.github.robozonky.cli;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.*;

class AbstractFeatureTest {

    private final InputStream originalSystemIn = System.in;

    static Stream<Arguments> describe() {
        return new CommandLine(new Cli())
                .getSubcommands()
                .values()
                .stream()
                .map(CommandLine::getCommand)
                .filter(c -> c instanceof AbstractFeature)
                .map(c -> (AbstractFeature) c)
                .map(Arguments::of);
    }

    @AfterEach
    void restoreSysIn() {
        System.setIn(originalSystemIn);
    }

    @ParameterizedTest
    @MethodSource("describe")
    void describe(final AbstractFeature feature) {
        assertThat(feature.describe()).isNotEmpty();
    }

    @Test
    void success() {
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        final AbstractFeature f = new MyTestingFeature();
        final ExitCode e = f.call();
        assertThat(e).isEqualTo(ExitCode.SUCCESS);
    }

    private static final class MyTestingFeature extends AbstractFeature {

        @Override
        public String describe() {
            return null;
        }

        @Override
        public void setup() {
            // no need to do anything
        }

        @Override
        public void test() {
            // no need to do anything
        }
    }
}

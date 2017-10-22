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

package com.github.robozonky.installer.panels;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractRunScriptGenerator implements Function<CommandLinePart, String> {

    private final File distributionFolder;

    protected AbstractRunScriptGenerator(final File distributionFolder) {
        this.distributionFolder = distributionFolder;
    }

    private static String assembleJavaOpts(final CommandLinePart commandLine, final String javaOptsPrefix) {
        final Stream<String> properties = commandLine.getProperties().entrySet().stream()
                .map(e -> "-D" + e.getKey() + '=' + e.getValue());
        final Stream<String> jvmArgs = commandLine.getJvmArguments().entrySet().stream()
                .map(e -> '-' + e.getValue().map(v -> e.getKey() + ' ' + v).orElse(e.getKey()));
        return Stream.concat(jvmArgs, properties).collect(Collectors.joining(" ", javaOptsPrefix, "\""));
    }

    private static Collection<String> getScript(final CommandLinePart commandLine,
                                                final BiFunction<String, String, String> envConverter,
                                                final String javaOptsPrefix) {
        final Collection<String> result = new ArrayList<>();
        commandLine.getEnvironmentVariables().forEach((k, v) -> result.add(envConverter.apply(k, v)));
        result.add(assembleJavaOpts(commandLine, javaOptsPrefix));
        return result;
    }

    private String createScript(final CommandLinePart commandLine, final String name) {
        final File subexecutable = new File(distributionFolder, name);
        subexecutable.setExecutable(true);
        return subexecutable.getAbsolutePath() + " " + commandLine.convertOptions();
    }

    public abstract File getRunScript(final File parentFolder);

    protected Collection<String> getCommonScript(final CommandLinePart commandLine,
                                                 final BiFunction<String, String, String> envConverter,
                                                 final String javaOptsPrefix, final String scriptName) {
        final Collection<String> result = getScript(commandLine, envConverter, javaOptsPrefix);
        result.add(this.createScript(commandLine, scriptName));
        return result;
    }
}

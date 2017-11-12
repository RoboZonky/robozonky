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

package com.github.robozonky.installer.panels.scripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.installer.panels.CommandLinePart;
import com.github.robozonky.internal.api.Defaults;
import freemarker.template.TemplateException;

public abstract class RunScriptGenerator implements Function<CommandLinePart, File> {

    private static final Logger LOGGER = Logger.getLogger(RunScriptGenerator.class.getSimpleName());
    protected final File configFile, distributionDirectory;

    protected RunScriptGenerator(final File distributionDirectory, final File configFile) {
        this.configFile = configFile;
        this.distributionDirectory = distributionDirectory;
    }

    public static RunScriptGenerator forWindows(final File distributionDirectory, final File configFile) {
        return new WindowsRunScriptGenerator(distributionDirectory, configFile);
    }

    public static RunScriptGenerator forUnix(final File distributionDirectory, final File configFile) {
        return new UnixRunScriptGenerator(distributionDirectory, configFile);
    }

    private static String assembleJavaOpts(final CommandLinePart commandLine) {
        final Stream<String> properties = commandLine.getProperties().entrySet().stream()
                .map(e -> "-D" + e.getKey() + '=' + e.getValue());
        final Stream<String> jvmArgs = commandLine.getJvmArguments().entrySet().stream()
                .map(e -> '-' + e.getValue().map(v -> e.getKey() + ' ' + v).orElse(e.getKey()));
        return Stream.concat(jvmArgs, properties).collect(Collectors.joining(" "));
    }

    protected File getRootFolder() {
        return this.configFile.getParentFile();
    }

    protected abstract File getRunScript();

    public abstract File getChildRunScript();

    protected File process(final CommandLinePart commandLine, final String templateName,
                           final Function<String, String> finisher) {
        try {
            final String result = TemplateProcessor.INSTANCE.process(templateName, new HashMap<String, Object>() {{
                this.put("root", distributionDirectory.getAbsolutePath());
                this.put("options", configFile.getAbsolutePath());
                this.put("javaOpts", assembleJavaOpts(commandLine));
                this.put("envVars", commandLine.getEnvironmentVariables());
            }});
            final File target = this.getRunScript();
            Files.write(target.toPath(), finisher.apply(result).getBytes(Defaults.CHARSET));
            LOGGER.info("Generated run script: " + target.getAbsolutePath());
            return target;
        } catch (final IOException | TemplateException e) {
            throw new IllegalStateException("Failed creating run script.", e);
        }
    }

    protected File process(final CommandLinePart commandLine, final String templateName) {
        return process(commandLine, templateName, Function.identity());
    }
}

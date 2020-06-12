/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.installer.configuration.scripts;

import static java.util.Map.entry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.util.FileUtil;

import freemarker.template.TemplateException;

public abstract class RunScriptGenerator implements Function<List<String>, File> {

    private static final Logger LOGGER = LogManager.getLogger(RunScriptGenerator.class.getSimpleName());
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

    protected File getRootFolder() {
        return this.configFile.getParentFile();
    }

    protected abstract File getRunScript();

    public abstract File getChildRunScript();

    protected File process(final List<String> javaOpts, final String templateName,
            final Function<String, String> finisher) {
        try {
            final String result = TemplateProcessor.INSTANCE.process(templateName, Map.ofEntries(
                    entry("root", distributionDirectory.getAbsolutePath()),
                    entry("options", configFile.getAbsolutePath()),
                    entry("javaOpts", String.join(" ", javaOpts)),
                    entry("envVars", Collections.emptyMap())));
            final File target = this.getRunScript();
            Files.write(target.toPath(), finisher.apply(result)
                .getBytes(Defaults.CHARSET));
            FileUtil.configurePermissions(target, true);
            LOGGER.info("Generated run script: {}.", target.getAbsolutePath());
            return target;
        } catch (final IOException | TemplateException e) {
            throw new IllegalStateException("Failed creating run script.", e);
        }
    }

    protected File process(final List<String> javaOpts, final String templateName) {
        return process(javaOpts, templateName, Function.identity());
    }
}

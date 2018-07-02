/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.installer.scripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.Maps;
import freemarker.template.TemplateException;

import static com.github.robozonky.internal.util.Maps.entry;

public enum ServiceGenerator implements Function<File, File> {

    SYSTEMD("robozonky-systemd.service"),
    UPSTART("robozonky-upstart.conf");

    private static final String ID = "robozonky";
    private final String filename;

    ServiceGenerator(final String filename) {
        this.filename = filename;
    }

    @Override
    public File apply(final File execScript) {
        try {
            final File root = execScript.getParentFile();
            final String result = TemplateProcessor.INSTANCE.process(filename + ".ftl",
                                                                     Maps.ofEntries(
                                                                             entry("uid", ID),
                                                                             entry("gid", ID),
                                                                             entry("pwd", root.getAbsolutePath()),
                                                                             entry("script", execScript)
                                                                     ));
            final File target = new File(root, filename);
            Files.write(target.toPath(), result.getBytes(Defaults.CHARSET));
            return target.getAbsoluteFile();
        } catch (final IOException | TemplateException e) {
            throw new IllegalStateException("Failed creating service.", e);
        }
    }
}

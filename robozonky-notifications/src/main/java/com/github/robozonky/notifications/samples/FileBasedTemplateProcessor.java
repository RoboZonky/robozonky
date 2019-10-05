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

package com.github.robozonky.notifications.samples;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

public enum FileBasedTemplateProcessor {

    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger(FileBasedTemplateProcessor.class);

    private static Configuration getFreemarkerConfiguration(final File templateFolder) throws IOException {
        final Configuration cfg = TemplateProcessor.getFreemarkerConfiguration();
        cfg.setDirectoryForTemplateLoading(templateFolder);
        return cfg;
    }

    private static File identifyTemplateRoot() {
        // expect the templates in the project root
        final Path path = Path.of("robozonky-notifications", "src", "main", "resources", "com", "github", "robozonky",
                "notifications", "templates", "html");
        LOGGER.info("Checking for templates in {}.", path);
        final File directory = path.toFile();
        if (directory.exists() && directory.isDirectory()) {
            return directory;
        }
        // expect the templates in the module root
        final Path path2 = path.subpath(1, path.getNameCount());
        LOGGER.info("Checking for templates in {}.", path2);
        final File directory2 = path2.toFile();
        if (directory2.exists() && directory2.isDirectory()) {
            return directory2;
        }
        // expect the templates in the current working directory
        LOGGER.info("Checking for templates in current working directory.");
        return new File(System.getProperty("user.dir"));
    }

    private static String process(final Configuration configuration, final String embeddedTemplate,
                                  final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        final Map<String, Object> data = Map.ofEntries(
                entry("timestamp", Date.from(DateUtil.now())),
                entry("robozonkyUrl", Defaults.ROBOZONKY_URL),
                entry("embed", embeddedTemplate),
                entry("data", embeddedData));
        final Template template = configuration.getTemplate("core.ftl");
        final StringWriter sw = new StringWriter();
        template.process(data, sw);
        return sw.toString().trim();
    }

    public String process(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        return process(getFreemarkerConfiguration(identifyTemplateRoot()), embeddedTemplate, embeddedData);
    }

}

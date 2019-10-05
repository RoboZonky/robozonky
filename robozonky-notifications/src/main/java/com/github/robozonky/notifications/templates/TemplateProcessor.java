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

package com.github.robozonky.notifications.templates;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.notifications.templates.html.HtmlTemplate;
import com.github.robozonky.notifications.templates.plaintext.PlainTextTemplate;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static java.util.Map.entry;

public enum TemplateProcessor {

    INSTANCE;

    private static final Logger LOGGER = LogManager.getLogger(TemplateProcessor.class);
    private static final Configuration PLAIN_TEXT_CONFIG = getFreemarkerConfiguration(PlainTextTemplate.class);
    private static final Configuration HTML_CLASSPATH_CONFIG = getFreemarkerConfiguration(HtmlTemplate.class);

    private static Configuration getFreemarkerConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        final Map<String, TemplateNumberFormatFactory> customNumberFormats =
                Collections.singletonMap("interest", InterestNumberFormatFactory.INSTANCE);
        cfg.setCustomNumberFormats(customNumberFormats);
        cfg.setLogTemplateExceptions(false);
        /*
         * This is important! We don't control installer's encoding, it will always be selected by the user running the
         * installer. Since templates are encoded in UTF-8, and e-mails are sent in UTF-8, we must set this here so that
         * the templates are read properly and installer e-mails are proper.
         *
         * This is not a problem within the daemon itself, since that will be run after the installer is run. And the
         * installer will enforce that the correct encoding is used when the daemon is run.
         */
        cfg.setDefaultEncoding(Defaults.CHARSET.displayName());
        return cfg;
    }

    static Configuration getFreemarkerConfiguration(final Class<?> templateRoot) {
        final Configuration cfg = getFreemarkerConfiguration();
        cfg.setClassForTemplateLoading(templateRoot, "");
        return cfg;
    }

    static Configuration getFreemarkerConfiguration(final File templateFolder) throws IOException {
        final Configuration cfg = getFreemarkerConfiguration();
        cfg.setDirectoryForTemplateLoading(templateFolder);
        return cfg;
    }

    private static String process(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        return process(PLAIN_TEXT_CONFIG, embeddedTemplate, embeddedData);
    }

    private static File identifyTemplateRoot() {
        // expect the templates in the project root
        final Path path = Path.of("robozonky-notifications","src", "main", "resources", "com", "github", "robozonky",
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

    private static String process(final String embeddedTemplate, final Map<String, Object> embeddedData,
                                  final boolean fromFile)
            throws IOException, TemplateException {
        if (fromFile) {
            final File source = identifyTemplateRoot();
            final Configuration configuration = getFreemarkerConfiguration(source);
            return process(configuration, embeddedTemplate, embeddedData);
        } else {
            return process(HTML_CLASSPATH_CONFIG, embeddedTemplate, embeddedData);
        }
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

    public String processPlainText(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        return process(embeddedTemplate, embeddedData);
    }

    public String processHtml(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        return process(embeddedTemplate, embeddedData, false);
    }

    public String processHtml(final String embeddedTemplate, final Map<String, Object> embeddedData,
                              final boolean fromFile)
            throws IOException, TemplateException {
        return process(embeddedTemplate, embeddedData, fromFile);
    }

}

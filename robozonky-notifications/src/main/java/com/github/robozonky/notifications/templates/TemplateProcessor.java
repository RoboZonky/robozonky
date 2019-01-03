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

package com.github.robozonky.notifications.templates;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.internal.util.Maps;
import com.github.robozonky.notifications.templates.html.HtmlTemplate;
import com.github.robozonky.notifications.templates.plaintext.PlainTextTemplate;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import static com.github.robozonky.internal.util.Maps.entry;

public enum TemplateProcessor {

    INSTANCE;

    private final Configuration config = TemplateProcessor.getFreemarkerConfiguration(PlainTextTemplate.class);
    private final Configuration htmlConfig = TemplateProcessor.getFreemarkerConfiguration(HtmlTemplate.class);

    static Configuration getFreemarkerConfiguration(final Class<?> templateRoot) {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        final Map<String, TemplateNumberFormatFactory> customNumberFormats =
                Collections.singletonMap("interest", InterestNumberFormatFactory.INSTANCE);
        cfg.setCustomNumberFormats(customNumberFormats);
        cfg.setClassForTemplateLoading(templateRoot, "");
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

    private static String process(final Configuration configuration, final String embeddedTemplate,
                                  final Map<String, Object> embeddedData) throws IOException, TemplateException {
        final Map<String, Object> data = Maps.ofEntries(
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
        return process(config, embeddedTemplate, embeddedData);
    }

    public String processHtml(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        return process(htmlConfig, embeddedTemplate, embeddedData);
    }

}

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
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.notifications.templates.plaintext.PlainTextTemplate;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public enum TemplateProcessor {

    INSTANCE;

    private final Configuration config = TemplateProcessor.getFreemarkerConfiguration();

    static Configuration getFreemarkerConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        final Map<String, TemplateNumberFormatFactory> customNumberFormats =
                Collections.singletonMap("interest", InterestNumberFormatFactory.INSTANCE);
        cfg.setCustomNumberFormats(customNumberFormats);
        cfg.setClassForTemplateLoading(PlainTextTemplate.class, "");
        cfg.setLogTemplateExceptions(false);
        cfg.setDefaultEncoding(Defaults.CHARSET.displayName());
        return cfg;
    }

    public String process(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        final Map<String, Object> data = new HashMap<String, Object>() {{
            put("timestamp", Date.from(Instant.now()));
            put("robozonkyUrl", Defaults.ROBOZONKY_URL);
            put("embed", embeddedTemplate);
            put("data", embeddedData);
        }};
        final Template template = this.config.getTemplate("core.ftl");
        final StringWriter sw = new StringWriter();
        template.process(data, sw);
        return sw.toString().trim();
    }

}

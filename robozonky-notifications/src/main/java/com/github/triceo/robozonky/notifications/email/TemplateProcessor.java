/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.github.triceo.robozonky.internal.api.Defaults;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

enum TemplateProcessor {

    INSTANCE;

    static Configuration getFreemarkerConfiguration(final Charset charset) {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_26);
        cfg.setClassForTemplateLoading(TemplateProcessor.class, "");
        cfg.setLogTemplateExceptions(false);
        cfg.setTimeZone(TimeZone.getTimeZone(Defaults.ZONE_ID));
        cfg.setLocale(Defaults.LOCALE);
        // don't give Freemarker an option to use any other encoding than the specified, ever
        cfg.setDefaultEncoding(charset.displayName());
        cfg.setEncoding(cfg.getLocale(), cfg.getDefaultEncoding());
        cfg.setOutputEncoding(cfg.getDefaultEncoding());
        return cfg;
    }

    static Configuration getFreemarkerConfiguration() {
        return TemplateProcessor.getFreemarkerConfiguration(Defaults.CHARSET);
    }

    private final Configuration config = TemplateProcessor.getFreemarkerConfiguration();

    public String process(final String embeddedTemplate, final Map<String, Object> embeddedData)
            throws IOException, TemplateException {
        final Map<String, Object> data = new HashMap<>();
        data.put("timestamp", Date.from(Instant.now()));
        data.put("robozonkyUrl", Defaults.ROBOZONKY_URL);
        data.put("embed", embeddedTemplate);
        data.put("data", embeddedData);
        final Template template = this.config.getTemplate("core.ftl");
        final StringWriter sw = new StringWriter();
        template.process(data, sw);
        return sw.toString().trim();
    }

}

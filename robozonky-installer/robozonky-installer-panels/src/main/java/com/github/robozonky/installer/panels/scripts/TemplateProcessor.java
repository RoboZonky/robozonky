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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

enum TemplateProcessor {

    INSTANCE;

    private final Configuration config = TemplateProcessor.getFreemarkerConfiguration();

    static Configuration getFreemarkerConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_27);
        cfg.setClassForTemplateLoading(TemplateProcessor.class, "");
        cfg.setLogTemplateExceptions(false);
        return cfg;
    }

    public String process(final String templateFile,
                          final Map<String, Object> data) throws IOException, TemplateException {
        final Template template = this.config.getTemplate(templateFile);
        final StringWriter sw = new StringWriter();
        template.process(Collections.singletonMap("data", data), sw);
        return sw.toString().trim();
    }

}

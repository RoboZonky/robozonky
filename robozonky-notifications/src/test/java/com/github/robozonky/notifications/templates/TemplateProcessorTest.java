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

package com.github.robozonky.notifications.templates;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.notifications.templates.html.HtmlTemplate;
import com.github.robozonky.notifications.templates.plaintext.PlainTextTemplate;

import freemarker.template.Configuration;

class TemplateProcessorTest {

    @Test
    void properPlainTextConfiguration() {
        final Configuration configuration = TemplateProcessor.getFreemarkerConfiguration(PlainTextTemplate.class);
        final String targetEncoding = Defaults.CHARSET.toString();
        assertSoftly(softly -> {
            softly.assertThat(configuration.getCustomNumberFormat("interest"))
                .isInstanceOf(InterestNumberFormatFactory.class);
            softly.assertThat(configuration.getLogTemplateExceptions())
                .isFalse();
            softly.assertThat(configuration.getDefaultEncoding())
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Locale.getDefault()))
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Locale.ENGLISH))
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Defaults.LOCALE))
                .isEqualTo(targetEncoding);
        });
    }

    @Test
    void properHtmlConfiguration() {
        final Configuration configuration = TemplateProcessor.getFreemarkerConfiguration(HtmlTemplate.class);
        final String targetEncoding = Defaults.CHARSET.toString();
        assertSoftly(softly -> {
            softly.assertThat(configuration.getCustomNumberFormat("interest"))
                .isInstanceOf(InterestNumberFormatFactory.class);
            softly.assertThat(configuration.getLogTemplateExceptions())
                .isFalse();
            softly.assertThat(configuration.getDefaultEncoding())
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Locale.getDefault()))
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Locale.ENGLISH))
                .isEqualTo(targetEncoding);
            softly.assertThat(configuration.getEncoding(Defaults.LOCALE))
                .isEqualTo(targetEncoding);
        });
    }
}

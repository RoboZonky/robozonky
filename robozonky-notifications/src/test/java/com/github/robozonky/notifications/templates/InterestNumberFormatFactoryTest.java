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

import java.math.BigDecimal;
import java.util.Locale;

import com.github.robozonky.internal.api.Defaults;
import freemarker.core.Environment;
import freemarker.core.InvalidFormatParametersException;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Portions of this test are disabled on Java 8, as the percentage formatting for CZ locale changed in Java 9 and the
 * test would just be too complicated if we wanted to cover both scenarios. (The change is that there was a space added
 * between the number and the '%' character, which is the proper CZ way of doing this.
 */
class InterestNumberFormatFactoryTest {

    @DisabledOnJre(JRE.JAVA_8)
    @Test
    void formattingCzech() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.0001");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0,01" + (char) 160 + "%");
    }

    @DisabledOnJre(JRE.JAVA_8)
    @Test
    void formattingCzech2() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.0000");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0,00" + (char) 160 + "%");
    }

    @DisabledOnJre(JRE.JAVA_8)
    @Test
    void formattingCzech3() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.001");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0,10" + (char) 160 + "%");
    }

    @Test
    void formattingEnglish() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.0001");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Locale.ENGLISH,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final Object result = f.formatToPlainText(m);
        assertThat(result).isEqualTo("0.01%");
    }

    @Test
    void formattingEnglishZero() throws TemplateValueFormatException, TemplateModelException {
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Locale.ENGLISH,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> BigDecimal.ZERO;
        final Object result = f.formatToPlainText(m);
        assertThat(result).isEqualTo("0.00%");
    }

    @Test
    void formattingEnglishRoundingUp() throws TemplateValueFormatException, TemplateModelException {
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Locale.ENGLISH,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> new BigDecimal("0.00049");
        final Object result = f.formatToPlainText(m);
        assertThat(result).isEqualTo("0.05%");
    }

    @Test
    void formattingEnglishRoundingDown() throws TemplateValueFormatException, TemplateModelException {
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Locale.ENGLISH,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> new BigDecimal("0.00051");
        final Object result = f.formatToPlainText(m);
        assertThat(result).isEqualTo("0.05%");
    }

    @Test
    void formal() throws TemplateValueFormatException {
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Locale.ENGLISH,
                                                                                Environment.getCurrentEnvironment());
        assertSoftly(softly -> {
            softly.assertThat(f.isLocaleBound()).isTrue();
            softly.assertThat(f.getDescription())
                    .isNotNull()
                    .isNotEmpty();
            softly.assertThatThrownBy(() -> InterestNumberFormatFactory.INSTANCE.get("someparam", Locale.ENGLISH,
                                                                                     Environment
                                                                                             .getCurrentEnvironment()))
                    .isInstanceOf(InvalidFormatParametersException.class);
        });
    }
}

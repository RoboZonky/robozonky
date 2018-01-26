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

package com.github.robozonky.notifications.email;

import java.math.BigDecimal;
import java.util.Locale;

import com.github.robozonky.internal.api.Defaults;
import freemarker.core.Environment;
import freemarker.core.InvalidFormatParametersException;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;

class InterestNumberFormatFactoryTest {

    @BeforeAll
    static void checkJava9() { // turns out that CZ locale outputs on Java 8 are different
        // TODO remove when Java 9 is the minimum platform
        Assumptions.assumeTrue(System.getProperty("java.version").startsWith("9"), () -> "Need Java 9 to run.");
    }

    @Test
    void formattingCzech() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.0001");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0,01" + (char) 160 + "%");
    }

    @Test
    void formattingCzech2() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.0000");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0" + (char) 160 + "%");
    }

    @Test
    void formattingCzech3() throws TemplateValueFormatException, TemplateModelException {
        final BigDecimal n = new BigDecimal("0.001");
        final TemplateNumberFormat f = InterestNumberFormatFactory.INSTANCE.get("", Defaults.LOCALE,
                                                                                Environment.getCurrentEnvironment());
        final TemplateNumberModel m = () -> n;
        final String result = f.formatToPlainText(m);
        assertThat(result.trim()).isEqualTo("0,1" + (char) 160 + "%");
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

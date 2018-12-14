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

import java.text.NumberFormat;
import java.util.Locale;

import freemarker.core.Environment;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import freemarker.core.UnformattableValueException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

class InterestNumberFormatFactory extends TemplateNumberFormatFactory {

    public static final InterestNumberFormatFactory INSTANCE = new InterestNumberFormatFactory();

    private InterestNumberFormatFactory() {
        // singleton
    }

    @Override
    public TemplateNumberFormat get(final String params, final Locale locale,
                                    final Environment env) throws TemplateValueFormatException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new InterestNumberFormatFactory.InterestNumberFormat(locale);
    }

    private static final class InterestNumberFormat extends TemplateNumberFormat {

        private final NumberFormat format;

        private InterestNumberFormat(final Locale locale) {
            format = NumberFormat.getPercentInstance(locale);
            format.setMinimumFractionDigits(2);
        }

        @Override
        public String formatToPlainText(final TemplateNumberModel numberModel)
                throws UnformattableValueException, TemplateModelException {
            return format.format(TemplateFormatUtil.getNonNullNumber(numberModel));
        }

        @Override
        public boolean isLocaleBound() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Percentage to at most two decimal points.";
        }
    }
}

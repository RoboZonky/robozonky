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

package com.github.robozonky.installer;

import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AbstractValidatorTest {

    private static class TestValidator extends AbstractValidator {

        @Override
        public String getErrorMessageId() {
            return null;
        }

        @Override
        protected DataValidator.Status validateDataPossiblyThrowingException(final InstallData installData) {
            throw new IllegalStateException();
        }
    }

    @Test
    void doesNotThrow() {
        final AbstractValidator v = new TestValidator();
        assertThat(v.validateData(null)).isEqualTo(DataValidator.Status.ERROR);
        assertThat(v.getWarningMessageId()).isEmpty();
    }
}

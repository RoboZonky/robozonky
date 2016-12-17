/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.configuration;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ConfirmationCredentialsTest {

    @Test
    public void fullCredentials() {
        final String first = "zonkoid", second = "password";
        final ConfirmationCredentials cc = new ConfirmationCredentials(first + ":" + second);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cc.getToolId()).isEqualTo(first);
        softly.assertThat(cc.getToken()).contains(second.toCharArray());
        softly.assertAll();
    }

    @Test
    public void tokenLess() {
        final String first = "zonkoid";
        final ConfirmationCredentials cc = new ConfirmationCredentials(first);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cc.getToolId()).isEqualTo(first);
        softly.assertThat(cc.getToken()).isEmpty();
        softly.assertAll();
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrong() {
        final String wrong = "zonkoid:password:extra";
        new ConfirmationCredentials(wrong);
    }

}

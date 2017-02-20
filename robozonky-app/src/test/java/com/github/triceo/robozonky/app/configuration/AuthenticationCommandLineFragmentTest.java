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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;

import com.beust.jcommander.ParameterException;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class AuthenticationCommandLineFragmentTest {

    @Test
    public void defaults() {
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment();
        SoftAssertions.assertSoftly(softly -> {
            Assertions.assertThat(fragment.getUsername()).isEmpty();
            Assertions.assertThat(fragment.getKeystore()).isEmpty();
        });
        Assertions.assertThatThrownBy(fragment::validate).isInstanceOf(ParameterException.class);
    }

    @Test
    public void bothSet() {
        final String username = "usr";
        final File keystore = new File("");
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment(username, keystore);
        SoftAssertions.assertSoftly(softly -> {
            Assertions.assertThat(fragment.getUsername()).hasValue(username);
            Assertions.assertThat(fragment.getKeystore()).hasValue(keystore);
        });
        Assertions.assertThatThrownBy(fragment::validate).isInstanceOf(ParameterException.class);
    }

    @Test
    public void userSet() {
        final String username = "usr";
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment(username, null);
        SoftAssertions.assertSoftly(softly -> {
            Assertions.assertThat(fragment.getUsername()).hasValue(username);
            Assertions.assertThat(fragment.getKeystore()).isEmpty();
        });
        fragment.validate();
    }

    @Test
    public void keystoreSet() {
        final File keystore = new File("");
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment(null, keystore);
        SoftAssertions.assertSoftly(softly -> {
            Assertions.assertThat(fragment.getUsername()).isEmpty();
            Assertions.assertThat(fragment.getKeystore()).hasValue(keystore);
        });
        fragment.validate();
    }

    @Test
    public void refreshEnabled() {
        final File keystore = new File("");
        final AuthenticationCommandLineFragment fragment =
                new AuthenticationCommandLineFragment("username", keystore, true);
        Assertions.assertThat(fragment.createAuthenticationHandler(SecretProvider.fallback("username", new char[0])))
                .isNotNull();
    }

}

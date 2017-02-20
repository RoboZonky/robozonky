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

import java.util.Optional;
import java.util.UUID;

import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ConfirmationCredentialsTest {

    private static final SecretProvider SECRETS = Mockito.mock(SecretProvider.class);

    @Test
    public void fullCredentials() {
        final AuthenticationCommandLineFragment cliAuth = Mockito.mock(AuthenticationCommandLineFragment.class);
        Mockito.when(cliAuth.getUsername()).thenReturn(Optional.of(UUID.randomUUID().toString()));
        Mockito.when(cliAuth.getPassword()).thenReturn(new char[0]);
        final CommandLineInterface cli = Mockito.mock(CommandLineInterface.class);
        Mockito.when(cli.getAuthenticationFragment()).thenReturn(cliAuth);
        final SecretProvider secretProvider = SecretProviderFactory.getFallbackSecretProvider(cli).get();
        final String first = "zonkoid", second = "password";
        final Credentials cc = new Credentials(first + ":" + second, secretProvider);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(cc.getToolId()).isEqualTo(first);
            softly.assertThat(cc.getToken()).contains(second.toCharArray());
        });
    }

    @Test
    public void tokenLess() {
        final String first = "zonkoid";
        final Credentials cc = new Credentials(first, ConfirmationCredentialsTest.SECRETS);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cc.getToolId()).isEqualTo(first);
        softly.assertThat(cc.getToken()).isEmpty();
        softly.assertAll();
    }

    @Test
    public void wrong() {
        final String wrong = "zonkoid:password:extra";
        Assertions.assertThatThrownBy(() -> new Credentials(wrong, null)).isInstanceOf(IllegalArgumentException.class);
    }

}

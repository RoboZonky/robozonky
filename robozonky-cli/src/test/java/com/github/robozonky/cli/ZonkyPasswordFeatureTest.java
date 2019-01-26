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

package com.github.robozonky.cli;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ZonkyPasswordFeatureTest {

    private static final String KEYSTORE_PASSWORD = "pwd";

    private static File newTempFile() throws IOException {
        final File f = File.createTempFile("robozonky-", ".keystore");
        f.delete();
        return f;
    }

    @Test
    void createNew() throws IOException, SetupFailedException, KeyStoreException {
        final File f = newTempFile();
        final String username = "someone@somewhere.cz";
        final String pwd = UUID.randomUUID().toString();
        final Feature feature = new ZonkyPasswordFeature(f, KEYSTORE_PASSWORD.toCharArray(), username,
                                                         pwd.toCharArray());
        feature.setup();
        final SecretProvider s = SecretProvider.keyStoreBased(KeyStoreHandler.open(f, KEYSTORE_PASSWORD.toCharArray()));
        assertSoftly(softly -> {
            softly.assertThat(s.getUsername()).isEqualTo(username);
            softly.assertThat(s.getPassword()).isEqualTo(pwd.toCharArray());
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ApiProvider mockApi(final String username, final char... password) {
        final ApiProvider api = spy(new ApiProvider());
        final ZonkyApiToken token = mock(ZonkyApiToken.class);
        when(token.getAccessToken()).thenReturn(new char[0]);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(eq(username), eq(password))).thenReturn(token);
        doAnswer(i -> {
            final Function f = i.getArgument(0);
            return f.apply(oauth);
        }).when(api).oauth(any());
        final Zonky z = mock(Zonky.class);
        doAnswer(i -> {
            final Consumer f = i.getArgument(0);
            f.accept(z);
            return null;
        }).when(api).run(any(Consumer.class), any());
        return api;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ApiProvider mockFailingApi() {
        final ApiProvider api = spy(new ApiProvider());
        final ZonkyApiToken token = mock(ZonkyApiToken.class);
        when(token.getAccessToken()).thenReturn(new char[0]);
        final OAuth oauth = mock(OAuth.class);
        when(oauth.login(any(), any())).thenReturn(token);
        doAnswer(i -> {
            final Function f = i.getArgument(0);
            return f.apply(oauth);
        }).when(api).oauth(any());
        final Zonky z = mock(Zonky.class);
        doAnswer(i -> {
            final Consumer f = i.getArgument(0);
            f.accept(z);
            return null;
        }).when(api).run(any(Consumer.class), any());
        doThrow(IllegalStateException.class).when(z).logout(); // last call will fail
        return api;
    }

    @SuppressWarnings("unchecked")
    @Test
    void testWorks() throws IOException, TestFailedException, SetupFailedException {
        final File f = newTempFile();
        final String username = "someone@somewhere.cz";
        final String pwd = UUID.randomUUID().toString();
        final ApiProvider api = mockApi(username, pwd.toCharArray());
        final Feature feature = new ZonkyPasswordFeature(api, f, KEYSTORE_PASSWORD.toCharArray(), username,
                                                         pwd.toCharArray());
        feature.setup();
        feature.test();
        verify(api).oauth(any());
        verify(api).run(any(Consumer.class), any());
    }

    @Test
    void testFailsRemotely() throws IOException, SetupFailedException {
        final File f = newTempFile();
        final String username = "someone@somewhere.cz";
        final String pwd = UUID.randomUUID().toString();
        final ApiProvider api = mockFailingApi();
        final Feature feature = new ZonkyPasswordFeature(api, f, KEYSTORE_PASSWORD.toCharArray(), username,
                                                         pwd.toCharArray());
        feature.setup();
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class); // remote failure caught
    }

    @Test
    void standaloneTestFails() throws IOException {
        final File f = newTempFile();
        final String username = "someone@somewhere.cz";
        final String pwd = UUID.randomUUID().toString();
        final ApiProvider api = mockApi(username, pwd.toCharArray());
        final Feature feature = new ZonkyPasswordFeature(api, f, KEYSTORE_PASSWORD.toCharArray(), username,
                                                         pwd.toCharArray());
        assertThatThrownBy(feature::test).isInstanceOf(TestFailedException.class); // no keystore exists
    }
}

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

package com.github.robozonky.app.configuration;

import java.io.File;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationCommandLineFragmentTest {

    @Test
    void defaults() {
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment();
        assertThat(fragment.getKeystore()).isEmpty();
    }

    @Test
    void keystoreSet() {
        final File keystore = new File("");
        final AuthenticationCommandLineFragment fragment = new AuthenticationCommandLineFragment(keystore);
        assertThat(fragment.getKeystore()).hasValue(keystore);
    }

}

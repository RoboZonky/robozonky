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

package com.github.robozonky.api.remote.enums;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OAuthScopesTest {

    @Test
    void empty() {
        final OAuthScopes result = OAuthScopes.valueOf("");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getPrimaryScope()).isEmpty();
            softly.assertThat(result.getOAuthScopes()).isEmpty();
        });
    }

    @Test
    void emptyNotTrimmed() {
        final OAuthScopes result = OAuthScopes.valueOf(" ");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getPrimaryScope()).isEmpty();
            softly.assertThat(result.getOAuthScopes()).isEmpty();
        });
    }

    @Test
    void oneValue() {
        final OAuthScopes result = OAuthScopes.valueOf("SCOPE_APP_WEB");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getPrimaryScope()).contains(OAuthScope.SCOPE_APP_WEB);
            softly.assertThat(result.getOAuthScopes()).containsOnly(OAuthScope.SCOPE_APP_WEB);
        });
    }

    @Test
    void twoValues() {
        final OAuthScopes result = OAuthScopes.valueOf("SCOPE_APP_OAUTH SCOPE_APP_WEB");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getPrimaryScope()).contains(OAuthScope.SCOPE_APP_WEB);
            softly.assertThat(result.getOAuthScopes()).containsOnly(OAuthScope.SCOPE_APP_WEB,
                                                                    OAuthScope.SCOPE_APP_OAUTH);
        });
    }

    @Test
    void preference() {
        final OAuthScopes result = OAuthScopes.valueOf("SCOPE_APP_OAUTH SCOPE_APP_WEB SCOPE_FILE_DOWNLOAD");
        assertThat(result.getPrimaryScope()).contains(OAuthScope.SCOPE_APP_WEB);
    }

    @Test
    void preference2() {
        final OAuthScopes result = OAuthScopes.valueOf("SCOPE_APP_OAUTH SCOPE_FILE_DOWNLOAD");
        assertThat(result.getPrimaryScope()).contains(OAuthScope.SCOPE_FILE_DOWNLOAD);
    }

    @Test
    void preference3() {
        final OAuthScopes result = OAuthScopes.valueOf("SCOPE_APP_OAUTH");
        assertThat(result.getPrimaryScope()).contains(OAuthScope.SCOPE_APP_OAUTH);
    }

    @Test
    void wrongValue() {
        assertThatThrownBy(() -> OAuthScopes.valueOf(UUID.randomUUID().toString()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofEmpty() {
        final OAuthScopes result = OAuthScopes.of();
        assertThat(result.getOAuthScopes()).isEmpty();
    }

    @Test
    void ofSome() {
        final OAuthScopes result = OAuthScopes.of(OAuthScope.SCOPE_APP_WEB, OAuthScope.SCOPE_APP_OAUTH);
        assertThat(result.getOAuthScopes()).containsExactly(OAuthScope.SCOPE_APP_OAUTH, OAuthScope.SCOPE_APP_WEB);
    }

    @Test
    void equals() {
        final OAuthScopes result = OAuthScopes.of(OAuthScope.SCOPE_APP_WEB, OAuthScope.SCOPE_APP_OAUTH);
        assertThat(result).isNotEqualTo(null);
        assertThat(result).isEqualTo(result);
        final OAuthScopes result2 = OAuthScopes.of(OAuthScope.SCOPE_APP_WEB, OAuthScope.SCOPE_APP_OAUTH);
        assertThat(result).isEqualTo(result2);
    }
}

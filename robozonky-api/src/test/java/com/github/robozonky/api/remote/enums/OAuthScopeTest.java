package com.github.robozonky.api.remote.enums;

import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class OAuthScopeTest {

    @Test
    void findByCode() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(OAuthScope.findByCode("SCOPE_APP_OAUTH")).isEqualTo(OAuthScope.SCOPE_APP_OAUTH);
            softly.assertThat(OAuthScope.findByCode("SCOPE_APP_WEB")).isEqualTo(OAuthScope.SCOPE_APP_WEB);
            softly.assertThat(OAuthScope.findByCode("SCOPE_FILE_DOWNLOAD")).isEqualTo(OAuthScope.SCOPE_FILE_DOWNLOAD);
            softly.assertThatThrownBy(() -> OAuthScope.findByCode(UUID.randomUUID().toString()))
                    .isInstanceOf(IllegalArgumentException.class);
            softly.assertThatThrownBy(() -> OAuthScope.findByCode(null)).isInstanceOf(IllegalArgumentException.class);
        });
    }

}

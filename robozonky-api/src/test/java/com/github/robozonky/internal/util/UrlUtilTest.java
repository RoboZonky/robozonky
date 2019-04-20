package com.github.robozonky.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UrlUtilTest {

    @Test
    void wrongUrl() {
        assertThatThrownBy(() -> UrlUtil.open(new URL("http://" + UUID.randomUUID()))).isInstanceOf(IOException.class);
    }

    @Test
    void correctUrl() throws IOException {
        try (final InputStream s = UrlUtil.open(new URL("http://www.google.com"))) {
            assertThat(s).isNotNull();
        }
    }

}

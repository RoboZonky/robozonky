package com.github.robozonky.installer;

import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UrlValidatorTest {

    private final ProcessingClient client = mock(ProcessingClient.class);
    private final UrlValidator validator = new UrlValidator();

    @Test
    void testCorrect() {
        when(client.getText()).thenReturn("http://www.robozonky.cz");
        assertThat(validator.validate(client)).isTrue();
    }

    @Test
    void testIncorrect() {
        when(client.getText()).thenReturn("adsf");
        assertThat(validator.validate(client)).isFalse();
    }

    @Test
    void testEmpty() {
        when(client.getText()).thenReturn("");
        assertThat(validator.validate(client)).isFalse();
    }

    @Test
    void testNull() {
        assertThat(validator.validate(client)).isFalse();
    }
}

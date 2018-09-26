/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.util;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IoUtilTest {

    @Test
    void closes() throws IOException {
        final InputStream s = mock(InputStream.class);
        final int result = IoUtil.tryFunction(() -> s, InputStream::read);
        assertThat(result).isEqualTo(0);
        verify(s).close();
    }

    @Test
    void closesOnException() throws IOException {
        final InputStream s = mock(InputStream.class);
        assertThatThrownBy(() -> IoUtil.tryFunction(() -> s, x -> {
            throw new IOException("Testing");
        })).isInstanceOf(IOException.class);
        verify(s).close();
    }

    @Test
    void consumerCloses() throws IOException {
        final InputStream s = mock(InputStream.class);
        IoUtil.tryConsumer(() -> s, InputStream::read);
        verify(s).close();
    }

    @Test
    void consumerClosesOnException() throws IOException {
        final InputStream s = mock(InputStream.class);
        assertThatThrownBy(() -> IoUtil.tryConsumer(() -> s, x -> {
            throw new IOException("Testing");
        })).isInstanceOf(IOException.class);
        verify(s).close();
    }

}

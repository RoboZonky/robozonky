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

package com.github.robozonky.integrations.zonkoid;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class UtilTest {

    @Test
    void success() {
        assertSoftly(softly -> {
            softly.assertThat(Util.isHttpSuccess(199)).isFalse();
            softly.assertThat(Util.isHttpSuccess(200)).isTrue();
            softly.assertThat(Util.isHttpSuccess(299)).isTrue();
            softly.assertThat(Util.isHttpSuccess(300)).isFalse();
        });
    }

    @Test
    void responseFails() throws IOException {
        final HttpEntity e = mock(HttpEntity.class);
        doThrow(IOException.class).when(e).writeTo(any());
        assertThat(Util.readEntity(e)).isNull();
    }

    @Test
    void responseEmpty() {
        assertThat(Util.readEntity(null)).isNull();
    }

    @Test
    void responseContent() {
        final Collection<NameValuePair> nvp = Collections.singletonList(new BasicNameValuePair("key", "value"));
        final HttpEntity e = new UrlEncodedFormEntity(nvp);
        assertThat(Util.readEntity(e)).isNotEmpty();
    }
}

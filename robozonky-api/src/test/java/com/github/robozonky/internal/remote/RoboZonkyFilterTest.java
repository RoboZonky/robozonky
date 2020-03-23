/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class RoboZonkyFilterTest extends AbstractCommonFilterTest {

    @Test
    void rebuildUri() throws URISyntaxException {
        final URI u = new URI("http://localhost/somewhere/something?param1=b&param2=c");
        final URI u2 = RoboZonkyFilter.addQueryParams(u, Collections.singletonMap("param2", new Object[] { 1, 2 }));
        assertThat(u2).isNotEqualTo(u);
    }

    @Override
    protected RoboZonkyFilter getTestedFilter() {
        return new RoboZonkyFilter();
    }

}

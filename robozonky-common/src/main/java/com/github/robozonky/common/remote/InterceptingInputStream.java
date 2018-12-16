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

package com.github.robozonky.common.remote;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.github.robozonky.internal.api.Defaults;

class InterceptingInputStream extends InputStream {

    private static final int MAX_ENTITY_SIZE = 1024;

    private final InputStream source;
    private final String intercepted;

    public InterceptingInputStream(final InputStream source) throws IOException {
        this.source = new BufferedInputStream(source); // to ensure mark() is supported
        this.source.mark(MAX_ENTITY_SIZE + 1);
        final byte[] entity = new byte[MAX_ENTITY_SIZE + 1];
        final int entitySize = this.source.read(entity);
        final StringBuilder s = new StringBuilder(MAX_ENTITY_SIZE);
        s.append(new String(entity, 0, Math.min(entitySize, MAX_ENTITY_SIZE), Defaults.CHARSET));
        if (entitySize > MAX_ENTITY_SIZE) {
            s.append("...more...");
        }
        this.intercepted = s.toString();
        this.source.reset();
    }

    public String getContents() {
        return intercepted;
    }

    @Override
    public int read() throws IOException {
        return source.read();
    }
}

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

package com.github.robozonky.common.state;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.util.TextUtil;

public final class TenantState {

    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private final StateStorage underlying;
    private final FileBackedStateStorage current;

    TenantState(final String username, final StateStorage underlying) { // no external instances
        this.underlying = underlying;
        this.current = new FileBackedStateStorage(getFile(username));
    }

    public static TenantState of(final SessionInfo session) {
        return Holder.of(session);
    }

    public static Collection<String> getKnownTenants() {
        return Holder.getKnownTenants();
    }

    private static File getFile(final String username) {
        final String encoded = TextUtil.md5(username).orElse(username);
        final String filename = "robozonky-" + encoded + ".state";
        return new File(filename);
    }

    public static void destroyAll() {
        Holder.destroy();
    }

    void assertNotDestroyed() {
        if (isDestroyed()) {
            throw new IllegalStateException("Already destroyed.");
        }
    }

    public <T> InstanceState<T> in(final Class<T> cls) {
        assertNotDestroyed();
        return new InstanceStateImpl<>(this, cls.getName(), current, underlying);
    }

    boolean isDestroyed() {
        return isDestroyed.get();
    }

    void destroy() {
        Holder.destroy(this);
        current.destroy();
        isDestroyed.set(true);
    }
}

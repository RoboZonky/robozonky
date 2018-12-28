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

package com.github.robozonky.common.management;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

final class RoboZonkyQueryExp implements QueryExp {

    private final AtomicReference<MBeanServer> server = new AtomicReference<>();

    @Override
    public boolean apply(final ObjectName name) {
        return name.getDomain().equals(ManagementBean.DOMAIN);
    }

    Optional<MBeanServer> getMBeanServer() {
        return Optional.ofNullable(server.get());
    }

    @Override
    public void setMBeanServer(final MBeanServer s) {
        server.set(s);
    }
}

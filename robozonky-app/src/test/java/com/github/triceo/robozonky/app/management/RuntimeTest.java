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

package com.github.triceo.robozonky.app.management;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.app.ShutdownEnabler;
import com.github.triceo.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RuntimeTest {

    @Before
    @After
    public void reset() {
        DaemonInvestmentMode.BLOCK_UNTIL_ZERO.set(new CountDownLatch(1));
        ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.set(new CountDownLatch(1));
    }

    @Test
    public void execute() {
        final Runtime r = new Runtime();
        Assertions.assertThat(r.getVersion()).isEqualTo(Defaults.ROBOZONKY_VERSION);
        final String username = UUID.randomUUID().toString();
        final ExecutionCompletedEvent evt = new ExecutionCompletedEvent(Collections.emptyList(), null);
        r.handle(evt, new SessionInfo(username));
        Assertions.assertThat(r.getLatestUpdatedDateTime()).isBeforeOrEqualTo(OffsetDateTime.now());
        Assertions.assertThat(r.getZonkyUsername()).isEqualTo(username);
        r.stopDaemon();
        Assertions.assertThat(DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get().getCount()).isEqualTo(0);
        Assertions.assertThat(ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.get().getCount()).isEqualTo(0);
    }
}

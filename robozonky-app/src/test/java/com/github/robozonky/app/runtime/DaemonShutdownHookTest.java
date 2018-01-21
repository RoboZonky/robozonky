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

package com.github.robozonky.app.runtime;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import org.junit.Test;
import org.mockito.Mockito;

public class DaemonShutdownHookTest {

    @Test(timeout = 5000)
    public void runtime() throws InterruptedException {
        final Lifecycle lifecycle = Mockito.mock(Lifecycle.class);
        final ShutdownEnabler se = Mockito.mock(ShutdownEnabler.class);
        final DaemonShutdownHook hook = new DaemonShutdownHook(lifecycle, se);
        hook.start();
        se.get().ifPresent(c -> c.accept(new ShutdownHook.Result(ReturnCode.OK, null)));
        while (hook.isAlive()) { // wait until the hook to terminate
            Thread.sleep(1);
        }
        Mockito.verify(se).waitUntilTriggered();
        Mockito.verify(lifecycle).resumeToShutdown();
    }
}

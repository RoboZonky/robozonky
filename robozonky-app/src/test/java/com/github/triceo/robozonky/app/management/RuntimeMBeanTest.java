/*
 * Copyright 2017 Lukáš Petrovický
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

import java.util.concurrent.Semaphore;

import com.github.triceo.robozonky.app.investing.DaemonInvestmentMode;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class RuntimeMBeanTest {

    @Test
    public void unblock() {
        final Semaphore s = DaemonInvestmentMode.BLOCK_UNTIL_RELEASED;
        s.acquireUninterruptibly(s.availablePermits());
        ((Runtime)MBean.RUNTIME.getImplementation()).stopDaemon();
        Assertions.assertThat(s.availablePermits()).isGreaterThan(0);
    }

}



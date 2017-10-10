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

package com.github.robozonky.app.configuration.daemon;

import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.AbstractInvestingTest;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class DaemonOperationTest extends AbstractInvestingTest {

    private static final class CustomOperation extends DaemonOperation {

        public CustomOperation(final Authenticated auth, final Consumer<Zonky> operation) {
            super(auth, operation);
        }
    }

    @Test
    public void exceptional() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        Mockito.doThrow(IllegalStateException.class).when(a).run(ArgumentMatchers.any());
        final DaemonOperation d = new CustomOperation(a, null);
        d.run();
        Assertions.assertThat(this.getNewEvents()).first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    public void standard() {
        final Zonky z = Mockito.mock(Zonky.class);
        final Consumer<Zonky> operation = zonky -> Assertions.assertThat(zonky).isSameAs(z);
        final Authenticated a = Mockito.mock(Authenticated.class);
        final DaemonOperation d = new CustomOperation(a, operation);
        d.run();
        Mockito.verify(a).run(ArgumentMatchers.eq(operation));
    }
}

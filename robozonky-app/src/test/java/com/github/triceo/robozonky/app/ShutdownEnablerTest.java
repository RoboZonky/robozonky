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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ShutdownEnablerTest {

    @Test
    public void proper() {
        Assertions.assertThat(ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.availablePermits()).isEqualTo(1);
        final ShutdownEnabler e = new ShutdownEnabler();
        final Optional<Consumer<ShutdownHook.Result>> result = e.get();
        Assertions.assertThat(ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.availablePermits()).isEqualTo(0);
        result.get().accept(new ShutdownHook.Result(ReturnCode.OK, null));
        Assertions.assertThat(ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.availablePermits()).isEqualTo(1);
    }

}

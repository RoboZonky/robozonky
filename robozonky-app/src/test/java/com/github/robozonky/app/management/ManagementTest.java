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

package com.github.robozonky.app.management;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Consumer;
import javax.management.MBeanServer;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.runtime.Lifecycle;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class ManagementTest {

    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();

    @Test
    public void registerAndUnregister() {
        final int beanCountBeforeRegister = ManagementTest.SERVER.getMBeanCount();
        final Management m = new Management(new Lifecycle());
        final Optional<Consumer<ShutdownHook.Result>> hook = m.get(); // register the mbeans
        final int beanCountAfterRegister = ManagementTest.SERVER.getMBeanCount();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(hook).isPresent();
            softly.assertThat(beanCountAfterRegister).isEqualTo(beanCountBeforeRegister + MBean.values().length);
        });
        hook.get().accept(new ShutdownHook.Result(ReturnCode.OK, null)); // unregister the mbeans
        final int beanCountAfterUnregister = ManagementTest.SERVER.getMBeanCount();
        Assertions.assertThat(beanCountAfterUnregister).isEqualTo(beanCountBeforeRegister);
    }
}

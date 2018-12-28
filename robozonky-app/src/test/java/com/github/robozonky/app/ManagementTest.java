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

package com.github.robozonky.app;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import java.util.function.Consumer;
import javax.management.MBeanServer;

import com.github.robozonky.app.runtime.Runtime;
import com.github.robozonky.app.runtime.RuntimeMBean;
import com.github.robozonky.app.runtime.RuntimeManagementBeanService;
import com.github.robozonky.common.management.ManagementBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ManagementTest extends AbstractEventLeveragingTest {

    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();

    @BeforeEach
    private void initBean() {
        final ManagementBean<Runtime> mb =
                new ManagementBean(RuntimeMBean.class, () -> new Runtime(null));
        RuntimeManagementBeanService.setManagementBean(mb);
    }

    @Test
    void registerAndUnregister() {
        final int beanCountBeforeRegister = ManagementTest.SERVER.getMBeanCount();
        final Management m = new Management();
        final Optional<Consumer<ShutdownHook.Result>> hook = m.get(); // register the mbeans
        final int beanCountAfterRegister = ManagementTest.SERVER.getMBeanCount();
        assertSoftly(softly -> {
            softly.assertThat(hook).isPresent();
            softly.assertThat(beanCountAfterRegister).isGreaterThan(beanCountBeforeRegister);
        });
        hook.get().accept(new ShutdownHook.Result(ReturnCode.OK, null)); // unregister the mbeans
        final int beanCountAfterUnregister = ManagementTest.SERVER.getMBeanCount();
        assertThat(beanCountAfterUnregister).isEqualTo(beanCountBeforeRegister);
    }
}

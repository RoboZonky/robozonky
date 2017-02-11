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

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanServer;
import javax.management.ReflectionException;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.app.management.MBean;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ManagementTest {

    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getMbeans() {
        return Arrays.stream(MBean.values()).map(bean -> new Object[] {bean}).collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public MBean bean;

    @Test
    public void registerAndUnregister() throws IntrospectionException, ReflectionException {
        final Management m = new Management();
        try {
            ManagementTest.SERVER.getMBeanInfo(bean.getObjectName());
            Assertions.fail("The MBean is registered and it shoudln't be.");
        } catch (final InstanceNotFoundException ex) {
            // all is OK.
        }
        final Optional<Consumer<ShutdownHook.Result>> hook = m.get(); // register the mbean
        Assertions.assertThat(hook).isPresent();
        try {
            ManagementTest.SERVER.getMBeanInfo(bean.getObjectName());
        } catch (final InstanceNotFoundException ex) {
            Assertions.fail("MBean was not registered.", ex);
        }
        hook.get().accept(new ShutdownHook.Result(ReturnCode.OK, null)); // unregister the mbean
        try {
            ManagementTest.SERVER.getMBeanInfo(bean.getObjectName());
            Assertions.fail("The MBean was not unregistered.");
        } catch (final InstanceNotFoundException ex) {
            // all is OK.
        }
    }

}

/*
 * Copyright 2019 The RoboZonky Project
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

import java.lang.management.ManagementFactory;
import java.util.Optional;
import javax.management.ObjectName;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ManagementTest {

    private static int countMbeans() {
        return ManagementFactory.getPlatformMBeanServer().getMBeanCount();
    }

    @Test
    void register() {
        final int before = countMbeans();
        final ManagementBean<BaseMBean> mb = new ManagementBean<>(BaseMBean.class, Base::new);
        final Optional<ObjectName> name = Management.register(mb);
        assertThat(name).isNotEmpty()
                .map(ObjectName::getDomain)
                .contains(ManagementBean.DOMAIN);
        final int after = countMbeans();
        assertThat(after).isEqualTo(before + 1);
        Management.unregisterAll();
        assertThat(countMbeans()).isEqualTo(before);
    }

}

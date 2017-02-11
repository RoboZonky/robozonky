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

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MBeanTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getMBeans() {
        return Stream.of(MBean.values()).map(m -> new Object[] {m}).collect(Collectors.toList());
    }

    @Parameterized.Parameter
    public MBean mbean;

    @Test
    public void isValid() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(mbean.getImplementation()).isNotNull();
            softly.assertThat(mbean.getObjectName()).isNotNull();
        });
    }

}

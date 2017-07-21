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

package com.github.triceo.robozonky.strategy.natural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DefaultPortfolioTest {

    @Parameterized.Parameters(name = "{0} + {1}")
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> result = new ArrayList<>(Rating.values().length);
        Stream.of(Rating.values()).forEach(r -> result.add(new Object[]{DefaultPortfolio.EMPTY, r}));
        return result;
    }

    @Parameterized.Parameter
    public DefaultPortfolio p;
    @Parameterized.Parameter(1)
    public Rating r;

    @Test
    public void hasValue() {
        Assertions.assertThat(p.getDefaultShare(r)).isEqualTo(0);
    }

    @Test
    public void unknownValue() {
        Assertions.assertThatThrownBy(() -> p.getDefaultShare(null)).isInstanceOf(IllegalStateException.class);
    }
}

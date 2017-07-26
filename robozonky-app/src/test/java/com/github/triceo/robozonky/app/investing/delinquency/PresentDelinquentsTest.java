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

package com.github.triceo.robozonky.app.investing.delinquency;

import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.AbstractStateLeveragingTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PresentDelinquentsTest extends AbstractStateLeveragingTest {

    @Test
    public void update() {
        final int id = 1;
        final Loan l = new Loan(id, 200);
        final Investment i = new Investment(l, (int) l.getAmount());
        final PresentDelinquents known = new PresentDelinquents();
        Assertions.assertThat(known.get()).isEmpty();
        known.update(Collections.singleton(i));
        Assertions.assertThat(known.get())
                .first().matches(r -> r.getLoanId() == id);
    }
}

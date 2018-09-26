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

package com.github.robozonky.app.daemon;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.state.InstanceState;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionalInstanceStateTest extends AbstractZonkyLeveragingTest {

    @SuppressWarnings("unchecked")
    @Test
    void delegatesKeys() {
        final InstanceState<String> parent = mock(InstanceState.class);
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(createTransactional(), parent);
        s.getKeys();
        verify(parent).getKeys();
    }

}

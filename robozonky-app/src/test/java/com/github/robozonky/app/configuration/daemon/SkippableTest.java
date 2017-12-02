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

import org.junit.Test;
import org.mockito.Mockito;

public class SkippableTest {

    @Test
    public void skips() {
        final Runnable r = Mockito.mock(Runnable.class);
        final Skippable s = new Skippable(r, () -> true);
        s.run();
        Mockito.verify(r, Mockito.never()).run();
    }

    @Test
    public void doesNotSkip() {
        final Runnable r = Mockito.mock(Runnable.class);
        final Skippable s = new Skippable(r, () -> false);
        s.run();
        Mockito.verify(r, Mockito.times(1)).run();
    }
}

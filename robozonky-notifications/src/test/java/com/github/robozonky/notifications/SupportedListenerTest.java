/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.notifications;

import java.util.Arrays;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SupportedListenerTest {

    @Test
    void globalGagOverride() {
        final SupportedListener[] overriding = new SupportedListener[] {
                SupportedListener.UPDATE_DETECTED, SupportedListener.CRASHED,
                SupportedListener.EXPERIMENTAL_UPDATE_DETECTED, SupportedListener.ENDING, SupportedListener.INITIALIZED,
                SupportedListener.WEEKLY_SUMMARY, SupportedListener.DAEMON_RESUMED, SupportedListener.DAEMON_SUSPENDED
        };
        Arrays.stream(SupportedListener.values()).forEach(s -> {
            final boolean shouldOverride = Arrays.asList(overriding).contains(s);
            assertThat(s.overrideGlobalGag()).isEqualTo(shouldOverride);
        });
    }

    @Test
    void nullity() {
        SoftAssertions.assertSoftly(softly -> Stream.of(SupportedListener.values())
                .forEach(l -> {
                    softly.assertThat(l.createSampleEvent()).isNotNull();
                    softly.assertThat(l.getListener(new EmailHandler(null))).isNotNull();
                }));
    }


}

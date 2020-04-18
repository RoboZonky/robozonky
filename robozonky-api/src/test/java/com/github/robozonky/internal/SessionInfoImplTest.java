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

package com.github.robozonky.internal;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Consents;
import com.github.robozonky.api.remote.entities.Restrictions;

class SessionInfoImplTest {

    @Test
    void constructorDryRun() {
        final SessionInfo s = new SessionInfoImpl("someone@somewhere.cz");
        assertSoftly(softly -> {
            softly.assertThat(s.getUsername())
                .isEqualTo("someone@somewhere.cz");
            softly.assertThat(s.isDryRun())
                .isTrue();
            softly.assertThat(s.getName())
                .isEqualTo("RoboZonky 'Test'");
        });
    }

    @Test
    void constructor() {
        var id = UUID.randomUUID()
            .toString();
        var sessionInfo = new SessionInfoImpl("someone@somewhere.cz", id, false);
        assertSoftly(softly -> {
            softly.assertThat(sessionInfo.getUsername())
                .isEqualTo("someone@somewhere.cz");
            softly.assertThat(sessionInfo.isDryRun())
                .isFalse();
            softly.assertThat(sessionInfo.getName())
                .isEqualTo("RoboZonky '" + id + "'");
            softly.assertThat(sessionInfo.canInvest())
                .isTrue();
            softly.assertThat(sessionInfo.canAccessSmp())
                .isTrue();
            softly.assertThat(sessionInfo.getMinimumInvestmentAmount())
                .isEqualTo(Money.from(200));
            softly.assertThat(sessionInfo.getInvestmentStep())
                .isEqualTo(Money.from(200));
            softly.assertThat(sessionInfo.getMaximumInvestmentAmount())
                .isEqualTo(Money.from(5_000));
        });
    }

    @Test
    void constructorRestrictive() {
        var id = UUID.randomUUID()
            .toString();
        var sessionInfo = new SessionInfoImpl(Consents::new, () -> new Restrictions(false), "someone@somewhere.cz", id,
                false);
        assertSoftly(softly -> {
            softly.assertThat(sessionInfo.getUsername())
                .isEqualTo("someone@somewhere.cz");
            softly.assertThat(sessionInfo.isDryRun())
                .isFalse();
            softly.assertThat(sessionInfo.getName())
                .isEqualTo("RoboZonky '" + id + "'");
            softly.assertThat(sessionInfo.canInvest())
                .isFalse();
            softly.assertThat(sessionInfo.canAccessSmp())
                .isFalse();
            softly.assertThat(sessionInfo.getMinimumInvestmentAmount())
                .isEqualTo(Money.from(200));
            softly.assertThat(sessionInfo.getInvestmentStep())
                .isEqualTo(Money.from(200));
            softly.assertThat(sessionInfo.getMaximumInvestmentAmount())
                .isEqualTo(Money.from(5_000));
        });
    }

    @Test
    void constructorNamed() {
        final SessionInfo s = new SessionInfoImpl("someone@somewhere.cz");
        assertSoftly(softly -> {
            softly.assertThat(s.getUsername())
                .isEqualTo("someone@somewhere.cz");
            softly.assertThat(s.isDryRun())
                .isTrue();
            softly.assertThat(s.getName())
                .isEqualTo("RoboZonky 'Test'");
        });
    }

    @Test
    void equals() {
        final SessionInfo s = new SessionInfoImpl("someone@somewhere.cz");
        final SessionInfo s2 = new SessionInfoImpl(UUID.randomUUID()
            .toString());
        final SessionInfo s3 = new SessionInfoImpl("someone@somewhere.cz");
        assertSoftly(softly -> {
            softly.assertThat(s)
                .isNotEqualTo(null);
            softly.assertThat(s)
                .isNotEqualTo(UUID.randomUUID()
                    .toString());
            softly.assertThat(s)
                .isEqualTo(s);
            softly.assertThat(s)
                .isNotEqualTo(s2);
            softly.assertThat(s2)
                .isNotEqualTo(s);
            softly.assertThat(s)
                .isEqualTo(s3);
            softly.assertThat(s3)
                .isEqualTo(s);
        });
    }
}

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

package com.github.robozonky.common.state;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.RemoteBalance;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StateCleanerTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    @AfterEach
    void destroyAllState() {
        TenantState.destroyAll();
    }

    @Test
    void cleansOverThreshold() {
        TenantState.of(SESSION_INFO).in(StateCleaner.class).update(m -> m.put("a", "b").put("b", "c"));
        TenantState.of(SESSION_INFO).in(SessionInfo.class).update(m -> m.put("c", "d"));
        final StateCleaner stateCleaner = new StateCleaner(OffsetDateTime.now().plusDays(1)); // delete everything
        stateCleaner.accept(new MyTenant());
        assertThat(TenantState.of(SESSION_INFO).in(StateCleaner.class).getLastUpdated()).isEmpty();
        assertThat(TenantState.of(SESSION_INFO).in(SessionInfo.class).getLastUpdated()).isEmpty();
    }

    private static final class MyTenant implements Tenant {

        @Override
        public <T> T call(final Function<Zonky, T> operation, final ZonkyScope scope) {
            return null;
        }

        @Override
        public boolean isAvailable(final ZonkyScope scope) {
            return false;
        }

        @Override
        public RemoteBalance getBalance() {
            return null;
        }

        @Override
        public Restrictions getRestrictions() {
            return null;
        }

        @Override
        public SessionInfo getSessionInfo() {
            return SESSION_INFO;
        }

        @Override
        public SecretProvider getSecrets() {
            return null;
        }

        @Override
        public Optional<InvestmentStrategy> getInvestmentStrategy() {
            return Optional.empty();
        }

        @Override
        public Optional<SellStrategy> getSellStrategy() {
            return Optional.empty();
        }

        @Override
        public Optional<PurchaseStrategy> getPurchaseStrategy() {
            return Optional.empty();
        }

        @Override
        public void close() {

        }
    }
}

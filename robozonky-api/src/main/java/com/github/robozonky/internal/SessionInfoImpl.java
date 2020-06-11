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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Consents;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.remote.entities.ConsentsImpl;
import com.github.robozonky.internal.remote.entities.RestrictionsImpl;
import com.github.robozonky.internal.test.DateUtil;

public class SessionInfoImpl implements SessionInfo {

    private final String userName, name;
    private final boolean isDryRun;
    private final Reloadable<Consents> consents;
    private final Reloadable<Restrictions> restrictions;

    public SessionInfoImpl(final String userName) {
        this(userName, "Test", true);
    }

    public SessionInfoImpl(final String userName, final String name, final boolean isDryRun) {
        this(ConsentsImpl::new, () -> new RestrictionsImpl(true), userName, name, isDryRun);
    }

    public SessionInfoImpl(final Supplier<Consents> consents, final Supplier<Restrictions> restrictions,
            final String userName, final String name, final boolean isDryRun) {
        this.name = name;
        this.userName = userName;
        this.isDryRun = isDryRun;
        this.consents = Reloadable.with(consents)
            .reloadAfter(Duration.ofHours(1))
            .build();
        this.restrictions = Reloadable.with(restrictions)
            .reloadAfter(Duration.ofHours(1))
            .build();
    }

    @Override
    public boolean isDryRun() {
        return isDryRun;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getName() {
        return Optional.ofNullable(name)
            .orElse("");
    }

    private Consents getConsents() {
        return consents.get()
            .getOrElseThrow(ex -> new IllegalStateException("Failed retrieving consents.", ex));
    }

    private Restrictions getRestrictions() {
        return restrictions.get()
            .getOrElseThrow(ex -> new IllegalStateException("Failed retrieving restrictions.", ex));
    }

    @Override
    public boolean canInvest() {
        return !getRestrictions().isCannotInvest();
    }

    @Override
    public boolean canAccessSmp() {
        var originalCanAccessSmp = !getRestrictions().isCannotAccessSmp();
        return originalCanAccessSmp && getConsents().getSmpConsent()
            .map(s -> s.getAgreedOn()
                .isBefore(DateUtil.offsetNow()))
            .orElse(false);
    }

    @Override
    public Money getMinimumInvestmentAmount() {
        return getRestrictions().getMinimumInvestmentAmount();
    }

    @Override
    public Money getInvestmentStep() {
        return getRestrictions().getInvestmentStep();
    }

    public Money getMaximumInvestmentAmount() {
        return getRestrictions().getMaximumInvestmentAmount();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionInfoImpl.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("userName='" + userName + "'")
            .add("isDryRun=" + isDryRun)
            .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final SessionInfoImpl that = (SessionInfoImpl) o;
        return Objects.equals(userName, that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName);
    }
}

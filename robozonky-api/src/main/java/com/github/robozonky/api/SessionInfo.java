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

package com.github.robozonky.api;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Consents;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.test.DateUtil;

/**
 * Uniquely identifies the Zonky user that the application is working on behalf of, and carries some Zonky-imposed
 * restrictions that need to be enforced on the session.
 */
public final class SessionInfo {

    private final String userName, name;
    private final boolean isDryRun;
    private final Reloadable<Consents> consents;
    private final Reloadable<Restrictions> restrictions;

    public SessionInfo(final String userName) {
        this(userName, null);
    }

    public SessionInfo(final String userName, final String name) {
        this(userName, name, true);
    }

    public SessionInfo(final String userName, final String name, final boolean isDryRun) {
        this(Consents::new, () -> new Restrictions(true), userName, name, isDryRun);
    }

    public SessionInfo(final Supplier<Consents> consents, final Supplier<Restrictions> restrictions,
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

    /**
     * Whether or not the robot is doing a dry run. Dry run means that no portfolio-altering operations will be
     * performed, even though the robot would still continue doing everything else.
     * 
     * @return True if the robot is doing a dry run.
     */
    public boolean isDryRun() {
        return isDryRun;
    }

    /**
     * @return Zonky username of the current user.
     */
    public String getUsername() {
        return userName;
    }

    /**
     * @return Name of the robot session currently running.
     */
    public String getName() {
        return Optional.ofNullable(name)
            .map(n -> "RoboZonky '" + n + '\'')
            .orElse("RoboZonky");
    }

    private Consents getConsents() {
        return consents.get()
            .getOrElseThrow(ex -> new IllegalStateException("Failed retrieving consents.", ex));
    }

    private Restrictions getRestrictions() {
        return restrictions.get()
            .getOrElseThrow(ex -> new IllegalStateException("Failed retrieving restrictions.", ex));
    }

    public boolean canInvest() {
        return !getRestrictions().isCannotInvest();
    }

    public boolean canAccessSmp() {
        var originalCanAccessSmp = !getRestrictions().isCannotAccessSmp();
        return originalCanAccessSmp && getConsents().getSmpConsent()
            .map(s -> s.getAgreedOn()
                .isBefore(DateUtil.offsetNow()))
            .orElse(false);
    }

    public Money getMinimumInvestmentAmount() {
        return getRestrictions().getMinimumInvestmentAmount();
    }

    public Money getInvestmentStep() {
        return getRestrictions().getInvestmentStep();
    }

    public Money getMaximumInvestmentAmount() {
        return getRestrictions().getMaximumInvestmentAmount();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SessionInfo.class.getSimpleName() + "[", "]")
            .add("name='" + name + "'")
            .add("userName='" + userName + "'")
            .add("isDryRun=" + isDryRun)
            .toString();
    }

    /**
     * Within the context of a single app run, sessions with the same username represent the same session.
     * 
     * @param o
     * @return
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final SessionInfo that = (SessionInfo) o;
        return Objects.equals(userName, that.userName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName);
    }
}

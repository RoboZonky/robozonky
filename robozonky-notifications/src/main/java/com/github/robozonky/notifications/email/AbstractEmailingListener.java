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

package com.github.robozonky.notifications.email;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.Financial;
import com.github.robozonky.api.notifications.InvestmentBased;
import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractEmailingListener<T extends Event> implements EventListener<T> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Counter emailsOfThisType;
    private final ListenerSpecificNotificationProperties properties;
    private final Collection<BiConsumer<T, SessionInfo>> finishers = new FastList<>(1);

    protected AbstractEmailingListener(final ListenerSpecificNotificationProperties properties) {
        this.properties = properties;
        this.emailsOfThisType = new Counter(this.getClass().getSimpleName(),
                                            properties.getListenerSpecificHourlyEmailLimit());
        this.registerFinisher((event, sessionInfo) -> {
            // increase spam-prevention counters
            emailsOfThisType.increase(sessionInfo);
            this.properties.getGlobalCounter().increase(sessionInfo);
            if (event instanceof Financial) { // register balance
                final int balance = ((Financial) event).getPortfolioOverview().getCzkAvailable();
                BalanceTracker.INSTANCE.setLastKnownBalance(sessionInfo, balance);
            }
        });
    }

    private static Email createNewEmail(final NotificationProperties properties,
                                        final SessionInfo session) throws EmailException {
        final Email email = new SimpleEmail();
        email.setCharset(Defaults.CHARSET.displayName());
        email.setHostName(properties.getSmtpHostname());
        email.setSmtpPort(properties.getSmtpPort());
        email.setStartTLSRequired(properties.isStartTlsRequired());
        email.setSSLOnConnect(properties.isSslOnConnectRequired());
        email.setAuthentication(properties.getSmtpUsername(), properties.getSmtpPassword());
        final String sessionName = session.getName().map(n -> "RoboZonky '" + n + "'").orElse("RoboZonky");
        email.setFrom(properties.getSender(), sessionName);
        email.addTo(properties.getRecipient());
        return email;
    }

    protected final void registerFinisher(final BiConsumer<T, SessionInfo> finisher) {
        if (!finishers.contains(finisher)) {
            this.finishers.add(finisher);
        }
    }

    int countFinishers() {
        return this.finishers.size();
    }

    private boolean allowGlobal(final SessionInfo sessionInfo) {
        return properties.overrideGlobalGag() || properties.getGlobalCounter().allow(sessionInfo);
    }

    boolean shouldSendEmail(final T event, final SessionInfo sessionInfo) {
        return allowGlobal(sessionInfo) && this.emailsOfThisType.allow(sessionInfo);
    }

    abstract String getSubject(final T event);

    abstract String getTemplateFileName();

    private Map<String, Object> getBaseData(final T event) {
        if (event instanceof LoanBased) {
            if (event instanceof InvestmentBased) {
                final InvestmentBased e = (InvestmentBased) event;
                return Util.getLoanData(e.getInvestment(), e.getLoan());
            } else {
                final LoanBased e = (LoanBased) event;
                return Util.getLoanData(e.getLoan());
            }
        }
        return Collections.emptyMap();
    }

    protected Map<String, Object> getData(final T event) {
        final Map<String, Object> result = new UnifiedMap<>(getBaseData(event));
        if (event instanceof Financial) {
            final PortfolioOverview portfolioOverview = ((Financial) event).getPortfolioOverview();
            result.put("portfolio", Util.summarizePortfolioStructure(portfolioOverview));
        }
        return result;
    }

    final Map<String, Object> getData(final T event, final SessionInfo sessionInfo) {
        return Collections.unmodifiableMap(new UnifiedMap<String, Object>(this.getData(event)) {{
            // ratings here need to have a stable iteration order, as it will be used to list them in notifications
            put("ratings", Stream.of(Rating.values()).collect(Collectors.toList()));
            put("session", new UnifiedMap<String, Object>() {{
                put("userName", Util.obfuscateEmailAddress(sessionInfo.getUsername()));
                put("userAgent", Defaults.ROBOZONKY_USER_AGENT);
                put("isDryRun", sessionInfo.isDryRun());
            }});
        }});
    }

    @Override
    public void handle(final T event, final SessionInfo sessionInfo) {
        if (!this.shouldSendEmail(event, sessionInfo)) {
            LOGGER.debug("Will not send e-mail.");
        } else {
            try {
                final Email email = AbstractEmailingListener.createNewEmail(properties, sessionInfo);
                email.setSubject(this.getSubject(event));
                email.setMsg(TemplateProcessor.INSTANCE.process(this.getTemplateFileName(),
                                                                this.getData(event, sessionInfo)));
                LOGGER.debug("Will send '{}' from {} to {} through {}:{} as {}.", email.getSubject(),
                             email.getFromAddress(), email.getToAddresses(), email.getHostName(), email.getSmtpPort(),
                             properties.getSmtpUsername());
                email.send();
                // perform finishers after the e-mail has been sent
                finishers.forEach(f -> {
                    try {
                        f.accept(event, sessionInfo);
                    } catch (final Exception ex) {
                        LOGGER.trace("Finisher failed.", ex);
                    }
                });
            } catch (final Exception ex) {
                throw new RuntimeException("Failed processing event.", ex);
            }
        }
    }
}

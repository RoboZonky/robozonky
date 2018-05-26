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

package com.github.robozonky.notifications.listeners;

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
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import com.github.robozonky.notifications.util.BalanceTracker;
import com.github.robozonky.notifications.util.TemplateUtil;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractListener<T extends Event> implements EventListener<T> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
    private final Collection<BiConsumer<T, SessionInfo>> finishers = new FastList<>(1);
    private final AbstractTargetHandler handler;
    private final SupportedListener listener;

    protected AbstractListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        this.listener = listener;
        this.handler = handler;
        this.registerFinisher((event, sessionInfo) -> {
            if (event instanceof Financial) { // register balance
                final int balance = ((Financial) event).getPortfolioOverview().getCzkAvailable();
                BalanceTracker.INSTANCE.setLastKnownBalance(sessionInfo, balance);
            }
        });
    }

    protected final void registerFinisher(final BiConsumer<T, SessionInfo> finisher) {
        if (!finishers.contains(finisher)) {
            this.finishers.add(finisher);
        }
    }

    boolean shouldNotify(final T event, final SessionInfo sessionInfo) {
        return true;
    }

    abstract String getSubject(final T event);

    abstract String getTemplateFileName();

    private Map<String, Object> getBaseData(final T event) {
        if (event instanceof LoanBased) {
            if (event instanceof InvestmentBased) {
                final InvestmentBased e = (InvestmentBased) event;
                return TemplateUtil.getLoanData(e.getInvestment(), e.getLoan());
            } else {
                final LoanBased e = (LoanBased) event;
                return TemplateUtil.getLoanData(e.getLoan());
            }
        }
        return Collections.emptyMap();
    }

    protected Map<String, Object> getData(final T event) {
        final Map<String, Object> result = new UnifiedMap<>(getBaseData(event));
        if (event instanceof Financial) {
            final PortfolioOverview portfolioOverview = ((Financial) event).getPortfolioOverview();
            result.put("portfolio", TemplateUtil.summarizePortfolioStructure(portfolioOverview));
        }
        return result;
    }

    final Map<String, Object> getData(final T event, final SessionInfo sessionInfo) {
        return Collections.unmodifiableMap(new UnifiedMap<String, Object>(this.getData(event)) {{
            // ratings here need to have a stable iteration order, as it will be used to list them in notifications
            put("ratings", Stream.of(Rating.values()).collect(Collectors.toList()));
            put("session", new UnifiedMap<String, Object>() {{
                put("userName", TemplateUtil.obfuscateEmailAddress(sessionInfo.getUsername()));
                put("userAgent", Defaults.ROBOZONKY_USER_AGENT);
                put("isDryRun", sessionInfo.isDryRun());
            }});
        }});
    }

    @Override
    public void handle(final T event, final SessionInfo sessionInfo) {
        if (!this.shouldNotify(event, sessionInfo)) {
            LOGGER.debug("Will not notify.");
        } else {
            try {
                final String message = TemplateProcessor.INSTANCE.process(this.getTemplateFileName(),
                                                                          this.getData(event, sessionInfo));
                handler.send(listener, sessionInfo, getSubject(event), message);
            } catch (final Exception ex) {
                throw new RuntimeException("Failed processing event.", ex);
            }
        }
    }
}

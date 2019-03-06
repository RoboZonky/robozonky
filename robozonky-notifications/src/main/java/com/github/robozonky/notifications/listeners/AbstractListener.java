/*
 * Copyright 2019 The RoboZonky Project
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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.DelinquencyBased;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.Financial;
import com.github.robozonky.api.notifications.InvestmentBased;
import com.github.robozonky.api.notifications.LoanBased;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.notifications.MarketplaceInvestmentBased;
import com.github.robozonky.api.notifications.MarketplaceLoanBased;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.Maps;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.Submission;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.internal.util.Maps.entry;

abstract class AbstractListener<T extends Event> implements EventListener<T> {

    protected final Logger logger = LogManager.getLogger(this.getClass());
    final BalanceTracker balanceTracker;
    final DelinquencyTracker delinquencyTracker;
    private final AbstractTargetHandler handler;
    private final SupportedListener listener;

    protected AbstractListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        this.listener = listener;
        this.handler = handler;
        this.balanceTracker = new BalanceTracker(handler.getTarget());
        this.delinquencyTracker = new DelinquencyTracker(handler.getTarget());
    }

    /**
     * Override to run custom code after {@link #handle(Event, SessionInfo)} has finished processing. Always call
     * {@link AbstractListener#finish(Event, SessionInfo)} in your override.
     * @param event
     * @param sessionInfo
     */
    protected void finish(final T event, final SessionInfo sessionInfo) {
        if (event instanceof Financial) { // register balance
            final BigDecimal balance = ((Financial) event).getPortfolioOverview().getCzkAvailable();
            balanceTracker.setLastKnownBalance(sessionInfo, balance);
        }
        if (event instanceof DelinquencyBased) {
            delinquencyTracker.setDelinquent(sessionInfo, ((DelinquencyBased) event).getInvestment());
        } else if (event instanceof LoanLostEvent || event instanceof LoanRepaidEvent ||
                event instanceof LoanNoLongerDelinquentEvent) {
            delinquencyTracker.unsetDelinquent(sessionInfo, ((InvestmentBased) event).getInvestment());
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
                return Util.getLoanData(e.getInvestment(), e.getLoan());
            } else {
                final LoanBased e = (LoanBased) event;
                return Util.getLoanData(e.getLoan());
            }
        } else if (event instanceof MarketplaceLoanBased) {
            if (event instanceof MarketplaceInvestmentBased) {
                final MarketplaceInvestmentBased e = (MarketplaceInvestmentBased) event;
                return Util.getLoanData(e.getInvestment(), e.getLoan());
            } else {
                final MarketplaceLoanBased e = (MarketplaceLoanBased) event;
                return Util.getLoanData(e.getLoan());
            }
        }
        return Collections.emptyMap();
    }

    protected Map<String, Object> getData(final T event) {
        final Map<String, Object> result = new HashMap<>(getBaseData(event));
        if (event instanceof Financial) {
            final PortfolioOverview portfolioOverview = ((Financial) event).getPortfolioOverview();
            result.put("portfolio", Util.summarizePortfolioStructure(portfolioOverview));
        }
        return result;
    }

    final Map<String, Object> getData(final T event, final SessionInfo sessionInfo) {
        final Map<String, Object> result = new HashMap<>(this.getData(event));
        // ratings here need to have a stable iteration order, as they will be used to list them in notifications
        result.put("ratings", Stream.of(Rating.values()).collect(Collectors.toList()));
        result.put("session", Maps.ofEntries(
                entry("userName", Util.obfuscateEmailAddress(sessionInfo.getUsername())),
                entry("userAgent", Defaults.ROBOZONKY_USER_AGENT),
                entry("isDryRun", sessionInfo.isDryRun())
        ));
        result.put("conception", Date.from(event.getConceivedOn().toInstant()));
        result.put("creation", Date.from(event.getCreatedOn().toInstant()));
        return Collections.unmodifiableMap(result);
    }

    private Submission createSubmission(final T event, final SessionInfo sessionInfo) {
        final String s = this.getSubject(event);
        final String t = this.getTemplateFileName();
        return new Submission() {

            @Override
            public SessionInfo getSessionInfo() {
                return sessionInfo;
            }

            @Override
            public SupportedListener getSupportedListener() {
                return listener;
            }

            @Override
            public Map<String, Object> getData() {
                final Map<String, Object> data = new HashMap<>(AbstractListener.this.getData(event, sessionInfo));
                data.put("subject", getSubject());
                return Collections.unmodifiableMap(data);
            }

            @Override
            public String getSubject() {
                return s;
            }

            @Override
            public String getMessage(final Map<String, Object> data) throws IOException, TemplateException {
                return TemplateProcessor.INSTANCE.processHtml(t, data);
            }

            @Override
            public String getFallbackMessage(final Map<String, Object> data) throws IOException,
                    TemplateException {
                return TemplateProcessor.INSTANCE.processPlainText(t, data);
            }
        };
    }

    @Override
    public final void handle(final T event, final SessionInfo sessionInfo) {
        final jdk.jfr.Event jfr = new EmailJfrEvent();
        jfr.begin();
        try {
            if (!this.shouldNotify(event, sessionInfo)) {
                logger.debug("Will not notify.");
            } else {
                // only do the heavy lifting in the handler, after the final send/no-send decision was made
                logger.debug("Notifying {}.", event);
                handler.offer(createSubmission(event, sessionInfo));
            }
        } catch (final Exception ex) {
            throw new IllegalStateException("Event processing failed.", ex);
        } finally {
            try {
                finish(event, sessionInfo);
            } catch (final Exception ex) {
                logger.trace("Finisher failed.", ex);
            } finally {
                logger.debug("Notified {}.", event);
                jfr.commit();
            }
        }
    }
}

package com.github.robozonky.app.summaries;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.TenantPayload;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class Summarizer implements TenantPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    private void run(final PowerTenant tenant) {
        // assemble processors
        final CashFlowProcessor cashFlow = new CashFlowProcessor();
        final OutgoingInvestmentProcessor leavingInvestmentProcessor = new OutgoingInvestmentProcessor(tenant);
        final IncomingInvestmentProcessor newInvestmentProcessor = new IncomingInvestmentProcessor(tenant);
        final Collection<Consumer<Transaction>> processors =
                Arrays.asList(cashFlow, leavingInvestmentProcessor, newInvestmentProcessor);
        // prepare transactions and process them with all the processors
        final LocalDate oneWeekAgo = DateUtil.localNow().toLocalDate().minusWeeks(1);
        LOGGER.debug("Will process transactions with date of {} and closer.", oneWeekAgo);
        final Select sinceLastTime = new Select().greaterThanOrEquals("transaction.transactionDate", oneWeekAgo);
        tenant.call(z -> z.getTransactions(sinceLastTime))
                .parallel() // there may be tens of thousands of them
                .forEach(t -> processors.forEach(p -> p.accept(t)));
        // now prepare the summary and trigger the event
        final Summary summary = new SummaryBuilder()
                .addCashFlows(cashFlow)
                .addIncomingInvestments(newInvestmentProcessor)
                .addOutgoingInvestments(leavingInvestmentProcessor)
                .build(tenant.getPortfolio().getOverview());
    }

    @Override
    public void accept(final Tenant tenant) {
        ((PowerTenant) tenant).inTransaction(this::run);
    }
}

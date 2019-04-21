package com.github.robozonky.app.summaries;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.tenant.Tenant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractTransactionProcessor<T> implements Predicate<Transaction>,
                                                          Function<Transaction, T>,
                                                          Consumer<Transaction>,
                                                          Supplier<Stream<T>> {

    protected final Logger logger = LogManager.getLogger();
    private final Collection<T> values = new CopyOnWriteArraySet<>();

    // FIXME cache this
    protected static Investment lookupOrFail(final int loanId, final Tenant auth) {
        return auth.call(zonky -> zonky.getInvestmentByLoanId(loanId))
                .orElseThrow(() -> new IllegalStateException("Investment not found for loan #" + loanId));
    }

    @Override
    public Stream<T> get() {
        return values.stream();
    }

    @Override
    public void accept(final Transaction transaction) {
        if (!test(transaction)) {
            logger.trace("Skipping: {}.", transaction);
            return;
        }
        logger.debug("Processing: {}.", transaction);
        values.add(apply(transaction));
    }
}

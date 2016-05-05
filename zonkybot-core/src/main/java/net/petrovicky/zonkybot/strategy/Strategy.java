package net.petrovicky.zonkybot.strategy;

import net.petrovicky.zonkybot.api.remote.Loan;

public interface Strategy {

    boolean isAcceptable(Loan loan);

}

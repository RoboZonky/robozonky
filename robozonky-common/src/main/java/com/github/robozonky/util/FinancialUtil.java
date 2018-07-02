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

package com.github.robozonky.util;

import java.math.BigDecimal;

import static com.github.robozonky.internal.util.BigDecimalCalculator.divide;
import static com.github.robozonky.internal.util.BigDecimalCalculator.minus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.plus;
import static com.github.robozonky.internal.util.BigDecimalCalculator.times;

/**
 * Cleaned up and adapted from here:
 * https://www.experts-exchange.com/articles/1948/A-Guide-to-the-PMT-FV-IPMT-and-PPMT-Functions.html
 */
class FinancialUtil {

    private FinancialUtil() {
        // no instances
    }

    /**
     * Emulates Excel/Calc's PMT(interest_rate, number_payments, PV, FV, Type)
     * function, which calculates the mortgage or annuity payment / yield per
     * period.
     * @param r - periodic interest rate represented as a decimal.
     * @param nper - number of total payments / periods.
     * @param pv - present value -- borrowed or invested principal.
     * @param fv - future value of loan or annuity.
     * @param type - when payment is made: beginning of period is 1; end, 0.
     * @return Representing periodic payment amount.
     */
    public static BigDecimal pmt(final BigDecimal r, final int nper, final BigDecimal pv, final BigDecimal fv,
                                 final int type) {
        // pmt = r / ((1 + r)^N - 1) * -(pv * (1 + r)^N + fv)
        final BigDecimal tmp = plus(1, r).pow(nper);
        final BigDecimal pmt = times(
                divide(r,
                       minus(tmp, 1)),
                plus(times(pv, tmp),
                     fv).negate()
        );
        // account for payments at beginning of period versus end.
        if (type == 1) {
            return divide(pmt, plus(1, r));
        } else {
            return pmt;
        }
    }

    /**
     * Overloaded pmt() call omitting type, which defaults to 0.
     * @see #pmt(BigDecimal, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal pmt(final BigDecimal r, final int nper, final BigDecimal pv, final BigDecimal fv) {
        return pmt(r, nper, pv, fv, 0);
    }

    /**
     * Overloaded pmt() call omitting fv and type, which both default to 0.
     * @see #pmt(BigDecimal, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal pmt(final BigDecimal r, final int nper, final BigDecimal pv) {
        return pmt(r, nper, pv, BigDecimal.ZERO);
    }

    /**
     * Emulates Excel/Calc's FV(interest_rate, number_payments, payment, PV,
     * Type) function, which calculates future value or principal at period N.
     * @param r - periodic interest rate represented as a decimal.
     * @param nper - number of total payments / periods.
     * @param c - periodic payment amount.
     * @param pv - present value -- borrowed or invested principal.
     * @param type - when payment is made: beginning of period is 1; end, 0.
     * @return Representing future principal value.
     */
    public static BigDecimal fv(final BigDecimal r, final int nper, final BigDecimal c, final BigDecimal pv,
                                final int type) {
        // account for payments at beginning of period versus end.
        // since we are going in reverse, we multiply by 1 plus interest rate.
        final BigDecimal c2 = (type == 1) ? times(c, plus(1, r)) : c;
        final BigDecimal tmp = plus(1, r).pow(nper);
        return plus(
                times(
                        divide(
                                minus(tmp, 1),
                                r),
                        c2),
                times(pv, tmp)
        ).negate();
    }

    /**
     * Overloaded fv() call omitting type, which defaults to 0.
     * @see #fv(BigDecimal, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal fv(final BigDecimal r, final int nper, final BigDecimal c, final BigDecimal pv) {
        return fv(r, nper, c, pv, 0);
    }

    /**
     * Emulates Excel/Calc's IPMT(interest_rate, period, number_payments, PV,
     * FV, Type) function, which calculates the portion of the payment at a
     * given period that is the interest on previous balance.
     * @param r - periodic interest rate represented as a decimal.
     * @param per - period (payment number) to check value at.
     * @param nper - number of total payments / periods.
     * @param pv - present value -- borrowed or invested principal.
     * @param fv - future value of loan or annuity.
     * @param type - when payment is made: beginning of period is 1; end, 0.
     * @return Representing interest portion of payment.
     * @see #pmt(BigDecimal, int, BigDecimal, BigDecimal, int)
     * @see #fv(BigDecimal, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal ipmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv,
                                  final BigDecimal fv, final int type) {
        // Prior period (i.e., per-1) balance times periodic interest rate.
        // i.e., ipmt = fv(r, per-1, c, pv, type) * r
        // where c = pmt(r, nper, pv, fv, type)
        final BigDecimal ipmt = times(fv(r, per - 1, pmt(r, nper, pv, fv, type), pv, type), r);
        // account for payments at beginning of period versus end.
        if (type == 1) {
            return divide(ipmt, plus(1, r));
        } else {
            return ipmt;
        }
    }

    /**
     * Overloaded ipmt() call omitting type, which defaults to 0.
     * @see #ipmt(BigDecimal, int, int, BigDecimal, BigDecimal)
     */
    public static BigDecimal ipmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv) {
        return ipmt(r, per, nper, pv, BigDecimal.ZERO);
    }

    /**
     * Overloaded ipmt() call omitting type, which defaults to 0.
     * @see #ipmt(BigDecimal, int, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal ipmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv,
                                  final BigDecimal fv) {
        return ipmt(r, per, nper, pv, fv, 0);
    }

    /**
     * Emulates Excel/Calc's PPMT(interest_rate, period, number_payments, PV,
     * FV, Type) function, which calculates the portion of the payment at a
     * given period that will apply to principal.
     * @param r - periodic interest rate represented as a decimal.
     * @param per - period (payment number) to check value at.
     * @param nper - number of total payments / periods.
     * @param pv - present value -- borrowed or invested principal.
     * @param fv - future value of loan or annuity.
     * @param type - when payment is made: beginning of period is 1; end, 0.
     * @return Representing principal portion of payment.
     * @see #pmt(BigDecimal, int, BigDecimal, BigDecimal, int)
     * @see #ipmt(BigDecimal, int, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal ppmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv,
                                  final BigDecimal fv, final int type) {
        // Calculated payment per period minus interest portion of that period.
        // i.e., ppmt = c - i
        // where c = pmt(r, nper, pv, fv, type)
        // and i = ipmt(r, per, nper, pv, fv, type)
        return minus(
                pmt(r, nper, pv, fv, type),
                ipmt(r, per, nper, pv, fv, type)
        );
    }

    /**
     * Overloaded ppmt() call omitting type, which defaults to 0.
     * @see #ppmt(BigDecimal, int, int, BigDecimal, BigDecimal, int)
     */
    public static BigDecimal ppmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv,
                                  final BigDecimal fv) {
        return ppmt(r, per, nper, pv, fv, 0);
    }

    /**
     * Overloaded ppmt() call omitting type, which defaults to 0.
     * @see #ppmt(BigDecimal, int, int, BigDecimal, BigDecimal)
     */
    public static BigDecimal ppmt(final BigDecimal r, final int per, final int nper, final BigDecimal pv) {
        return ppmt(r, per, nper, pv, BigDecimal.ZERO);
    }
}

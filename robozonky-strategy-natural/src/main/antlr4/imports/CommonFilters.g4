grammar CommonFilters;

import Tokens;

@header {
    import java.util.Collection;
    import java.util.ArrayList;
    import java.util.Map;
    import java.util.HashMap;
    import com.github.robozonky.strategy.natural.conditions.*;
}

relativeProfitCondition returns [MarketplaceFilterCondition result]:
    'dosažený výnos ' (
        (c1 = relativeProfitConditionRangeOpen { $result = $c1.result; })
        | (c2 = relativeProfitConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = relativeProfitConditionRangeClosedRight { $result = $c3.result; })
    ) ' % původní jistiny'
;

relativeProfitConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = RelativeProfitCondition.exact(Ratio.fromPercentage($min.result), Ratio.fromPercentage($max.result)); }
;

relativeProfitConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = RelativeProfitCondition.moreThan(Ratio.fromPercentage($min.result)); }
;

relativeProfitConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = RelativeProfitCondition.lessThan(Ratio.fromPercentage($max.result)); }
;

relativeSaleDiscountCondition returns [MarketplaceFilterCondition result]:
    (
        'sleva ' (
            (c1 = relativeSaleDiscountConditionRangeOpen { $result = $c1.result; })
            | (c2 = relativeSaleDiscountConditionRangeClosedLeft { $result = $c2.result; })
            | (c3 = relativeSaleDiscountConditionRangeClosedRight { $result = $c3.result; })
        ) ' % zbývající jistiny'
    ) | (
        'se slevou' { $result = RelativeDiscountCondition.moreThan(Ratio.fromPercentage(0)); }
    ) | (
        'bez slevy' { $result = RelativeDiscountCondition.exact(Ratio.fromPercentage(0), Ratio.fromPercentage(0)); }
    )
;

relativeSaleDiscountConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = RelativeDiscountCondition.exact(Ratio.fromPercentage($min.result), Ratio.fromPercentage($max.result)); }
;

relativeSaleDiscountConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = RelativeDiscountCondition.moreThan(Ratio.fromPercentage($min.result)); }
;

relativeSaleDiscountConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = RelativeDiscountCondition.lessThan(Ratio.fromPercentage($max.result)); }
;

healthCondition returns [MarketplaceFilterCondition result]:
    { HealthCondition c = new HealthCondition(); }
    (
        (
            r1=healthExpression OR_COMMA { c.add($r1.result); }
        )*
        r2=healthExpression OR { c.add($r2.result); }
    )?
    r3=healthExpression { c.add($r3.result); }
    ' po splatnosti'
    { $result = c; }
;

saleFeeCondition returns [MarketplaceFilterCondition result]:
    'prodej '
    (
        ( IS { $result = SmpFeePresenceCondition.PRESENT; } )  |
        ( 'není ' { $result = SmpFeePresenceCondition.NOT_PRESENT; } )
    )
    'zpoplatněn'
;

regionCondition returns [MarketplaceFilterCondition result]:
    { BorrowerRegionCondition c = new BorrowerRegionCondition(); }
    'kraj klienta ' IS (
        (
            r1=regionExpression OR_COMMA { c.add($r1.result); }
        )*
        r2=regionExpression OR { c.add($r2.result); }
    )?
    r3=regionExpression { c.add($r3.result); }
    { $result = c; }
;

incomeCondition returns [MarketplaceFilterCondition result]:
    { final BorrowerIncomeCondition c = new BorrowerIncomeCondition(); }
    'klient ' IS (
        (
            i1=incomeExpression OR_COMMA { c.add($i1.result); }
        )*
        i2=incomeExpression OR { c.add($i2.result); }
    )?
    i3=incomeExpression { c.add($i3.result); }
    { $result = c; }
;

purposeCondition returns [MarketplaceFilterCondition result]:
    { final LoanPurposeCondition c = new LoanPurposeCondition(); }
    'účel ' IS (
        (
            p1=purposeExpression OR_COMMA { c.add($p1.result); }
        )*
        p2=purposeExpression OR { c.add($p2.result); }
    )?
    p3=purposeExpression { c.add($p3.result); }
    { $result = c; }
;

storyCondition returns [MarketplaceFilterCondition result]:
    'příběh ' IS (
        'velmi krátký' { $result = new VeryShortStoryCondition(); }
        | 'kratší než průměrný' { $result = new ShortStoryCondition(); }
        | 'průměrně dlouhý' { $result = new AverageStoryCondition(); }
        | 'delší než průměrný' { $result = new LongStoryCondition(); }
    )
;

insuranceCondition returns [MarketplaceFilterCondition result]:
    'pojištění ' (
        ( IS { $result = InsuranceCondition.ACTIVE; })
        | ('není ' { $result = InsuranceCondition.INACTIVE; })
    ) 'aktivní'
;

termCondition returns [MarketplaceFilterCondition result]:
    'délka ' (
        (c1 = termConditionRangeOpen { $result = $c1.result; })
        | (c2 = termConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = termConditionRangeClosedRight { $result = $c3.result; })
    ) ' měsíců'
;

termConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = LoanTermCondition.exact($min.result, $max.result); }
;

termConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = LoanTermCondition.moreThan($min.result); }
;

termConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = LoanTermCondition.lessThan($max.result); }
;

relativeTermCondition returns [MarketplaceFilterCondition result]:
    'délka ' (
        (c1 = relativeTermConditionRangeOpen { $result = $c1.result; })
        | (c2 = relativeTermConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = relativeTermConditionRangeClosedRight { $result = $c3.result; })
    ) ' % původní délky'
;

relativeTermConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = RelativeLoanTermCondition.exact(Ratio.fromPercentage($min.result), Ratio.fromPercentage($max.result)); }
;

relativeTermConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = RelativeLoanTermCondition.moreThan(Ratio.fromPercentage($min.result)); }
;

relativeTermConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = RelativeLoanTermCondition.lessThan(Ratio.fromPercentage($max.result)); }
;

elapsedTermCondition returns [MarketplaceFilterCondition result]:
    'uhrazeno ' (
        (c1 = elapsedTermConditionRangeOpen { $result = $c1.result; })
        | (c2 = elapsedTermConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = elapsedTermConditionRangeClosedRight { $result = $c3.result; })
    ) ' splátek'
;

elapsedTermConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = ElapsedLoanTermCondition.exact($min.result, $max.result); }
;

elapsedTermConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    'více než' min=intExpr
    { $result = ElapsedLoanTermCondition.moreThan($min.result); }
;

elapsedTermConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    'méně než' max=intExpr
    { $result = ElapsedLoanTermCondition.lessThan($max.result); }
;

elapsedRelativeTermCondition returns [MarketplaceFilterCondition result]:
    'uhrazeno ' (
        (c1 = elapsedRelativeTermConditionRangeOpen { $result = $c1.result; })
        | (c2 = elapsedRelativeTermConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = elapsedRelativeTermConditionRangeClosedRight { $result = $c3.result; })
    ) ' % splátek'
;

elapsedRelativeTermConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = RelativeElapsedLoanTermCondition.exact(Ratio.fromPercentage($min.result), Ratio.fromPercentage($max.result)); }
;

elapsedRelativeTermConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    'více než' min=intExpr
    { $result = RelativeElapsedLoanTermCondition.moreThan(Ratio.fromPercentage($min.result)); }
;

elapsedRelativeTermConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    'méně než' max=intExpr
    { $result = RelativeElapsedLoanTermCondition.lessThan(Ratio.fromPercentage($max.result)); }
;

amountCondition returns [MarketplaceFilterCondition result]:
    'výše ' (
        (c1 = amountConditionRangeOpen { $result = $c1.result; })
        | (c2 = amountConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = amountConditionRangeClosedRight { $result = $c3.result; })
    ) KC
;

amountConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = LoanAmountCondition.exact($min.result, $max.result); }
;

amountConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = LoanAmountCondition.moreThan($min.result); }
;

amountConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = LoanAmountCondition.lessThan($max.result); }
;

remainingPrincipalCondition returns [MarketplaceFilterCondition result]:
    'zbývající jistina ' (
        (c1 = remainingPrincipalConditionRangeOpen { $result = $c1.result; })
        | (c2 = remainingPrincipalConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = remainingPrincipalConditionRangeClosedRight { $result = $c3.result; })
    ) KC
;

remainingPrincipalConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = RemainingPrincipalCondition.exact($min.result, $max.result); }
;

remainingPrincipalConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = RemainingPrincipalCondition.moreThan($min.result); }
;

remainingPrincipalConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = RemainingPrincipalCondition.lessThan($max.result); }
;

annuityCondition returns [MarketplaceFilterCondition result]:
    'měsíční splátka ' (
        (c1 = annuityConditionRangeOpen { $result = $c1.result; })
        | (c2 = annuityConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = annuityConditionRangeClosedRight { $result = $c3.result; })
    ) KC
;

annuityConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=intExpr UP_TO max=intExpr
    { $result = LoanAnnuityCondition.exact($min.result, $max.result); }
;

annuityConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=intExpr
    { $result = LoanAnnuityCondition.moreThan($min.result); }
;

annuityConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=intExpr
    { $result = LoanAnnuityCondition.lessThan($max.result); }
;

revenueRateCondition returns [MarketplaceFilterCondition result]:
    'optimální výnos ' (
        (c1 = revenueRateConditionRangeOpen { $result = $c1.result; })
        | (c2 = revenueRateConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = revenueRateConditionRangeClosedRight { $result = $c3.result; })
     ) ' % p.a' DOT?
;

revenueRateConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=floatExpr UP_TO max=floatExpr {
        $result = RevenueRateCondition.exact(Ratio.fromPercentage($min.result), Ratio.fromPercentage($max.result));
    }
;

revenueRateConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=floatExpr
    { $result = RevenueRateCondition.moreThan(Ratio.fromPercentage($min.result)); }
;

revenueRateConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=floatExpr
    { $result = RevenueRateCondition.lessThan(Ratio.fromPercentage($max.result)); }
;

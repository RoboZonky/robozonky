grammar CommonFilters;

import Tokens;

@header {
    import java.util.Collection;
    import java.util.ArrayList;
    import java.util.Map;
    import java.util.HashMap;
    import com.github.robozonky.strategy.natural.*;
}

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

incomeCondition returns [BorrowerIncomeCondition result]:
    { $result = new BorrowerIncomeCondition(); }
    'klient ' IS (
        (
            i1=incomeExpression OR_COMMA { $result.add($i1.result); }
        )*
        i2=incomeExpression OR { $result.add($i2.result); }
    )?
    i3=incomeExpression { $result.add($i3.result); }
;

purposeCondition returns [LoanPurposeCondition result]:
    { $result = new LoanPurposeCondition(); }
    'účel ' IS (
        (
            p1=purposeExpression OR_COMMA { $result.add($p1.result); }
        )*
        p2=purposeExpression OR { $result.add($p2.result); }
    )?
    p3=purposeExpression { $result.add($p3.result); }
;

storyCondition returns [AbstractStoryCondition result]:
    'příběh ' IS (
        'velmi krátký' { $result = new VeryShortStoryCondition(); }
        | 'kratší než průměrný' { $result = new ShortStoryCondition(); }
        | 'průměrně dlouhý' { $result = new AverageStoryCondition(); }
        | 'delší než průměrný' { $result = new LongStoryCondition(); }
    )
;

termCondition returns [LoanTermCondition result]:
    'délka ' (
        (c1 = termConditionRangeOpen { $result = $c1.result; })
        | (c2 = termConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = termConditionRangeClosedRight { $result = $c3.result; })
    ) ' měsíců'
;

termConditionRangeOpen returns [LoanTermCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

termConditionRangeClosedLeft returns [LoanTermCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()) + 1); }
;

termConditionRangeClosedRight returns [LoanTermCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanTermCondition(0, Integer.parseInt($max.getText()) - 1); }
;

interestCondition returns [LoanInterestRateCondition result]:
    'úrok ' (
        (c1 = interestConditionRangeOpen { $result = $c1.result; })
        | (c2 = interestConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = interestConditionRangeClosedRight { $result = $c3.result; })
    ) ' % p.a' DOT? // last dot is optional, so that it is possible to end sentence like "p.a." and not "p.a.."
;

interestConditionRangeOpen returns [LoanInterestRateCondition result]:
    IS min=floatExpression UP_TO max=floatExpression
    { $result = new LoanInterestRateCondition($min.result, $max.result); }
;

interestConditionRangeClosedLeft returns [LoanInterestRateCondition result]:
    MORE_THAN min=floatExpression
    { $result = new LoanInterestRateCondition(LoanInterestRateCondition.moreThan($min.result)); }
;

interestConditionRangeClosedRight returns [LoanInterestRateCondition result]:
    LESS_THAN max=floatExpression
    { $result = new LoanInterestRateCondition(BigDecimal.ZERO, LoanInterestRateCondition.lessThan($max.result)); }
;

amountCondition returns [LoanAmountCondition result]:
    'výše ' (
        (c1 = amountConditionRangeOpen { $result = $c1.result; })
        | (c2 = amountConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = amountConditionRangeClosedRight { $result = $c3.result; })
    ) ' ' KC
;

amountConditionRangeOpen returns [LoanAmountCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

amountConditionRangeClosedLeft returns [LoanAmountCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()) + 1); }
;

amountConditionRangeClosedRight returns [LoanAmountCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanAmountCondition(0, Integer.parseInt($max.getText()) - 1); }
;

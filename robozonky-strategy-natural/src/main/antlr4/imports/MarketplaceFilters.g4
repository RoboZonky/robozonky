grammar MarketplaceFilters;

import Tokens;

@header {
    import java.util.Collection;
    import com.github.triceo.robozonky.strategy.natural.*;
}

marketplaceFilterExpression returns [Collection<MarketplaceFilter> result]:
    { $result = new ArrayList<>(); }
    (f=marketplaceFilter { $result.add($f.result); })+
;

marketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Ignorovat úvěr, kde: ' r=marketplaceFilterConditions { $result.ignoreWhen($r.result); }
    ('(Ale ne když: ' s=marketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

marketplaceFilterConditions returns [Collection<MarketplaceFilterCondition> result]:
    { Collection<MarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=marketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=marketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

marketplaceFilterCondition returns [MarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c2=ratingCondition { $result = $c2.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c7=amountCondition { $result = $c7.result; }
    | c8=interestCondition { $result = $c8.result; }
;

regionCondition returns [BorrowerRegionCondition result]:
    { $result = new BorrowerRegionCondition(); }
    'kraj klienta ' IS (
        (
            r1=regionExpression OR_COMMA { $result.add($r1.result); }
        )*
        r2=regionExpression OR { $result.add($r2.result); }
    )?
    r3=regionExpression { $result.add($r3.result); }
;

ratingCondition returns [MarketplaceFilterCondition result]:
    'rating ' IS (
        ( r1=ratingEnumeratedExpression
            {
                $result = new LoanRatingEnumeratedCondition();
                $result.add($r1.result);
            })
        | ('lepší než ' r2=ratingExpression { $result = new LoanRatingBetterOrEqualCondition($r2.result); })
        | ('horší než ' r3=ratingExpression { $result = new LoanRatingWorseOrEqualCondition($r3.result); })
    )
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

storyCondition returns [MarketplaceFilterCondition result]:
    'příběh ' IS (
        'velmi krátký' { $result = new VeryShortStoryCondition(); }
        | 'kratší než průměrný' { $result = new ShortStoryCondition(); }
        | 'průměrně dlouhý' { $result = new AverageStoryCondition(); }
        | 'delší než průměrný' { $result = new LongStoryCondition(); }
    )
;

termCondition returns [MarketplaceFilterCondition result]:
    'délka ' (
        (c1 = termConditionRangeOpen { $result = $c1.result; })
        | (c2 = termConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = termConditionRangeClosedRight { $result = $c3.result; })
    ) ' měsíců'
;

termConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

termConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()) + 1); }
;

termConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanTermCondition(0, Integer.parseInt($max.getText()) - 1); }
;

interestCondition returns [MarketplaceFilterCondition result]:
    'úrok ' (
        (c1 = interestConditionRangeOpen { $result = $c1.result; })
        | (c2 = interestConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = interestConditionRangeClosedRight { $result = $c3.result; })
    ) ' % p.a' DOT? // last dot is optional, so that it is possible to end sentence like "p.a." and not "p.a.."
;

interestConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=floatExpression UP_TO max=floatExpression
    { $result = new LoanInterestRateCondition($min.result, $max.result); }
;

interestConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=floatExpression
    { $result = new LoanInterestRateCondition(LoanInterestRateCondition.moreThan($min.result)); }
;

interestConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=floatExpression
    { $result = new LoanInterestRateCondition(BigDecimal.ZERO, LoanInterestRateCondition.lessThan($max.result)); }
;

amountCondition returns [MarketplaceFilterCondition result]:
    'výše ' (
        (c1 = amountConditionRangeOpen { $result = $c1.result; })
        | (c2 = amountConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = amountConditionRangeClosedRight { $result = $c3.result; })
    ) ' ' KC
;

amountConditionRangeOpen returns [MarketplaceFilterCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

amountConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()) + 1); }
;

amountConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanAmountCondition(0, Integer.parseInt($max.getText()) - 1); }
;

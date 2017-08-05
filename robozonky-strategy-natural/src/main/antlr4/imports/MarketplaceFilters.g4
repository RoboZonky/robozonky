grammar MarketplaceFilters;

import Tokens;

@header {
    import java.util.Collection;
    import com.github.triceo.robozonky.strategy.natural.*;
}

marketplaceFilterExpression returns [Collection<MarketplaceFilter> result]:
    { $result = new ArrayList<>(); }
    (
        (j=jointMarketplaceFilter { $result.add($j.result); })
        |(p=primaryMarketplaceFilter { $result.add($p.result); })
        |(s=secondaryMarketplaceFilter { $result.add($s.result); })
    )+
;

jointMarketplaceFilter returns [JointMarketplaceFilter result]:
    { $result = new JointMarketplaceFilter(); }
    'Ignorovat úvěr i participaci, kde: ' r=jointMarketplaceFilterConditions { $result.ignoreWhen($r.result); }
    ('(Ale ne když: ' s=jointMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

primaryMarketplaceFilter returns [PrimaryMarketplaceFilter result]:
    { $result = new PrimaryMarketplaceFilter(); }
    'Ignorovat úvěr, kde: ' r=primaryMarketplaceFilterConditions { $result.ignoreWhen($r.result); }
    ('(Ale ne když: ' s=primaryMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

secondaryMarketplaceFilter returns [SecondaryMarketplaceFilter result]:
    { $result = new SecondaryMarketplaceFilter(); }
    'Ignorovat participaci, kde: ' r=secondaryMarketplaceFilterConditions { $result.ignoreWhen($r.result); }
    ('(Ale ne když: ' s=secondaryMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

jointMarketplaceFilterConditions returns [Collection<JointMarketplaceFilterCondition> result]:
    { Collection<JointMarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=primaryMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=primaryMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

primaryMarketplaceFilterConditions returns [Collection<PrimaryMarketplaceFilterCondition> result]:
    { Collection<PrimaryMarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=primaryMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=primaryMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

secondaryMarketplaceFilterConditions returns [Collection<SecondaryMarketplaceFilterCondition> result]:
    { Collection<SecondaryMarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=secondaryMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=secondaryMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

jointMarketplaceFilterCondition returns [JointMarketplaceFilterCondition result]:
    | c2=ratingCondition { $result = $c2.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c6=termCondition { $result = $c6.result; }
    | c8=interestCondition { $result = $c8.result; }
;

primaryMarketplaceFilterCondition returns [PrimaryMarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c2=ratingCondition { $result = $c2.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c7=amountCondition { $result = $c7.result; }
    | c8=interestCondition { $result = $c8.result; }
;

secondaryMarketplaceFilterCondition returns [SecondaryMarketplaceFilterCondition result]:
    c2=ratingCondition { $result = $c2.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c6=termCondition { $result = $c6.result; }
    | c8=interestCondition { $result = $c8.result; }
    | c9=remainingTermCondition { $result = $c9.result; }
    | c10=remainingAmountCondition { $result = $c10.result; }
;

regionCondition returns [PrimaryMarketplaceFilterCondition result]:
    { $result = new BorrowerRegionCondition(); }
    'kraj klienta ' IS (
        (
            r1=regionExpression OR_COMMA { $result.add($r1.result); }
        )*
        r2=regionExpression OR { $result.add($r2.result); }
    )?
    r3=regionExpression { $result.add($r3.result); }
;

ratingCondition returns [JointMarketplaceFilterCondition result]:
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

incomeCondition returns [JointBorrowerIncomeCondition result]:
    { $result = new BorrowerIncomeCondition(); }
    'klient ' IS (
        (
            i1=incomeExpression OR_COMMA { $result.add($i1.result); }
        )*
        i2=incomeExpression OR { $result.add($i2.result); }
    )?
    i3=incomeExpression { $result.add($i3.result); }
;

purposeCondition returns [JointLoanPurposeCondition result]:
    { $result = new LoanPurposeCondition(); }
    'účel ' IS (
        (
            p1=purposeExpression OR_COMMA { $result.add($p1.result); }
        )*
        p2=purposeExpression OR { $result.add($p2.result); }
    )?
    p3=purposeExpression { $result.add($p3.result); }
;

storyCondition returns [PrimaryMarketplaceFilterCondition result]:
    'příběh ' IS (
        'velmi krátký' { $result = new VeryShortStoryCondition(); }
        | 'kratší než průměrný' { $result = new ShortStoryCondition(); }
        | 'průměrně dlouhý' { $result = new AverageStoryCondition(); }
        | 'delší než průměrný' { $result = new LongStoryCondition(); }
    )
;

termCondition returns [JointMarketplaceFilterCondition result]:
    'délka ' (
        (c1 = termConditionRangeOpen { $result = $c1.result; })
        | (c2 = termConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = termConditionRangeClosedRight { $result = $c3.result; })
    ) ' měsíců'
;

termConditionRangeOpen returns [JointMarketplaceFilterCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

termConditionRangeClosedLeft returns [JointMarketplaceFilterCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanTermCondition(Integer.parseInt($min.getText()) + 1); }
;

termConditionRangeClosedRight returns [JointMarketplaceFilterCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanTermCondition(0, Integer.parseInt($max.getText()) - 1); }
;

remainingTermCondition returns [SecondaryMarketplaceFilterCondition result]:
    'zbývající délka ' (
        (c1 = termConditionRangeOpen { $result = $c1.result; })
        | (c2 = termConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = termConditionRangeClosedRight { $result = $c3.result; })
    ) ' měsíců'
;

interestCondition returns [JointMarketplaceFilterCondition result]:
    'úrok ' (
        (c1 = interestConditionRangeOpen { $result = $c1.result; })
        | (c2 = interestConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = interestConditionRangeClosedRight { $result = $c3.result; })
    ) ' % p.a' DOT? // last dot is optional, so that it is possible to end sentence like "p.a." and not "p.a.."
;

interestConditionRangeOpen returns [JointMarketplaceFilterCondition result]:
    IS min=floatExpression UP_TO max=floatExpression
    { $result = new LoanInterestRateCondition($min.result, $max.result); }
;

interestConditionRangeClosedLeft returns [JointMarketplaceFilterCondition result]:
    MORE_THAN min=floatExpression
    { $result = new LoanInterestRateCondition(LoanInterestRateCondition.moreThan($min.result)); }
;

interestConditionRangeClosedRight returns [JointMarketplaceFilterCondition result]:
    LESS_THAN max=floatExpression
    { $result = new LoanInterestRateCondition(BigDecimal.ZERO, LoanInterestRateCondition.lessThan($max.result)); }
;

amountCondition returns [PrimaryMarketplaceFilterCondition result]:
    'výše ' (
        (c1 = amountConditionRangeOpen { $result = $c1.result; })
        | (c2 = amountConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = amountConditionRangeClosedRight { $result = $c3.result; })
    ) ' ' KC
;

amountConditionRangeOpen returns [PrimaryMarketplaceFilterCondition result]:
    IS min=INTEGER UP_TO max=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()), Integer.parseInt($max.getText())); }
;

amountConditionRangeClosedLeft returns [PrimaryMarketplaceFilterCondition result]:
    MORE_THAN min=INTEGER
    { $result = new LoanAmountCondition(Integer.parseInt($min.getText()) + 1); }
;

amountConditionRangeClosedRight returns [PrimaryMarketplaceFilterCondition result]:
    LESS_THAN max=INTEGER
    { $result = new LoanAmountCondition(0, Integer.parseInt($max.getText()) - 1); }
;

remainingAmountCondition returns [SecondaryMarketplaceFilterCondition result]:
    'zbývající výše ' (
        (c1 = amountConditionRangeOpen { $result = $c1.result; })
        | (c2 = amountConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = amountConditionRangeClosedRight { $result = $c3.result; })
    ) ' ' KC
;

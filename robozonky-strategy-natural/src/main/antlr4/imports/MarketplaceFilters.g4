grammar MarketplaceFilters;

import CommonFilters, Tokens;

@header {
    import java.util.Collection;
    import java.util.ArrayList;
    import java.util.Map;
    import java.util.HashMap;
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;
}

marketplaceFilterExpression returns [Collection<MarketplaceFilter> primary, Collection<MarketplaceFilter> secondary,
                                        boolean primaryEnabled, boolean secondaryEnabled]
    @init {
        $primary = new ArrayList<>();
        $secondary = new ArrayList<>();
        $primaryEnabled = true;
        $secondaryEnabled = true;
    } :
    (
        n=noFiltersGivenExpression {
            $primaryEnabled = $n.primaryEnabled;
            $secondaryEnabled = $n.secondaryEnabled;
        }
    ) | (
        p=onlyPrimaryFilterExpression {
            $primary = $p.primary;
            $secondaryEnabled = $p.secondaryEnabled;
        }
    ) | (
        s=onlySecondaryFilterExpression {
            $primaryEnabled = $s.primaryEnabled;
            $secondary = $s.secondary;
        }
    ) | (
        o=oldMarketplaceFilterExpression {
            $primary = $o.primary;
            $secondary = $o.secondary;
        }
    )
;

oldMarketplaceFilterExpression returns [Collection<MarketplaceFilter> primary, Collection<MarketplaceFilter> secondary]
    @init {
        $primary = new ArrayList<>();
        $secondary = new ArrayList<>();
    } :
    (
        (j=jointMarketplaceFilter {
            $primary.add($j.result);
            $secondary.add($j.result);
        }) | (p=primaryMarketplaceFilter {
            $primary.add($p.result);
        }) | (s=secondaryMarketplaceFilter {
            $secondary.add($s.result);
        })
    )* {
        if ($primary.isEmpty()) {
            LogManager.getLogger(this.getClass())
                .warn("Primary marketplace filters missing without excuse. This is deprecated and will eventually break.");
        }
        if ($secondary.isEmpty()) {
            LogManager.getLogger(this.getClass())
                .warn("Secondary marketplace filters missing without excuse. This is deprecated and will eventually break.");
        }
   }
;

noFiltersGivenExpression returns [boolean primaryEnabled, boolean secondaryEnabled]:
    p=primaryEnablementExpression {
        $primaryEnabled = $p.enabled;
    }
    s=secondaryEnablementExpression {
        $secondaryEnabled = $s.enabled;
    }
;

primaryEnablementExpression returns [boolean enabled]:
    (
        'Ignorovat všechny půjčky.' { $enabled = false; }
    ) | (
        'Investovat do všech půjček.' { $enabled = true; }
    )
;

secondaryEnablementExpression returns [boolean enabled]:
    (
        'Ignorovat všechny participace.' { $enabled = false; }
    ) | (
        'Investovat do všech participací.' { $enabled = true; }
    )
;

onlySecondaryFilterExpression returns [Collection<MarketplaceFilter> secondary, boolean primaryEnabled]
    @init {
        $secondary = new ArrayList<>(0);
    } :
    p=primaryEnablementExpression {
        $primaryEnabled = $p.enabled;
    }
    (
        s=secondaryMarketplaceFilter {
            $secondary.add($s.result);
        }
    )+
;

onlyPrimaryFilterExpression returns [Collection<MarketplaceFilter> primary, boolean secondaryEnabled]
    @init {
        $primary = new ArrayList<>(0);
    } :
    (
        p=primaryMarketplaceFilter {
            $primary.add($p.result);
        }
    )+
    s=secondaryEnablementExpression {
        $secondaryEnabled = $s.enabled;
    }
;


sellFilterExpression returns [Collection<MarketplaceFilter> result]:
    { $result = new ArrayList<>(0); }
    (
        (j=sellMarketplaceFilter { $result.add($j.result); })
    )*
;

jointMarketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Ignorovat vše, kde: ' r=jointMarketplaceFilterConditions { $result.when($r.result); }
    ('(Ale ne když: ' s=jointMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

primaryMarketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Ignorovat úvěr, kde: ' r=primaryMarketplaceFilterConditions { $result.when($r.result); }
    ('(Ale ne když: ' s=primaryMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

secondaryMarketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Ignorovat participaci, kde: ' r=secondaryMarketplaceFilterConditions { $result.when($r.result); }
    ('(Ale ne když: ' s=secondaryMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

sellMarketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Prodat participaci, kde: ' r=secondaryMarketplaceFilterConditions { $result.when($r.result); }
    ('(Ale ne když: ' s=secondaryMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

jointMarketplaceFilterConditions returns [Collection<MarketplaceFilterCondition> result]:
    { Collection<MarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=jointMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=jointMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

primaryMarketplaceFilterConditions returns [Collection<MarketplaceFilterCondition> result]:
    { Collection<MarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=primaryMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=primaryMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

secondaryMarketplaceFilterConditions returns [Collection<MarketplaceFilterCondition> result]:
    { Collection<MarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=secondaryMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=secondaryMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

jointMarketplaceFilterCondition returns [MarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c7=amountCondition { $result = $c7.result; }
    | c8=interestCondition { $result = $c8.result; }
    | c12=insuranceCondition { $result = $c12.result; }
    | c14=annuityCondition { $result = $c14.result; }
    | c15=revenueRateCondition { $result = $c15.result; }
;

primaryMarketplaceFilterCondition returns [MarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c7=amountCondition { $result = $c7.result; }
    | c8=interestCondition { $result = $c8.result; }
    | c12=insuranceCondition { $result = $c12.result; }
    | c14=annuityCondition { $result = $c14.result; }
    | c15=revenueRateCondition { $result = $c15.result; }
;

secondaryMarketplaceFilterCondition returns [MarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c7=amountCondition { $result = $c7.result; }
    | c8=interestCondition { $result = $c8.result; }
    | c9=relativeTermCondition { $result = $c9.result; }
    | c10=elapsedTermCondition { $result = $c10.result; }
    | c11=elapsedRelativeTermCondition { $result = $c11.result; }
    | c12=insuranceCondition { $result = $c12.result; }
    | c13=remainingPrincipalCondition { $result = $c13.result; }
    | c14=annuityCondition { $result = $c14.result; }
    | c15=revenueRateCondition { $result = $c15.result; }
;

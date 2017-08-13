grammar SellRules;

import CommonFilters, Tokens;

@header {
    import java.util.Collection;
    import java.util.ArrayList;
    import java.util.Map;
    import java.util.HashMap;
    import com.github.triceo.robozonky.strategy.natural.*;
}

sellFilterExpression returns [Collection<MarketplaceFilter> result]:
    { $result = new ArrayList<>();
    }
    (
        (j=sellMarketplaceFilter { $result.add($j.result); })
    )+
;

sellMarketplaceFilter returns [MarketplaceFilter result]:
    { $result = new MarketplaceFilter(); }
    'Prodat participaci, kde: ' r=sellMarketplaceFilterConditions { $result.ignoreWhen($r.result); }
    ('(Ale ne když: ' s=sellMarketplaceFilterConditions { $result.butNotWhen($s.result); } ')')?
;

sellMarketplaceFilterConditions returns [Collection<MarketplaceFilterCondition> result]:
    { Collection<MarketplaceFilterCondition> result = new LinkedHashSet<>(); }
    (c1=sellMarketplaceFilterCondition { result.add($c1.result); } '; ')*
    c2=sellMarketplaceFilterCondition { result.add($c2.result); } DOT
    { $result = result; }
;

sellMarketplaceFilterCondition returns [MarketplaceFilterCondition result]:
    c1=regionCondition { $result = $c1.result; }
    | c2=ratingCondition { $result = $c2.result; }
    | c3=incomeCondition { $result = $c3.result; }
    | c4=purposeCondition { $result = $c4.result; }
    | c5=storyCondition { $result = $c5.result; }
    | c6=termCondition { $result = $c6.result; }
    | c8=interestCondition { $result = $c8.result; }
;

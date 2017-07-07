grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigInteger;
    import com.github.triceo.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [Collection<PortfolioStructureItem> result]:
 { Collection<PortfolioStructureItem> result = new LinkedHashSet<>(); }
 (i=portfolioStructureRatingExpression { result.add($i.result); })+
 { $result = result; }
;

portfolioStructureRatingExpression returns [PortfolioStructureItem result] :
    'Prostředky v ratingu ' r=ratingExpression ' tvoří' (
        (' až ' maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioStructureItem($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' ' minimumInvestmentInCzk=INTEGER ' až ' maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioStructureItem($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' % aktuální zůstatkové částky' DOT
;

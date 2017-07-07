grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigInteger;
    import com.github.triceo.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [Collection<PortfolioShare> result]:
 { Collection<PortfolioShare> result = new LinkedHashSet<>(); }
 (i=portfolioStructureRatingExpression { result.add($i.result); })+
 { $result = result; }
;

portfolioStructureRatingExpression returns [PortfolioShare result] :
    'Prostředky v ratingu ' r=ratingExpression ' tvoří' (
        (UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioShare($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' ' minimumInvestmentInCzk=INTEGER UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioShare($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' % aktuální zůstatkové částky' DOT
;

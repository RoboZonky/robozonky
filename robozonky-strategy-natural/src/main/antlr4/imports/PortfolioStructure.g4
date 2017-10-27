grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigInteger;
    import com.github.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [Collection<PortfolioShare> result]:
 { Collection<PortfolioShare> result = new LinkedHashSet<>(); }
 (i=portfolioStructureRatingExpression { result.add($i.result); })*
 { $result = result; }
;

portfolioStructureRatingExpression returns [PortfolioShare result] :
    'Prostředky v ratingu ' r=ratingExpression ' tvoří ' (
        ( maximumInvestmentInCzk=intExpr
            { $result = new PortfolioShare($r.result, $maximumInvestmentInCzk.result); })
        | ( minimumInvestmentInCzk=intExpr UP_TO maximumInvestmentInCzk=intExpr
            { $result = new PortfolioShare($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })
    ) ' % aktuální zůstatkové částky' DOT
;

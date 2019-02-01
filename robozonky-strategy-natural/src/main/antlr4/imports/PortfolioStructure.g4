grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigInteger;
    import com.github.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [Collection<PortfolioShare> result]:
 { Collection<PortfolioShare> result = new LinkedHashSet<>(); }
 (i=portfolioStructureInterestRateExpression { result.add($i.result); })+
 { $result = result; }
;

portfolioStructureInterestRateExpression returns [PortfolioShare result] :
    'Prostředky úročené ' r=interestRateBasedRatingExpression 'mají tvořit' (
        ( maximumInvestmentInCzk=intExpr
            { $result = new PortfolioShare($r.result, $maximumInvestmentInCzk.result); })
        | ( minimumInvestmentInCzk=intExpr UP_TO maximumInvestmentInCzk=intExpr
            { $result = new PortfolioShare($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })
    ) ' % aktuální zůstatkové částky' DOT
;

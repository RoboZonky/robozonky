grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigInteger;
    import com.github.triceo.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [PortfolioStructure result]:
 { $result = new PortfolioStructure(); }
 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })
 (i=portfolioStructureRatingExpression { $result.addItem($i.result); })+
;

targetPortfolioSizeExpression returns [int result] :
    'Cílová zůstatková částka je ' maximumInvestmentInCzk=INTEGER ' ' KC DOT
    {$result = Integer.parseInt($maximumInvestmentInCzk.getText());}
;

portfolioStructureRatingExpression returns [PortfolioStructureItem result] :
    'Prostředky v ratingu ' r=ratingExpression (
        (' mohou tvořit až ' maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioStructureItem($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' musí tvořit ' minimumInvestmentInCzk=INTEGER ' až ' maximumInvestmentInCzk=INTEGER
            { $result = new PortfolioStructureItem($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' % aktuální zůstatkové částky' DOT
;

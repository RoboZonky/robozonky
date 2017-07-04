grammar PortfolioStructure;

import Tokens;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import com.github.triceo.robozonky.api.remote.enums.*;
    import com.github.triceo.robozonky.api.remote.entities.*;
    import com.github.triceo.robozonky.strategy.natural.*;
}

portfolioStructureExpression returns [PortfolioStructure result]:

 { $result = new PortfolioStructure(); }
 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })
 (i=portfolioStructureRatingExpression { $result.addItem($i.result); })+

;

targetPortfolioSizeExpression returns [BigInteger result] :

    'Cílová zůstatková částka je ' maximumInvestmentInCzk=INTEGER ',- Kč.'
    {$result = new BigInteger($maximumInvestmentInCzk.getText());}

;

portfolioStructureRatingExpression returns [PortfolioStructureItem result] :

    ('Prostředky v ratingu ' r=ratingExpression ' mohou tvořit až ' maximumInvestmentInCzk=percentExpression
        ' % aktuální zůstatkové částky.'
        { $result = new PortfolioStructureItem($r.result, $maximumInvestmentInCzk.result); })
    | ('Prostředky v ratingu ' r=ratingExpression ' musí tvořit ' minimumInvestmentInCzk=percentExpression ' až '
        maximumInvestmentInCzk=percentExpression ' % aktuální zůstatkové částky.'
        { $result = new PortfolioStructureItem($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

percentExpression returns [BigInteger result] :
    r=INTEGER_ZERO_TO_HUNDRED { $result = new BigInteger($r.getText()); }
;

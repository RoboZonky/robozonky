grammar InvestmentSize;

import Tokens;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import com.github.triceo.robozonky.api.remote.enums.*;
    import com.github.triceo.robozonky.api.remote.entities.*;
    import com.github.triceo.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [InvestmentSize result]:

 { $result = new InvestmentSize(); }
 (d=defaultInvestmentSizeExpression { $result.setDefaultInvestmentSize($d.result); })
 (i=investmentSizeRatingExpression { $result.addItem($i.result); })+

;

defaultInvestmentSizeExpression returns [DefaultInvestmentSize result] :

    ('Běžná výše investice je ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new DefaultInvestmentSize($maximumInvestmentInCzk.result); })
    | ('Běžná výše investice je ' minimumInvestmentInCzk=investmentAmountExpression  ',- až ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new DefaultInvestmentSize($minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

investmentSizeRatingExpression returns [InvestmentSizeItem result] :

    ('Do půjček v ratingu ' r=ratingExpression ' investovat až ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new InvestmentSizeItem($r.result, $maximumInvestmentInCzk.result); })
    | ('Do půjček v ratingu ' r=ratingExpression ' investovat ' minimumInvestmentInCzk=investmentAmountExpression ',- až '
        maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new InvestmentSizeItem($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

investmentAmountExpression returns [BigInteger result] :
    r=INTEGER_ALLOWED_INVESTMENTS { $result = new BigInteger($r.getText()); }
;

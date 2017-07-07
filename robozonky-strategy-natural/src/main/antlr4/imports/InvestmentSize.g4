grammar InvestmentSize;

import Tokens;

@header {
    import com.github.triceo.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Collection<InvestmentSizeItem> result]:
 { Collection<InvestmentSizeItem> result = new LinkedHashSet<>(); }
 (i=investmentSizeRatingExpression { result.add($i.result); })+
 { $result = result; }
;

investmentSizeRatingExpression returns [InvestmentSizeItem result] :
    'Do úvěrů v ratingu ' r=ratingExpression ' investovat' (
        (UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' ' minimumInvestmentInCzk=INTEGER UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' ' KC DOT
;

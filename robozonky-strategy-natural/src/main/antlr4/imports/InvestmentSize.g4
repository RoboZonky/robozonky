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
    'Do úvěrů v ratingu ' r=ratingExpression (
        (' investovat až ' maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' investovat ' minimumInvestmentInCzk=INTEGER ' až ' maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' ' KC DOT
;

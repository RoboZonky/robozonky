grammar InvestmentSize;

import Tokens;

@header {
    import com.github.triceo.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Collection<InvestmentSize> result]:
 { Collection<InvestmentSize> result = new LinkedHashSet<>(); }
 (i=investmentSizeRatingExpression { result.add($i.result); })+
 { $result = result; }
;

investmentSizeRatingExpression returns [InvestmentSize result] :
    'Do úvěrů v ratingu ' r=ratingExpression ' investovat' (
        (UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSize($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' ' minimumInvestmentInCzk=INTEGER UP_TO maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSize($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' ' KC DOT
;

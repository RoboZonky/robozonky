grammar InvestmentSize;

import Tokens;

@header {
    import com.github.triceo.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [InvestmentSize result]:
 { $result = new InvestmentSize(); }
 (d=defaultInvestmentSizeExpression { $result.setDefaultInvestmentSize($d.result); })
 (i=investmentSizeRatingExpression { $result.addItem($i.result); })+
;

defaultInvestmentSizeExpression returns [DefaultInvestmentSize result] :
    'Běžná výše investice je ' (
        (maximumInvestmentInCzk=INTEGER
            { $result = new DefaultInvestmentSize(Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (minimumInvestmentInCzk=INTEGER ' až ' maximumInvestmentInCzk=INTEGER
        { $result = new DefaultInvestmentSize(Integer.parseInt($minimumInvestmentInCzk.getText()),
            Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' ' KC DOT
;

investmentSizeRatingExpression returns [InvestmentSizeItem result] :
    'Do půjček v ratingu ' r=ratingExpression (
        (' investovat až ' maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($maximumInvestmentInCzk.getText())); })
        | (' investovat ' minimumInvestmentInCzk=INTEGER ' až ' maximumInvestmentInCzk=INTEGER
            { $result = new InvestmentSizeItem($r.result, Integer.parseInt($minimumInvestmentInCzk.getText()),
                Integer.parseInt($maximumInvestmentInCzk.getText())); })
    ) ' ' KC DOT
;

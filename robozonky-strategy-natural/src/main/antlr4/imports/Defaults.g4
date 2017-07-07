grammar Defaults;

import Tokens;

@header {
    import com.github.triceo.robozonky.strategy.natural.*;
}

defaultExpression returns [DefaultValues result]:
 { $result = new DefaultValues(); }
 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })?
 (d=defaultInvestmentSizeExpression { $result.setInvestmentSize($d.result); })?
 (b=targetBalanceExpression { $result.setMinimumBalance($b.result); })?
 (c=confirmationExpression { $result.setConfirmationCondition($c.result); })?
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

targetPortfolioSizeExpression returns [int result] :
    'Cílová zůstatková částka je ' maximumInvestmentInCzk=INTEGER ' ' KC DOT
    {$result = Integer.parseInt($maximumInvestmentInCzk.getText());}
;

targetBalanceExpression returns [int result] :
    'Investovat pouze pokud disponibilní zůstatek přesáhne ' balance=INTEGER ' ' KC DOT
    {$result = Integer.parseInt($balance.getText());}
;

confirmationExpression returns [MarketplaceFilterCondition result] :
    'Potvrzovat mobilem investice do úvěrů, kde ' r=ratingCondition DOT
    {$result = $r.result;}
;


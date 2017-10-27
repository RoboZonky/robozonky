grammar Defaults;

import Tokens;

@header {
    import com.github.robozonky.strategy.natural.*;
}

defaultExpression returns [DefaultValues result]:
 r=portfolioExpression
 { $result = new DefaultValues($r.result); }
 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })?
 (d=defaultInvestmentSizeExpression { $result.setInvestmentSize($d.result); })?
 (s=defaultInvestmentShareExpression { $result.setInvestmentShare($s.result); })?
 (b=targetBalanceExpression { $result.setMinimumBalance($b.result); })?
 (c=confirmationExpression { $result.setConfirmationCondition($c.result); })?
;

defaultInvestmentSizeExpression returns [DefaultInvestmentSize result] :
    'Běžná výše investice je' i=investmentSizeRatingSubExpression {
         $result = $i.result;
    }
;

defaultInvestmentShareExpression returns [DefaultInvestmentShare result] :
    'Investovat maximálně '
        maximumInvestmentInCzk=intExpr
        { $result = new DefaultInvestmentShare($maximumInvestmentInCzk.result); }
    ' % výše úvěru' DOT
;

targetPortfolioSizeExpression returns [int result] :
    'Cílová zůstatková částka je ' maximumInvestmentInCzk=intExpr ' ' KC DOT
    {$result = $maximumInvestmentInCzk.result;}
;

targetBalanceExpression returns [int result] :
    'Investovat pouze pokud disponibilní zůstatek přesáhne ' balance=intExpr ' ' KC DOT
    {$result = $balance.result;}
;

confirmationExpression returns [MarketplaceFilterCondition result] :
    'Potvrzovat mobilem investice do úvěrů, kde ' r=ratingCondition DOT
    {$result = $r.result;}
;


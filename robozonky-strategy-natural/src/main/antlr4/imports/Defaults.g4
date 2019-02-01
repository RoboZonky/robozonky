grammar Defaults;

import Tokens;

@header {
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;
}

defaultExpression returns [DefaultValues result]:
 r=portfolioExpression { $result = new DefaultValues($r.result); }
 (e=exitDateExpression { $result.setExitProperties($e.result); })?
 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })?
 (d=defaultInvestmentSizeExpression { $result.setInvestmentSize($d.result); })?
 (s=defaultInvestmentShareExpression { $result.setInvestmentShare($s.result); })?
 (b=targetBalanceExpression { $result.setMinimumBalance($b.result); })?
 (c=confirmationExpression { $result.setConfirmationCondition($c.result); })?
;

portfolioExpression returns [DefaultPortfolio result] :
    'Robot má udržovat ' (
        ( 'konzervativní' { $result = DefaultPortfolio.CONSERVATIVE; } )
        | ( 'balancované' { $result = DefaultPortfolio.BALANCED; } )
        | ( 'progresivní' { $result = DefaultPortfolio.PROGRESSIVE; } )
        | ( 'uživatelem definované' { $result = DefaultPortfolio.EMPTY; } )
    ) ' portfolio' DOT
;

exitDateExpression returns [ExitProperties result]:
    'Opustit Zonky k ' termination=dateExpr (
        ( { $result = new ExitProperties($termination.result); } )
        | (
            OR_COMMA 'výprodej zahájit ' selloff=dateExpr {
                $result = new ExitProperties($termination.result, $selloff.result);
            }
        )
    ) DOT
;

defaultInvestmentSizeExpression returns [InvestmentSize result] :
    'Běžná výše investice je ' i=investmentSizeRatingSubExpression {
         $result = $i.result;
    }
;

defaultInvestmentShareExpression returns [DefaultInvestmentShare result] :
    'Investovat maximálně '
        maximumInvestmentInCzk=intExpr
        { $result = new DefaultInvestmentShare($maximumInvestmentInCzk.result); }
    ' % výše úvěru' DOT
;

confirmationExpression returns [MarketplaceFilterCondition result] :
    'Potvrzovat mobilem investice do úvěrů, kde ' s=interestCondition DOT { $result = $s.result; }
;


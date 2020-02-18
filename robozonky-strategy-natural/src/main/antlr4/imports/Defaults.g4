grammar Defaults;

import Tokens;

@header {
    import com.github.robozonky.api.strategies.*;
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;
}

defaultExpression returns [DefaultValues result]:
    r2=portfolioExpression { $result = new DefaultValues($r2.result); }
    (v2=reservationExpression { $result.setReservationMode($v2.result); })
    (i2=defaultInvestmentSizeExpression { $result.setInvestmentSize($i2.result); })?
    (b2=defaultPurchaseSizeExpression { $result.setPurchaseSize($b2.result); })?
    (p2=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p2.result); })?
    (e2=exitDateExpression { $result.setExitProperties($e2.result); })?
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

defaultInvestmentSizeExpression returns [int result] :
    'Robot má investovat do půjček po ' amount=intExpr KC '.' { $result = $amount.result; }
;

defaultPurchaseSizeExpression returns [int result] :
    'Robot má nakupovat participace nejvýše za ' amount=intExpr KC '.' { $result = $amount.result; }
;

defaultInvestmentShareExpression returns [DefaultInvestmentShare result] :
    'Investovat maximálně '
        maximumInvestmentInCzk=intExpr
        { $result = new DefaultInvestmentShare($maximumInvestmentInCzk.result); }
    ' % výše úvěru' DOT
;

reservationExpression returns [ReservationMode result] :
    (
        'Robot má pravidelně kontrolovat rezervační systém a přijímat rezervace půjček odpovídajících této strategii.' {
            $result = ReservationMode.ACCEPT_MATCHING;
        }
    ) | (
        'Robot má převzít kontrolu nad rezervačním systémem a přijímat rezervace půjček odpovídajících této strategii.' {
            $result = ReservationMode.FULL_OWNERSHIP;
        }
    )  | (
        'Robot má zcela ignorovat rezervační systém.' {
            $result = null;
        }
    )
;

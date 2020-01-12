grammar Tokens;

@header {
    import java.lang.String;
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.time.LocalDate;
    import java.util.Collection;
    import java.util.LinkedHashSet;
    import com.github.robozonky.api.Ratio;
    import com.github.robozonky.api.remote.enums.*;
    import com.github.robozonky.api.remote.entities.*;
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;
}

targetPortfolioSizeExpression returns [long result] :
    'Cílová zůstatková částka je ' maximumInvestmentInCzk=longExpr KC DOT
    {$result = $maximumInvestmentInCzk.result;}
;

interestCondition returns [MarketplaceFilterCondition result]:
    'úrok ' (
        (c1 = interestConditionRangeOpen { $result = $c1.result; })
        | (c2 = interestConditionRangeClosedLeft { $result = $c2.result; })
        | (c3 = interestConditionRangeClosedRight { $result = $c3.result; })
     ) ' % p.a' DOT?
;

interestConditionRangeOpen returns [MarketplaceFilterCondition result]:
    { boolean needsBothValues = false; }
    IS min=floatExpr
    (
        UP_TO max=floatExpr { needsBothValues = true; }
    ) ?
    {
        if (needsBothValues) {
            // if the second one is provided, use the range
            $result = LoanInterestRateCondition.exact(Ratio.fromPercentage($min.result),
                                                      Ratio.fromPercentage($max.result));
        } else {
            // by default, just pick the one rating
            final Rating r = Rating.findByCode($min.result.toString());
            LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
            c.add(r);
            $result = c;
        }
    }
;

interestConditionRangeClosedLeft returns [MarketplaceFilterCondition result]:
    MORE_THAN min=floatExpr
    {
        $result = LoanInterestRateCondition.moreThan(Ratio.fromPercentage($min.result));
    }
;

interestConditionRangeClosedRight returns [MarketplaceFilterCondition result]:
    LESS_THAN max=floatExpr
    {
        $result = LoanInterestRateCondition.lessThan(Ratio.fromPercentage($max.result));
    }
;

interestEnumeratedExpression returns [Collection<Rating> result]:
    { $result = new LinkedHashSet<Rating>(); }
    (
        (
            r1=interestRateBasedRatingExpression OR_COMMA { $result.add($r1.result); }
        )*
        r2=interestRateBasedRatingExpression OR { $result.add($r2.result); }
    )?
    r3=interestRateBasedRatingExpression { $result.add($r3.result); }
;

interestRateBasedRatingExpression returns [Rating result] :
    r=floatExpr ' % p.a' DOT? { $result = Rating.findByCode($r.result.toString()); }
;

investmentSizeRatingSubExpression returns [MoneyRange result] :
    (
        (amount=intExpr
            { $result = new MoneyRange($amount.result, $amount.result); })
        | ('až' max=intExpr
            { $result = new MoneyRange($max.result); })
        | (min=intExpr UP_TO max=intExpr
            { $result = new MoneyRange($min.result, $max.result); })
    ) KC DOT
;

regionExpression returns [Region result] :
    r=REGION {
        $result = Region.findByCode($r.getText());
    }
;

incomeExpression returns [MainIncomeType result] :
    r=(INCOME | OTHER) {
        $result = MainIncomeType.findByCode($r.getText());
    }
;

purposeExpression returns [Purpose result] :
    r=(PURPOSE | OTHER) {
        $result = Purpose.findByCode($r.getText());
    }
;

healthExpression returns [LoanHealth result] :
    HEALTH_NOT { $result = LoanHealth.CURRENTLY_IN_DUE; }
    | HEALTH_NOW { $result = LoanHealth.HISTORICALLY_IN_DUE; }
    | HEALTH_ALWAYS  { $result = LoanHealth.HEALTHY; }
;

dateExpr returns [LocalDate result] :
    d=intExpr DOT m=intExpr DOT y=intExpr
    {
        $result = LocalDate.of($y.result, $m.result, $d.result);
    }
;

floatExpr returns [BigDecimal result] :
    f=FLOAT {
        final String replaced = $f.getText().replaceFirst("\\Q,\\E", ".");
        $result = new BigDecimal(replaced);
    } | i=intExpr {
        $result = new BigDecimal($i.result);
    }
;

intExpr returns [int result] :
    i=INTEGER {
        $result = Integer.parseInt($i.getText());
    }
;

longExpr returns [long result] :
    i=INTEGER {
        $result = Long.parseLong($i.getText());
    }
;

// regions
REGION   : (REGION_A | REGION_B | REGION_C | REGION_E | REGION_H | REGION_J | REGION_K | REGION_L | REGION_M | REGION_P
            | REGION_S | REGION_T | REGION_U | REGION_Z);
REGION_A : 'Praha';
REGION_B : 'Jihomoravský';
REGION_C : 'Jihočeský';
REGION_E : 'Pardubický';
REGION_H : 'Královéhradecký';
REGION_J : 'Vysočina';
REGION_K : 'Karlovarský';
REGION_L : 'Liberecký';
REGION_M : 'Olomoucký';
REGION_P : 'Plzeňský';
REGION_S : 'Středočeský';
REGION_T : 'Moravskoslezský';
REGION_U : 'Ústecký';
REGION_Z : 'Zlínský';

// main income types
INCOME                      : (INCOME_EMPLOYMENT | INCOME_ENTREPRENEUR | INCOME_SELF_EMPLOYMENT | INCOME_PENSION
                                | INCOME_MATERNITY_LEAVE | INCOME_STUDENT | INCOME_UNEMPLOYED
                                | INCOME_LIBERAL_PROFESSION);
INCOME_EMPLOYMENT           : 'zaměstnanec';
INCOME_ENTREPRENEUR         : 'podnikatel';
INCOME_LIBERAL_PROFESSION   : 'svobodné povolání';
INCOME_MATERNITY_LEAVE      : 'na rodičovské dovolené';
INCOME_PENSION              : 'důchodce';
INCOME_SELF_EMPLOYMENT      : 'OSVČ';
INCOME_STUDENT              : 'student';
INCOME_UNEMPLOYED           : 'bez zaměstnání';

// loan purpose types
PURPOSE                         : (PURPOSE_AUTO_MOTO | PURPOSE_TRAVEL | PURPOSE_HOUSEHOLD | PURPOSE_ELECTRONICS
                                    | PURPOSE_REFINANCING | PURPOSE_OWN_PROJECT | PURPOSE_EDUCATION | PURPOSE_HEALTH);
PURPOSE_AUTO_MOTO               : 'auto-moto';
PURPOSE_TRAVEL                  : 'cestování';
PURPOSE_HOUSEHOLD               : 'domácnost';
PURPOSE_ELECTRONICS             : 'elektronika';
PURPOSE_REFINANCING             : 'refinancování půjček';
PURPOSE_OWN_PROJECT             : 'vlastní projekt';
PURPOSE_EDUCATION               : 'vzdělání';
PURPOSE_HEALTH                  : 'zdraví';

// loan healt states
HEALTH          : HEALTH_ALWAYS | HEALTH_NOW | HEALTH_NOT;
HEALTH_ALWAYS   : 'nikdy nebyla';
HEALTH_NOW      : 'nyní není';
HEALTH_NOT      : 'nyní je';

// shared strings
KC        : ' Kč' ;
DOT       : '.' ;
DELIM     : '- ' ;
UP_TO     : ' až ';
IS        : 'je ';
OR        : ' nebo ';
OR_COMMA  : COMMA ' ';
LESS_THAN : 'nedosahuje ';
MORE_THAN : 'přesahuje ';
OTHER     : 'jiné';

// basic types
INTEGER    : DIGIT+;
FLOAT      : DIGIT+ COMMA DIGIT+;

// skip whitespace and comments
COMMENT     : '#' ~[\r\n]* NEWLINE -> skip ;
NEWLINE     : ('\r\n' | '\r' | '\n') -> skip;
WHITESPACE  : [ \t]+ -> skip;

fragment DIGIT: [0-9];
fragment COMMA: ',';


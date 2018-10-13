grammar Tokens;

@header {
    import java.lang.String;
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.time.LocalDate;
    import java.util.Collection;
    import java.util.LinkedHashSet;
    import com.github.robozonky.api.remote.enums.*;
    import com.github.robozonky.api.remote.entities.*;
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;
}

portfolioExpression returns [DefaultPortfolio result] :
    'Robot má udržovat ' (
        ( 'konzervativní' { $result = DefaultPortfolio.CONSERVATIVE; } )
        | ( 'balancované' { $result = DefaultPortfolio.BALANCED; } )
        | ( 'progresivní' { $result = DefaultPortfolio.PROGRESSIVE; } )
        | ( 'prázdné' { $result = DefaultPortfolio.EMPTY; } )
    ) ' portfolio' DOT
;

targetPortfolioSizeExpression returns [long result] :
    'Cílová zůstatková částka je ' maximumInvestmentInCzk=longExpr KC DOT
    {$result = $maximumInvestmentInCzk.result;}
;

targetBalanceExpression returns [long result] :
    'Investovat pouze pokud disponibilní zůstatek přesáhne ' balance=longExpr KC DOT
    {$result = $balance.result;}
;

ratingCondition returns [MarketplaceFilterCondition result]:
    'rating ' IS (
        ( r1=ratingEnumeratedExpression
            {
                LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
                c.add($r1.result);
                $result = c;
            })
        | ('lepší než ' r2=ratingExpression { $result = new LoanRatingBetterCondition($r2.result); })
        | ('horší než ' r3=ratingExpression { $result = new LoanRatingWorseCondition($r3.result); })
    )
;

ratingExpression returns [Rating result] :
    r=RATING
    { $result = Rating.findByCode($r.getText()); }
;

ratingEnumeratedExpression returns [Collection<Rating> result]:
    { $result = new LinkedHashSet<Rating>(); }
    (
        (
            r1=ratingExpression OR_COMMA { $result.add($r1.result); }
        )*
        r2=ratingExpression OR { $result.add($r2.result); }
    )?
    r3=ratingExpression { $result.add($r3.result); }
;

investmentSizeRatingSubExpression returns [InvestmentSize result] :
    (
        (amount=intExpr
            { $result = new InvestmentSize($amount.result, $amount.result); })
        | ('až' max=intExpr
            { $result = new InvestmentSize($max.result); })
        | (min=intExpr UP_TO max=intExpr
            { $result = new InvestmentSize($min.result, $max.result); })
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

// ratings
RATING       : (RATING_AAAAA | RATING_AAAA | RATING_AAA | RATING_AA | RATING_A | RATING_B | RATING_C | RATING_D);
RATING_AAAAA : 'A**';
RATING_AAAA  : 'A*';
RATING_AAA   : 'A++';
RATING_AA    : 'A+';
RATING_A     : 'A';
RATING_B     : 'B';
RATING_C     : 'C';
RATING_D     : 'D';

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
PURPOSE                         : (PURPOSE_AUTO_MOTO | PURPOSE_CESTOVANI | PURPOSE_DOMACNOST | PURPOSE_ELEKTRONIKA
                                    | PURPOSE_REFINANCOVANI_PUJCEK | PURPOSE_VLASTNI_PROJEKT | PURPOSE_VZDELANI
                                    | PURPOSE_ZDRAVI);
PURPOSE_AUTO_MOTO               : 'auto-moto';
PURPOSE_CESTOVANI               : 'cestování';
PURPOSE_DOMACNOST               : 'domácnost';
PURPOSE_ELEKTRONIKA             : 'elektronika';
PURPOSE_REFINANCOVANI_PUJCEK    : 'refinancování půjček';
PURPOSE_VLASTNI_PROJEKT         : 'vlastní projekt';
PURPOSE_VZDELANI                : 'vzdělání';
PURPOSE_ZDRAVI                  : 'zdraví';

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


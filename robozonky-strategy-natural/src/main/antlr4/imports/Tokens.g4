grammar Tokens;

@header {
    import java.lang.String;
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.util.Collection;
    import java.util.LinkedHashSet;
    import com.github.triceo.robozonky.api.remote.enums.*;
    import com.github.triceo.robozonky.api.remote.entities.*;
    import com.github.triceo.robozonky.strategy.natural.*;
}

portfolioExpression returns [DefaultPortfolio result] :
    'Robot má udržovat ' (
        ( 'konzervativní' { $result = DefaultPortfolio.CONSERVATIVE; } )
        | ( 'balancované' { $result = DefaultPortfolio.BALANCED; } )
        | ( 'progresivní' { $result = DefaultPortfolio.PROGRESSIVE; } )
        | ( 'prázdné' { $result = DefaultPortfolio.EMPTY; } )
    ) ' portfolio' DOT
;

ratingCondition returns [MarketplaceFilterCondition result]:
    'rating je ' (
        ( r1=ratingEnumeratedExpression
            {
                AbstractEnumeratedCondition<Rating> c = new LoanRatingEnumeratedCondition();
                c.add($r1.result);
                $result = c;
            })
        | ('lepší než ' r2=ratingExpression { $result = new LoanRatingBetterOrEqualCondition($r2.result); })
        | ('horší než ' r3=ratingExpression { $result = new LoanRatingWorseOrEqualCondition($r3.result); })
    )
;

ratingExpression returns [Rating result] :
    r=(RATING_AAAAA | RATING_AAAA | RATING_AAA | RATING_AA | RATING_A | RATING_B | RATING_C | RATING_D)
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

regionExpression returns [Region result] :
    r=(REGION_A | REGION_B | REGION_C | REGION_E | REGION_H | REGION_J | REGION_K | REGION_L | REGION_M | REGION_P |
        REGION_S | REGION_T | REGION_U | REGION_Z)
    { $result = Region.findByCode($r.getText()); }
;

incomeExpression returns [MainIncomeType result] :
    r=(INCOME_EMPLOYMENT | INCOME_ENTREPRENEUR | INCOME_SELF_EMPLOYMENT | INCOME_PENSION | INCOME_MATERNITY_LEAVE
        | INCOME_STUDENT | INCOME_UNEMPLOYED | INCOME_LIBERAL_PROFESSION | INCOME_OTHER )
    { $result = MainIncomeType.findByCode($r.getText()); }
;

purposeExpression returns [Purpose result] :
    r=(PURPOSE_AUTO_MOTO | PURPOSE_CESTOVANI | PURPOSE_DOMACNOST | PURPOSE_ELEKTRONIKA | PURPOSE_REFINANCOVANI_PUJCEK
        | PURPOSE_VLASTNI_PROJEKT | PURPOSE_VZDELANI | PURPOSE_ZDRAVI | PURPOSE_JINE)
    { $result = Purpose.findByCode($r.getText()); }
;

floatExpression returns [BigDecimal result] :
    f=FLOAT {
        final String replaced = $f.getText().replaceFirst("\\Q,\\E", ".");
        $result = new BigDecimal(replaced);
    }
;

// shared strings
KC      : 'Kč' ;
DOT     : '.' ;
DELIM   : '- ' ;
UP_TO   : ' až ';
OR      : ' nebo ';
OR_COMMA: COMMA ' ';

// regions
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
RATING_AAAAA : 'A**';
RATING_AAAA  : 'A*';
RATING_AAA   : 'A++';
RATING_AA    : 'A+';
RATING_A     : 'A';
RATING_B     : 'B';
RATING_C     : 'C';
RATING_D     : 'D';

// main income types
INCOME_EMPLOYMENT           : 'zaměstnanec';
INCOME_ENTREPRENEUR         : 'podnikatel';
INCOME_LIBERAL_PROFESSION   : 'svobodné povolání';
INCOME_MATERNITY_LEAVE      : 'na rodičovské dovolené';
INCOME_PENSION              : 'důchodce';
INCOME_SELF_EMPLOYMENT      : 'OSVČ';
INCOME_STUDENT              : 'student';
INCOME_UNEMPLOYED           : 'bez zaměstnání';
INCOME_OTHER                : 'ostatní';

// loan purpose types
PURPOSE_AUTO_MOTO               : 'auto-moto';
PURPOSE_CESTOVANI               : 'cestování';
PURPOSE_DOMACNOST               : 'domácnost';
PURPOSE_ELEKTRONIKA             : 'elektronika';
PURPOSE_REFINANCOVANI_PUJCEK    : 'refinancování půjček';
PURPOSE_VLASTNI_PROJEKT         : 'vlastní projekt';
PURPOSE_VZDELANI                : 'vzdělání';
PURPOSE_ZDRAVI                  : 'zdraví';
PURPOSE_JINE                    : 'jiné';

// basic types
INTEGER : DIGIT+ ;
FLOAT   : DIGIT+ COMMA DIGIT+;

// skip whitespace and comments
COMMENT     : ('#' ~( '\r' | '\n' )*) -> skip;
WHITESPACE  : (' '|'\r'|'\n'|'\t') -> channel(HIDDEN);

fragment DIGIT: [0-9];
fragment COMMA: ',';


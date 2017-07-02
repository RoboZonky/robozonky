grammar NaturalLanguageStrategy;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import com.github.triceo.robozonky.api.remote.enums.*;
    import com.github.triceo.robozonky.api.remote.entities.*;
    import com.github.triceo.robozonky.strategy.natural.*;
}

primaryExpression returns [ParsedStrategy result] :
    '1. Struktura portfolia'

    portfolioStructureExpression

    '2. Velikost investice'
    '3. Výjimky'
    EOF
    {$result = new ParsedStrategy();}
;

portfolioStructureExpression returns [PortfolioStructure result]
    @init {
        $result = new PortfolioStructure();
    } :

 (i=portfolioStructureRatingExpression { $result.addItem($i.result); })+

;

portfolioStructureRatingExpression returns [PortfolioStructureItem result] :

    ('Rating ' r=ratingExpression ' může představovat až ' max=percentExpression ' % portfolia.'
        { $result = new PortfolioStructureItem($r.result, $max.result); })
    | ('Rating ' r=ratingExpression ' může představovat ' min=percentExpression ' až ' max=percentExpression ' % portfolia.'
        { $result = new PortfolioStructureItem($r.result, $min.result, $max.result); })

;

percentExpression returns [BigInteger result] :
    r=INTEGER_ZERO_TO_HUNDRED { $result = new BigInteger($r.getText()); }
;

ratingExpression returns [Rating result] :
    r=(AAAAA | AAAA | AAA | AA | A | B | C | D) { $result = Rating.findByCode($r.getText()); }
;

// ratings
AAAAA : 'A**';
AAAA  : 'A*';
AAA   : 'A++';
AA    : 'A+';
A     : 'A';
B     : 'B';
C     : 'C';
D     : 'D';

// basic types
INTEGER_ZERO_TO_HUNDRED: '0' | '100' | [0-9] | [1-9][0-9];
INTEGER : [1-9][0-9]* ;
DOUBLE  : INTEGER ',' [0-9]*[1-9];

// skip whitespace and comments
COMMENT : ('#' ~( '\r' | '\n' )*) -> skip;
NL      : ( [\r\n]+ ) -> skip;


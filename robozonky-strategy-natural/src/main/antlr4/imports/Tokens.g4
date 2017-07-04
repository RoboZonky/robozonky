grammar Tokens;

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
INTEGER_ALLOWED_INVESTMENTS: '200' | '400' | '600' | '800' | '1000' |
    '1200' | '1400' | '1600' | '1800' | '2000' |
    '2200' | '2400' | '2600' | '2800' | '3000' |
    '3200' | '3400' | '3600' | '3800' | '4000' |
    '4200' | '4400' | '4600' | '4800' | '5000';
INTEGER : [1-9][0-9]* ;
DOUBLE  : INTEGER ',' [0-9]*[1-9];

// skip whitespace and comments
COMMENT : ('#' ~( '\r' | '\n' )*) -> skip;
NL      : ( [\r\n]+ ) -> skip;


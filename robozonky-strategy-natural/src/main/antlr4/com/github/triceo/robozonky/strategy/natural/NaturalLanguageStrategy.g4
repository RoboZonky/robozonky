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

    investmentSizeExpression

    '3. Výjimky'
    EOF
    {$result = new ParsedStrategy();}
;

portfolioStructureExpression returns [PortfolioStructure result]
    @init {
        $result = new PortfolioStructure();
    } :

 (p=targetPortfolioSizeExpression { $result.setTargetPortfolioSize($p.result); })
 (i=portfolioStructureRatingExpression { $result.addItem($i.result); })+

;

targetPortfolioSizeExpression returns [BigInteger result] :

    'Cílová zůstatková částka je ' maximumInvestmentInCzk=INTEGER ',- Kč.'
    {$result = new BigInteger($maximumInvestmentInCzk.getText());}

;

portfolioStructureRatingExpression returns [PortfolioStructureItem result] :

    ('Prostředky v ratingu ' r=ratingExpression ' mohou tvořit až ' maximumInvestmentInCzk=percentExpression
        ' % aktuální zůstatkové částky.'
        { $result = new PortfolioStructureItem($r.result, $maximumInvestmentInCzk.result); })
    | ('Prostředky v ratingu ' r=ratingExpression ' musí tvořit ' minimumInvestmentInCzk=percentExpression ' až '
        maximumInvestmentInCzk=percentExpression ' % aktuální zůstatkové částky.'
        { $result = new PortfolioStructureItem($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

investmentSizeExpression returns [InvestmentSize result]
    @init {
        $result = new InvestmentSize();
    } :

 (d=defaultInvestmentSizeExpression { $result.setDefaultInvestmentSize($d.result); })
 (i=investmentSizeRatingExpression { $result.addItem($i.result); })+

;

defaultInvestmentSizeExpression returns [DefaultInvestmentSize result] :

    ('Běžná výše investice je ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new DefaultInvestmentSize($maximumInvestmentInCzk.result); })
    | ('Běžná výše investice je ' minimumInvestmentInCzk=investmentAmountExpression  ',- až ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new DefaultInvestmentSize($minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

investmentSizeRatingExpression returns [InvestmentSizeItem result] :

    ('Do půjček v ratingu ' r=ratingExpression ' investovat až ' maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new InvestmentSizeItem($r.result, $maximumInvestmentInCzk.result); })
    | ('Do půjček v ratingu ' r=ratingExpression ' investovat ' minimumInvestmentInCzk=investmentAmountExpression ',- až '
        maximumInvestmentInCzk=investmentAmountExpression ',- Kč.'
        { $result = new InvestmentSizeItem($r.result, $minimumInvestmentInCzk.result, $maximumInvestmentInCzk.result); })

;

investmentAmountExpression returns [BigInteger result] :
    r=INTEGER_ALLOWED_INVESTMENTS { $result = new BigInteger($r.getText()); }
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


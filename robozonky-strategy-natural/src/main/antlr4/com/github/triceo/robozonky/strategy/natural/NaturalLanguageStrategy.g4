grammar NaturalLanguageStrategy;

import InvestmentSize, PortfolioStructure;

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

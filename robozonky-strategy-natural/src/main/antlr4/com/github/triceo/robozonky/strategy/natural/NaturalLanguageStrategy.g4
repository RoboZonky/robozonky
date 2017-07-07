grammar NaturalLanguageStrategy;

import InvestmentSize, PortfolioStructure, MarketplaceFilters;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.util.Collection;
    import java.util.Collections;
    import com.github.triceo.robozonky.api.remote.enums.*;
    import com.github.triceo.robozonky.api.remote.entities.*;
    import com.github.triceo.robozonky.strategy.natural.*;
}

primaryExpression returns [ParsedStrategy result] :

    '1. Struktura portfolia'
    p=portfolioStructureExpression

    '2. Velikost investice'
    i=investmentSizeExpression

    { Collection<MarketplaceFilter> filters = Collections.emptyList(); }
    (
        '3. Filtrování tržiště'
        m=marketplaceFilterExpression
        { filters = $m.result; }
    )?

    EOF
    {$result = new ParsedStrategy($p.result, $i.result, filters);}
;

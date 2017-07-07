grammar NaturalLanguageStrategy;

import Defaults, InvestmentSize, PortfolioStructure, MarketplaceFilters;

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

    DELIM 'Obecná nastavení'
    d=defaultExpression

    { Collection<PortfolioStructureItem> portfolioStructures = Collections.emptyList(); }
    (
        DELIM 'Struktura portfolia'
        p=portfolioStructureExpression
        { portfolioStructures = $p.result; }
    )?

    { Collection<InvestmentSizeItem> investmentSizes = Collections.emptyList(); }
    (
        DELIM 'Velikost investice'
        i=investmentSizeExpression
        { investmentSizes = $i.result; }
    )?

    { Collection<MarketplaceFilter> filters = Collections.emptyList(); }
    (
        DELIM 'Filtrování tržiště'
        m=marketplaceFilterExpression
        { filters = $m.result; }
    )?

    EOF
    {$result = new ParsedStrategy($d.result, portfolioStructures, investmentSizes, filters);}
;

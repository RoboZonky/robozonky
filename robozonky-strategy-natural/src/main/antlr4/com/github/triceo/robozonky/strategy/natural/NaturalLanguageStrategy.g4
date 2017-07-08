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
    ( s=portfolioExpression { $result = new ParsedStrategy($s.result); })
    | ( c=complexExpression { $result = $c.result; })
;

complexExpression returns [ParsedStrategy result] :

    DELIM 'Obecná nastavení'
    d=defaultExpression

    { Collection<PortfolioShare> portfolioStructures = Collections.emptyList(); }
    (
        DELIM 'Úprava struktury portfolia'
        p=portfolioStructureExpression
        { portfolioStructures = $p.result; }
    )?

    { Collection<InvestmentSize> investmentSizes = Collections.emptyList(); }
    (
        DELIM 'Výše investice'
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

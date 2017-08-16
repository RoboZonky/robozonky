grammar NaturalLanguageStrategy;

import Defaults, InvestmentSize, PortfolioStructure, MarketplaceFilters, SellRules;

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
    (
        ( s=portfolioExpression { $result = new ParsedStrategy($s.result); })
        | ( c=complexExpression { $result = $c.result; })
    )
    EOF
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

    { Map<Boolean, Collection<MarketplaceFilter>> buyFilters = Collections.emptyMap(); }
    (
        DELIM 'Filtrování tržiště'
        m=marketplaceFilterExpression
        { buyFilters = $m.result; }
    )?

    { Collection<MarketplaceFilter> sellFilters = Collections.emptyList(); }
    (
        DELIM 'Prodej participací'
        s=sellFilterExpression
        { sellFilters = $s.result; }
    )?

    {$result = new ParsedStrategy($d.result, portfolioStructures, investmentSizes, buyFilters, sellFilters);}
;

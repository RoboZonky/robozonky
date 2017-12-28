grammar NaturalLanguageStrategy;

import Defaults, InvestmentSize, PortfolioStructure, MarketplaceFilters;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.util.Collection;
    import java.util.Collections;
    import com.github.robozonky.api.remote.enums.*;
    import com.github.robozonky.api.remote.entities.*;
    import com.github.robozonky.strategy.natural.*;
}

primaryExpression returns [ParsedStrategy result] :

    v=minimumVersionExpression?

    (
        ( s=portfolioExpression { $result = new ParsedStrategy($s.result); })
        | ( c=complexExpression { $result = $c.result; })
    ) {
        // only set version when the optional expression was actually present
        if ($ctx.minimumVersionExpression() != null) $result.setMinimumVersion($v.result);
    }
    EOF
;

minimumVersionExpression returns [RoboZonkyVersion result] :
    'Tato strategie vyžaduje RoboZonky ve verzi ' major=intExpr DOT minor=intExpr DOT micro=intExpr ' nebo pozdější.' {
        $result = new RoboZonkyVersion($major.result, $minor.result, $micro.result);
    }
;

complexExpression returns [ParsedStrategy result]
    @init {
        Collection<PortfolioShare> portfolioStructures = Collections.emptyList();
        Map<Rating, InvestmentSize> investmentSizes = Collections.emptyMap();
        Collection<MarketplaceFilter> primaryFilters = Collections.emptyList();
        Collection<MarketplaceFilter> secondaryFilters = Collections.emptyList();
        Collection<MarketplaceFilter> sellFilters = Collections.emptyList();
    }:

    DELIM 'Obecná nastavení'
    d=defaultExpression

    (
        DELIM 'Úprava struktury portfolia'
        p=portfolioStructureExpression
        { portfolioStructures = $p.result; }
    )?

    (
        DELIM 'Výše investice'
        i=investmentSizeExpression
        { investmentSizes = $i.result; }
    )?

    (
        DELIM 'Filtrování tržiště'
        m=marketplaceFilterExpression {
            primaryFilters = $m.primary;
            secondaryFilters = $m.secondary;
        }
    )?

    (
        DELIM 'Prodej participací'
        s=sellFilterExpression
        { sellFilters = $s.result; }
    )?

    {
        final DefaultValues v = $d.result;
        $result = new ParsedStrategy(v, portfolioStructures, investmentSizes,
                                     new FilterSupplier(v, primaryFilters, secondaryFilters, sellFilters));
    }
;

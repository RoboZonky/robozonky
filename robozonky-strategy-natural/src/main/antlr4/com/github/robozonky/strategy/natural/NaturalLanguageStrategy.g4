grammar NaturalLanguageStrategy;

import Defaults, InvestmentSize, PortfolioStructure, MarketplaceFilters;

@header {
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.util.Collection;
    import java.util.Collections;
    import org.apache.logging.log4j.LogManager;
    import com.github.robozonky.api.remote.enums.*;
    import com.github.robozonky.api.remote.entities.*;
    import com.github.robozonky.strategy.natural.*;
}

primaryExpression returns [ParsedStrategy result] :

    v=minimumVersionExpression?

    (
        ( s=portfolioExpression {
            final DefaultValues v = new DefaultValues($s.result);
            // enable primary and secondary marketplaces, disable selling, do not enable reservation system
            final FilterSupplier f = new FilterSupplier(v, Collections.emptySet(), Collections.emptySet());
            $result = new ParsedStrategy(v, Collections.emptySet(), Collections.emptyMap(), f); })
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
        (
            { boolean marketplaceFiltersMissing = true; }
            (
                DELIM 'Filtrování tržiště'
                m=marketplaceFilterExpression {
                    primaryFilters = $m.primaryEnabled ? $m.primary : null;
                    secondaryFilters = $m.secondaryEnabled ? $m.secondary : null;
                    marketplaceFiltersMissing = false;
                }
            )? {
                if (marketplaceFiltersMissing) {
                    LogManager.getLogger(this.getClass())
                        .warn("Marketplace filters are missing without excuse. This is deprecated and will eventually break.");
                }
            }
        ) | (
            'Ignorovat všechny půjčky i participace.' {
                primaryFilters = null;
                secondaryFilters = null;
            }
        ) | (
            'Investovat do všech půjček a participací.'
        )
    )

    { boolean emptySellFiltersIsOk = true; }
    (
        (
            DELIM 'Prodej participací'
            s=sellFilterExpression {
                sellFilters = $s.result;
                if (sellFilters.isEmpty()) emptySellFiltersIsOk = false;
            }
        ) | (
            'Prodej participací zakázán.' {
                sellFilters = Collections.emptySet();
            }
        )
    )? {
        if (!emptySellFiltersIsOk) {
            LogManager.getLogger(this.getClass())
                .warn("Sell filters are missing without excuse. This is deprecated and will eventually break.");
        }
    }

    {
        final DefaultValues v = $d.result;
        $result = new ParsedStrategy(v, portfolioStructures, investmentSizes,
                                     new FilterSupplier(v, primaryFilters, secondaryFilters, sellFilters));
    }
;

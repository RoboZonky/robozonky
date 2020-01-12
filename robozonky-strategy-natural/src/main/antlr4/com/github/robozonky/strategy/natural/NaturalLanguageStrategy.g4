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
        Map<Rating, InvestmentSize> purchaseSizes = Collections.emptyMap();
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
        (
            DELIM 'Výše investice'
            i1=legacyInvestmentSizeExpression
            { investmentSizes = $i1.result; }
        ) | (
            (
                DELIM 'Výše investice'
                i2=investmentSizeExpression
                { investmentSizes = $i2.result; }
            )
            (
                DELIM 'Výše nákupu'
                i3=purchaseSizeExpression
                { purchaseSizes = $i3.result; }
            )?

        )
    )?

    (
        (
            (
                DELIM 'Filtrování tržiště'
                m=marketplaceFilterExpression {
                    primaryFilters = $m.primaryEnabled ? $m.primary : null;
                    secondaryFilters = $m.secondaryEnabled ? $m.secondary : null;
                }
            )
        ) | (
            'Ignorovat všechny půjčky i participace.' {
                primaryFilters = null;
                secondaryFilters = null;
            }
        ) | (
            'Investovat do všech půjček a participací.'
        )
    )

    { DefaultValues v = $d.result; }
    (
        (
            DELIM 'Prodej participací'
            s=sellFilterExpression {
                v.setSellingMode(SellingMode.SELL_FILTERS);
                sellFilters = $s.result;
            }
        ) | (
             'Prodávat všechny participace bez poplatku a slevy, které odpovídají filtrům tržiště.' {
                 v.setSellingMode(SellingMode.FREE_UNDISCOUNTED_AND_OUTSIDE_STRATEGY);
             }
         ) | (
            'Prodávat všechny participace bez poplatku, které odpovídají filtrům tržiště.' {
                v.setSellingMode(SellingMode.FREE_AND_OUTSIDE_STRATEGY);
            }
        ) | (
            'Prodej participací zakázán.'
        )
    )

    {
        $result = new ParsedStrategy(v, portfolioStructures, investmentSizes, purchaseSizes,
                                     new FilterSupplier(v, primaryFilters, secondaryFilters, sellFilters));
    }
;

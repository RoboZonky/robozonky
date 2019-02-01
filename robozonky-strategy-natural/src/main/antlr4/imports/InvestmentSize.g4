grammar InvestmentSize;

import Tokens;

@header {
    import java.util.Map;
    import java.util.EnumMap;
    import com.github.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Map<Rating, InvestmentSize> result]:
    {
        final EnumMap<Rating, InvestmentSize> result = new EnumMap<>(Rating.class);
    }
    (i=investmentSizeInterestRateExpression { result.put($i.rating, $i.size); })+ {
        $result = result;
    }
;

investmentSizeInterestRateExpression returns [Rating rating, InvestmentSize size] :
    'S úročením ' r=interestRateBasedRatingExpression 'jednotlivě investovat ' i=investmentSizeRatingSubExpression {
        $rating = $r.result;
        $size = $i.result;
    }
;

grammar InvestmentSize;

import Tokens;

@header {
    import java.util.Map;
    import java.util.EnumMap;
    import com.github.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Map<Rating, InvestmentSize> result]
    @init {
        final EnumMap<Rating, InvestmentSize> result = new EnumMap<>(Rating.class);
    }:
    (i=investmentSizeRatingExpression { result.put($i.rating, $i.size); })* {
        $result = result;
    }
;

investmentSizeRatingExpression returns [Rating rating, InvestmentSize size] :
    'Do úvěrů v ratingu ' r=ratingExpression 'investovat ' i=investmentSizeRatingSubExpression {
        $rating = $r.result;
        $size = $i.result;
    }
;

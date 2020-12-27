grammar InvestmentSize;

import Tokens;

@header {
    import java.util.Map;
    import java.util.EnumMap;
    import com.github.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Map<Ratio, MoneyRange> result]:
    {
        final Map<Ratio, MoneyRange> result = new HashMap<>();
    }
    (i=investmentSizeInterestRateExpression { result.put($i.rating, $i.size); })+ {
        $result = result;
    }
;

investmentSizeInterestRateExpression returns [Ratio rating, MoneyRange size] :
    'Do půjček s úročením ' r=interestRateBasedRatingExpression 'investovat po ' i=intExpr KC DOT {
        $rating = $r.result;
        $size = new MoneyRange($i.result, $i.result);
    }
;

purchaseSizeExpression returns [Map<Ratio, MoneyRange> result]:
    {
        final Map<Ratio, MoneyRange> result = new HashMap<>();
    }
    (i=purchaseSizeInterestRateExpression { result.put($i.rating, $i.size); })+ {
        $result = result;
    }
;

purchaseSizeInterestRateExpression returns [Ratio rating, MoneyRange size] :
    'Participace s úročením ' r=interestRateBasedRatingExpression 'nakupovat nejvýše za ' i=intExpr KC DOT {
        $rating = $r.result;
        $size = new MoneyRange(1, $i.result);
    }
;

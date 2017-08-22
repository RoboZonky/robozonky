grammar InvestmentSize;

import Tokens;

@header {
    import com.github.robozonky.strategy.natural.*;
}

investmentSizeExpression returns [Collection<InvestmentSize> result]:
 { Collection<InvestmentSize> result = new LinkedHashSet<>(); }
 (i=investmentSizeRatingExpression { result.add($i.result); })+
 { $result = result; }
;

investmentSizeRatingExpression returns [InvestmentSize result] :
    'Do úvěrů v ratingu ' r=ratingExpression ' investovat' i=investmentSizeRatingSubExpression {
        $result = new InvestmentSize($r.result, $i.result);
    }
;

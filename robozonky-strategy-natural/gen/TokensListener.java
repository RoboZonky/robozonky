/*
 * Copyright 2020 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated from /home/triceo/IdeaProjects/robozonky/robozonky-strategy-natural/src/main/antlr4/imports/Tokens.g4 by ANTLR 4.8

    import java.lang.String;
    import java.math.BigDecimal;
    import java.math.BigInteger;
    import java.time.LocalDate;
    import java.util.Collection;
    import java.util.LinkedHashSet;
    import com.github.robozonky.api.Ratio;
    import com.github.robozonky.api.remote.enums.*;
    import com.github.robozonky.api.remote.entities.*;
    import com.github.robozonky.strategy.natural.*;
    import com.github.robozonky.strategy.natural.conditions.*;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TokensParser}.
 */
public interface TokensListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TokensParser#targetPortfolioSizeExpression}.
	 * @param ctx the parse tree
	 */
	void enterTargetPortfolioSizeExpression(TokensParser.TargetPortfolioSizeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#targetPortfolioSizeExpression}.
	 * @param ctx the parse tree
	 */
	void exitTargetPortfolioSizeExpression(TokensParser.TargetPortfolioSizeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestCondition}.
	 * @param ctx the parse tree
	 */
	void enterInterestCondition(TokensParser.InterestConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestCondition}.
	 * @param ctx the parse tree
	 */
	void exitInterestCondition(TokensParser.InterestConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestConditionRangeOpen}.
	 * @param ctx the parse tree
	 */
	void enterInterestConditionRangeOpen(TokensParser.InterestConditionRangeOpenContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestConditionRangeOpen}.
	 * @param ctx the parse tree
	 */
	void exitInterestConditionRangeOpen(TokensParser.InterestConditionRangeOpenContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestConditionRangeClosedLeft}.
	 * @param ctx the parse tree
	 */
	void enterInterestConditionRangeClosedLeft(TokensParser.InterestConditionRangeClosedLeftContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestConditionRangeClosedLeft}.
	 * @param ctx the parse tree
	 */
	void exitInterestConditionRangeClosedLeft(TokensParser.InterestConditionRangeClosedLeftContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestConditionRangeClosedRight}.
	 * @param ctx the parse tree
	 */
	void enterInterestConditionRangeClosedRight(TokensParser.InterestConditionRangeClosedRightContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestConditionRangeClosedRight}.
	 * @param ctx the parse tree
	 */
	void exitInterestConditionRangeClosedRight(TokensParser.InterestConditionRangeClosedRightContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestEnumeratedExpression}.
	 * @param ctx the parse tree
	 */
	void enterInterestEnumeratedExpression(TokensParser.InterestEnumeratedExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestEnumeratedExpression}.
	 * @param ctx the parse tree
	 */
	void exitInterestEnumeratedExpression(TokensParser.InterestEnumeratedExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#interestRateBasedRatingExpression}.
	 * @param ctx the parse tree
	 */
	void enterInterestRateBasedRatingExpression(TokensParser.InterestRateBasedRatingExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#interestRateBasedRatingExpression}.
	 * @param ctx the parse tree
	 */
	void exitInterestRateBasedRatingExpression(TokensParser.InterestRateBasedRatingExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#investmentSizeRatingSubExpression}.
	 * @param ctx the parse tree
	 */
	void enterInvestmentSizeRatingSubExpression(TokensParser.InvestmentSizeRatingSubExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#investmentSizeRatingSubExpression}.
	 * @param ctx the parse tree
	 */
	void exitInvestmentSizeRatingSubExpression(TokensParser.InvestmentSizeRatingSubExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#regionExpression}.
	 * @param ctx the parse tree
	 */
	void enterRegionExpression(TokensParser.RegionExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#regionExpression}.
	 * @param ctx the parse tree
	 */
	void exitRegionExpression(TokensParser.RegionExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#incomeExpression}.
	 * @param ctx the parse tree
	 */
	void enterIncomeExpression(TokensParser.IncomeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#incomeExpression}.
	 * @param ctx the parse tree
	 */
	void exitIncomeExpression(TokensParser.IncomeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#purposeExpression}.
	 * @param ctx the parse tree
	 */
	void enterPurposeExpression(TokensParser.PurposeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#purposeExpression}.
	 * @param ctx the parse tree
	 */
	void exitPurposeExpression(TokensParser.PurposeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#healthExpression}.
	 * @param ctx the parse tree
	 */
	void enterHealthExpression(TokensParser.HealthExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#healthExpression}.
	 * @param ctx the parse tree
	 */
	void exitHealthExpression(TokensParser.HealthExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#dateExpr}.
	 * @param ctx the parse tree
	 */
	void enterDateExpr(TokensParser.DateExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#dateExpr}.
	 * @param ctx the parse tree
	 */
	void exitDateExpr(TokensParser.DateExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#floatExpr}.
	 * @param ctx the parse tree
	 */
	void enterFloatExpr(TokensParser.FloatExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#floatExpr}.
	 * @param ctx the parse tree
	 */
	void exitFloatExpr(TokensParser.FloatExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#intExpr}.
	 * @param ctx the parse tree
	 */
	void enterIntExpr(TokensParser.IntExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#intExpr}.
	 * @param ctx the parse tree
	 */
	void exitIntExpr(TokensParser.IntExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link TokensParser#longExpr}.
	 * @param ctx the parse tree
	 */
	void enterLongExpr(TokensParser.LongExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link TokensParser#longExpr}.
	 * @param ctx the parse tree
	 */
	void exitLongExpr(TokensParser.LongExprContext ctx);
}

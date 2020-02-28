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

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TokensParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TokensVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TokensParser#targetPortfolioSizeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTargetPortfolioSizeExpression(TokensParser.TargetPortfolioSizeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestCondition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestCondition(TokensParser.InterestConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestConditionRangeOpen}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestConditionRangeOpen(TokensParser.InterestConditionRangeOpenContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestConditionRangeClosedLeft}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestConditionRangeClosedLeft(TokensParser.InterestConditionRangeClosedLeftContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestConditionRangeClosedRight}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestConditionRangeClosedRight(TokensParser.InterestConditionRangeClosedRightContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestEnumeratedExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestEnumeratedExpression(TokensParser.InterestEnumeratedExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#interestRateBasedRatingExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterestRateBasedRatingExpression(TokensParser.InterestRateBasedRatingExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#investmentSizeRatingSubExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvestmentSizeRatingSubExpression(TokensParser.InvestmentSizeRatingSubExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#regionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegionExpression(TokensParser.RegionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#incomeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncomeExpression(TokensParser.IncomeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#purposeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPurposeExpression(TokensParser.PurposeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#healthExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHealthExpression(TokensParser.HealthExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#dateExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDateExpr(TokensParser.DateExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#floatExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatExpr(TokensParser.FloatExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#intExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntExpr(TokensParser.IntExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link TokensParser#longExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLongExpr(TokensParser.LongExprContext ctx);
}

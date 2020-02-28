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

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TokensParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.8", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, REGION=5, REGION_A=6, REGION_B=7, REGION_C=8, 
		REGION_E=9, REGION_H=10, REGION_J=11, REGION_K=12, REGION_L=13, REGION_M=14, 
		REGION_P=15, REGION_S=16, REGION_T=17, REGION_U=18, REGION_Z=19, INCOME=20, 
		INCOME_EMPLOYMENT=21, INCOME_ENTREPRENEUR=22, INCOME_LIBERAL_PROFESSION=23, 
		INCOME_MATERNITY_LEAVE=24, INCOME_PENSION=25, INCOME_SELF_EMPLOYMENT=26, 
		INCOME_STUDENT=27, INCOME_UNEMPLOYED=28, PURPOSE=29, PURPOSE_AUTO_MOTO=30, 
		PURPOSE_TRAVEL=31, PURPOSE_HOUSEHOLD=32, PURPOSE_ELECTRONICS=33, PURPOSE_REFINANCING=34, 
		PURPOSE_OWN_PROJECT=35, PURPOSE_EDUCATION=36, PURPOSE_HEALTH=37, HEALTH=38, 
		HEALTH_ALWAYS=39, HEALTH_NOW=40, HEALTH_NOT=41, KC=42, DOT=43, DELIM=44, 
		UP_TO=45, IS=46, OR=47, OR_COMMA=48, LESS_THAN=49, MORE_THAN=50, OTHER=51, 
		MONTHS=52, INTEGER=53, FLOAT=54, COMMENT=55, NEWLINE=56, WHITESPACE=57;
	public static final int
		RULE_targetPortfolioSizeExpression = 0, RULE_interestCondition = 1, RULE_interestConditionRangeOpen = 2, 
		RULE_interestConditionRangeClosedLeft = 3, RULE_interestConditionRangeClosedRight = 4, 
		RULE_interestEnumeratedExpression = 5, RULE_interestRateBasedRatingExpression = 6, 
		RULE_investmentSizeRatingSubExpression = 7, RULE_regionExpression = 8, 
		RULE_incomeExpression = 9, RULE_purposeExpression = 10, RULE_healthExpression = 11, 
		RULE_dateExpr = 12, RULE_floatExpr = 13, RULE_intExpr = 14, RULE_longExpr = 15;
	private static String[] makeRuleNames() {
		return new String[] {
			"targetPortfolioSizeExpression", "interestCondition", "interestConditionRangeOpen", 
			"interestConditionRangeClosedLeft", "interestConditionRangeClosedRight", 
			"interestEnumeratedExpression", "interestRateBasedRatingExpression", 
			"investmentSizeRatingSubExpression", "regionExpression", "incomeExpression", 
			"purposeExpression", "healthExpression", "dateExpr", "floatExpr", "intExpr", 
			"longExpr"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'C\u00EDlov\u00E1 z\u016Fstatkov\u00E1 \u010D\u00E1stka je '", 
			"'\u00FArok '", "' % p.a'", "'a\u017E'", null, "'Praha'", "'Jihomoravsk\u00FD'", 
			"'Jiho\u010Desk\u00FD'", "'Pardubick\u00FD'", "'Kr\u00E1lov\u00E9hradeck\u00FD'", 
			"'Vyso\u010Dina'", "'Karlovarsk\u00FD'", "'Libereck\u00FD'", "'Olomouck\u00FD'", 
			"'Plze\u0148sk\u00FD'", "'St\u0159edo\u010Desk\u00FD'", "'Moravskoslezsk\u00FD'", 
			"'\u00DAsteck\u00FD'", "'Zl\u00EDnsk\u00FD'", null, "'zam\u011Bstnanec'", 
			"'podnikatel'", "'svobodn\u00E9 povol\u00E1n\u00ED'", "'na rodi\u010Dovsk\u00E9 dovolen\u00E9'", 
			"'d\u016Fchodce'", "'OSV\u010C'", "'student'", "'bez zam\u011Bstn\u00E1n\u00ED'", 
			null, "'auto-moto'", "'cestov\u00E1n\u00ED'", "'dom\u00E1cnost'", "'elektronika'", 
			"'refinancov\u00E1n\u00ED p\u016Fj\u010Dek'", "'vlastn\u00ED projekt'", 
			"'vzd\u011Bl\u00E1n\u00ED'", "'zdrav\u00ED'", null, "'nikdy nebyla'", 
			"'nyn\u00ED nen\u00ED'", "'nyn\u00ED je'", "' K\u010D'", "'.'", "'- '", 
			"' a\u017E '", "'je '", "' nebo '", null, "'nedosahuje '", "'p\u0159esahuje '", 
			"'jin\u00E9'", "' m\u011Bs\u00EDc\u016F'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, "REGION", "REGION_A", "REGION_B", "REGION_C", 
			"REGION_E", "REGION_H", "REGION_J", "REGION_K", "REGION_L", "REGION_M", 
			"REGION_P", "REGION_S", "REGION_T", "REGION_U", "REGION_Z", "INCOME", 
			"INCOME_EMPLOYMENT", "INCOME_ENTREPRENEUR", "INCOME_LIBERAL_PROFESSION", 
			"INCOME_MATERNITY_LEAVE", "INCOME_PENSION", "INCOME_SELF_EMPLOYMENT", 
			"INCOME_STUDENT", "INCOME_UNEMPLOYED", "PURPOSE", "PURPOSE_AUTO_MOTO", 
			"PURPOSE_TRAVEL", "PURPOSE_HOUSEHOLD", "PURPOSE_ELECTRONICS", "PURPOSE_REFINANCING", 
			"PURPOSE_OWN_PROJECT", "PURPOSE_EDUCATION", "PURPOSE_HEALTH", "HEALTH", 
			"HEALTH_ALWAYS", "HEALTH_NOW", "HEALTH_NOT", "KC", "DOT", "DELIM", "UP_TO", 
			"IS", "OR", "OR_COMMA", "LESS_THAN", "MORE_THAN", "OTHER", "MONTHS", 
			"INTEGER", "FLOAT", "COMMENT", "NEWLINE", "WHITESPACE"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Tokens.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TokensParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class TargetPortfolioSizeExpressionContext extends ParserRuleContext {
		public long result;
		public LongExprContext maximumInvestmentInCzk;
		public TerminalNode KC() { return getToken(TokensParser.KC, 0); }
		public TerminalNode DOT() { return getToken(TokensParser.DOT, 0); }
		public LongExprContext longExpr() {
			return getRuleContext(LongExprContext.class,0);
		}
		public TargetPortfolioSizeExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_targetPortfolioSizeExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterTargetPortfolioSizeExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitTargetPortfolioSizeExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitTargetPortfolioSizeExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TargetPortfolioSizeExpressionContext targetPortfolioSizeExpression() throws RecognitionException {
		TargetPortfolioSizeExpressionContext _localctx = new TargetPortfolioSizeExpressionContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_targetPortfolioSizeExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(32);
			match(T__0);
			setState(33);
			((TargetPortfolioSizeExpressionContext)_localctx).maximumInvestmentInCzk = longExpr();
			setState(34);
			match(KC);
			setState(35);
			match(DOT);
			((TargetPortfolioSizeExpressionContext)_localctx).result =  ((TargetPortfolioSizeExpressionContext)_localctx).maximumInvestmentInCzk.result;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestConditionContext extends ParserRuleContext {
		public MarketplaceFilterCondition result;
		public InterestConditionRangeOpenContext c1;
		public InterestConditionRangeClosedLeftContext c2;
		public InterestConditionRangeClosedRightContext c3;
		public TerminalNode DOT() { return getToken(TokensParser.DOT, 0); }
		public InterestConditionRangeOpenContext interestConditionRangeOpen() {
			return getRuleContext(InterestConditionRangeOpenContext.class,0);
		}
		public InterestConditionRangeClosedLeftContext interestConditionRangeClosedLeft() {
			return getRuleContext(InterestConditionRangeClosedLeftContext.class,0);
		}
		public InterestConditionRangeClosedRightContext interestConditionRangeClosedRight() {
			return getRuleContext(InterestConditionRangeClosedRightContext.class,0);
		}
		public InterestConditionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestCondition; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestCondition(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestCondition(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestCondition(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestConditionContext interestCondition() throws RecognitionException {
		InterestConditionContext _localctx = new InterestConditionContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_interestCondition);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(38);
			match(T__1);
			setState(48);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IS:
				{
				{
				setState(39);
				((InterestConditionContext)_localctx).c1 = interestConditionRangeOpen();
				 ((InterestConditionContext)_localctx).result =  ((InterestConditionContext)_localctx).c1.result; 
				}
				}
				break;
			case MORE_THAN:
				{
				{
				setState(42);
				((InterestConditionContext)_localctx).c2 = interestConditionRangeClosedLeft();
				 ((InterestConditionContext)_localctx).result =  ((InterestConditionContext)_localctx).c2.result; 
				}
				}
				break;
			case LESS_THAN:
				{
				{
				setState(45);
				((InterestConditionContext)_localctx).c3 = interestConditionRangeClosedRight();
				 ((InterestConditionContext)_localctx).result =  ((InterestConditionContext)_localctx).c3.result; 
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(50);
			match(T__2);
			setState(52);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(51);
				match(DOT);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestConditionRangeOpenContext extends ParserRuleContext {
		public MarketplaceFilterCondition result;
		public FloatExprContext min;
		public FloatExprContext max;
		public TerminalNode IS() { return getToken(TokensParser.IS, 0); }
		public List<FloatExprContext> floatExpr() {
			return getRuleContexts(FloatExprContext.class);
		}
		public FloatExprContext floatExpr(int i) {
			return getRuleContext(FloatExprContext.class,i);
		}
		public TerminalNode UP_TO() { return getToken(TokensParser.UP_TO, 0); }
		public InterestConditionRangeOpenContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestConditionRangeOpen; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestConditionRangeOpen(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestConditionRangeOpen(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestConditionRangeOpen(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestConditionRangeOpenContext interestConditionRangeOpen() throws RecognitionException {
		InterestConditionRangeOpenContext _localctx = new InterestConditionRangeOpenContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_interestConditionRangeOpen);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			 boolean needsBothValues = false; 
			setState(55);
			match(IS);
			setState(56);
			((InterestConditionRangeOpenContext)_localctx).min = floatExpr();
			setState(61);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==UP_TO) {
				{
				setState(57);
				match(UP_TO);
				setState(58);
				((InterestConditionRangeOpenContext)_localctx).max = floatExpr();
				 needsBothValues = true; 
				}
			}


			        if (needsBothValues) {
			            // if the second one is provided, use the range
			            ((InterestConditionRangeOpenContext)_localctx).result =  LoanInterestRateCondition.exact(Ratio.fromPercentage(((InterestConditionRangeOpenContext)_localctx).min.result),
			                                                      Ratio.fromPercentage(((InterestConditionRangeOpenContext)_localctx).max.result));
			        } else {
			            // by default, just pick the one rating
			            final Rating r = Rating.findByCode(((InterestConditionRangeOpenContext)_localctx).min.result.toString());
			            LoanRatingEnumeratedCondition c = new LoanRatingEnumeratedCondition();
			            c.add(r);
			            ((InterestConditionRangeOpenContext)_localctx).result =  c;
			        }
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestConditionRangeClosedLeftContext extends ParserRuleContext {
		public MarketplaceFilterCondition result;
		public FloatExprContext min;
		public TerminalNode MORE_THAN() { return getToken(TokensParser.MORE_THAN, 0); }
		public FloatExprContext floatExpr() {
			return getRuleContext(FloatExprContext.class,0);
		}
		public InterestConditionRangeClosedLeftContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestConditionRangeClosedLeft; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestConditionRangeClosedLeft(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestConditionRangeClosedLeft(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestConditionRangeClosedLeft(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestConditionRangeClosedLeftContext interestConditionRangeClosedLeft() throws RecognitionException {
		InterestConditionRangeClosedLeftContext _localctx = new InterestConditionRangeClosedLeftContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_interestConditionRangeClosedLeft);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(65);
			match(MORE_THAN);
			setState(66);
			((InterestConditionRangeClosedLeftContext)_localctx).min = floatExpr();

			        ((InterestConditionRangeClosedLeftContext)_localctx).result =  LoanInterestRateCondition.moreThan(Ratio.fromPercentage(((InterestConditionRangeClosedLeftContext)_localctx).min.result));
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestConditionRangeClosedRightContext extends ParserRuleContext {
		public MarketplaceFilterCondition result;
		public FloatExprContext max;
		public TerminalNode LESS_THAN() { return getToken(TokensParser.LESS_THAN, 0); }
		public FloatExprContext floatExpr() {
			return getRuleContext(FloatExprContext.class,0);
		}
		public InterestConditionRangeClosedRightContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestConditionRangeClosedRight; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestConditionRangeClosedRight(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestConditionRangeClosedRight(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestConditionRangeClosedRight(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestConditionRangeClosedRightContext interestConditionRangeClosedRight() throws RecognitionException {
		InterestConditionRangeClosedRightContext _localctx = new InterestConditionRangeClosedRightContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_interestConditionRangeClosedRight);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(69);
			match(LESS_THAN);
			setState(70);
			((InterestConditionRangeClosedRightContext)_localctx).max = floatExpr();

			        ((InterestConditionRangeClosedRightContext)_localctx).result =  LoanInterestRateCondition.lessThan(Ratio.fromPercentage(((InterestConditionRangeClosedRightContext)_localctx).max.result));
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestEnumeratedExpressionContext extends ParserRuleContext {
		public Collection<Rating> result;
		public InterestRateBasedRatingExpressionContext r1;
		public InterestRateBasedRatingExpressionContext r2;
		public InterestRateBasedRatingExpressionContext r3;
		public List<InterestRateBasedRatingExpressionContext> interestRateBasedRatingExpression() {
			return getRuleContexts(InterestRateBasedRatingExpressionContext.class);
		}
		public InterestRateBasedRatingExpressionContext interestRateBasedRatingExpression(int i) {
			return getRuleContext(InterestRateBasedRatingExpressionContext.class,i);
		}
		public TerminalNode OR() { return getToken(TokensParser.OR, 0); }
		public List<TerminalNode> OR_COMMA() { return getTokens(TokensParser.OR_COMMA); }
		public TerminalNode OR_COMMA(int i) {
			return getToken(TokensParser.OR_COMMA, i);
		}
		public InterestEnumeratedExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestEnumeratedExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestEnumeratedExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestEnumeratedExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestEnumeratedExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestEnumeratedExpressionContext interestEnumeratedExpression() throws RecognitionException {
		InterestEnumeratedExpressionContext _localctx = new InterestEnumeratedExpressionContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_interestEnumeratedExpression);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			 ((InterestEnumeratedExpressionContext)_localctx).result =  new LinkedHashSet<Rating>(); 
			setState(87);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				{
				setState(80);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(74);
						((InterestEnumeratedExpressionContext)_localctx).r1 = interestRateBasedRatingExpression();
						setState(75);
						match(OR_COMMA);
						 _localctx.result.add(((InterestEnumeratedExpressionContext)_localctx).r1.result); 
						}
						} 
					}
					setState(82);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
				}
				setState(83);
				((InterestEnumeratedExpressionContext)_localctx).r2 = interestRateBasedRatingExpression();
				setState(84);
				match(OR);
				 _localctx.result.add(((InterestEnumeratedExpressionContext)_localctx).r2.result); 
				}
				break;
			}
			setState(89);
			((InterestEnumeratedExpressionContext)_localctx).r3 = interestRateBasedRatingExpression();
			 _localctx.result.add(((InterestEnumeratedExpressionContext)_localctx).r3.result); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InterestRateBasedRatingExpressionContext extends ParserRuleContext {
		public Rating result;
		public FloatExprContext r;
		public FloatExprContext floatExpr() {
			return getRuleContext(FloatExprContext.class,0);
		}
		public TerminalNode DOT() { return getToken(TokensParser.DOT, 0); }
		public InterestRateBasedRatingExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interestRateBasedRatingExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInterestRateBasedRatingExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInterestRateBasedRatingExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInterestRateBasedRatingExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InterestRateBasedRatingExpressionContext interestRateBasedRatingExpression() throws RecognitionException {
		InterestRateBasedRatingExpressionContext _localctx = new InterestRateBasedRatingExpressionContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_interestRateBasedRatingExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(92);
			((InterestRateBasedRatingExpressionContext)_localctx).r = floatExpr();
			setState(93);
			match(T__2);
			setState(95);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==DOT) {
				{
				setState(94);
				match(DOT);
				}
			}

			 ((InterestRateBasedRatingExpressionContext)_localctx).result =  Rating.findByCode(((InterestRateBasedRatingExpressionContext)_localctx).r.result.toString()); 
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class InvestmentSizeRatingSubExpressionContext extends ParserRuleContext {
		public MoneyRange result;
		public IntExprContext amount;
		public IntExprContext max;
		public IntExprContext min;
		public TerminalNode KC() { return getToken(TokensParser.KC, 0); }
		public TerminalNode DOT() { return getToken(TokensParser.DOT, 0); }
		public TerminalNode UP_TO() { return getToken(TokensParser.UP_TO, 0); }
		public List<IntExprContext> intExpr() {
			return getRuleContexts(IntExprContext.class);
		}
		public IntExprContext intExpr(int i) {
			return getRuleContext(IntExprContext.class,i);
		}
		public InvestmentSizeRatingSubExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_investmentSizeRatingSubExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterInvestmentSizeRatingSubExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitInvestmentSizeRatingSubExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitInvestmentSizeRatingSubExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InvestmentSizeRatingSubExpressionContext investmentSizeRatingSubExpression() throws RecognitionException {
		InvestmentSizeRatingSubExpressionContext _localctx = new InvestmentSizeRatingSubExpressionContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_investmentSizeRatingSubExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(111);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
			case 1:
				{
				{
				setState(99);
				((InvestmentSizeRatingSubExpressionContext)_localctx).amount = intExpr();
				 ((InvestmentSizeRatingSubExpressionContext)_localctx).result =  new MoneyRange(((InvestmentSizeRatingSubExpressionContext)_localctx).amount.result, ((InvestmentSizeRatingSubExpressionContext)_localctx).amount.result); 
				}
				}
				break;
			case 2:
				{
				{
				setState(102);
				match(T__3);
				setState(103);
				((InvestmentSizeRatingSubExpressionContext)_localctx).max = intExpr();
				 ((InvestmentSizeRatingSubExpressionContext)_localctx).result =  new MoneyRange(((InvestmentSizeRatingSubExpressionContext)_localctx).max.result); 
				}
				}
				break;
			case 3:
				{
				{
				setState(106);
				((InvestmentSizeRatingSubExpressionContext)_localctx).min = intExpr();
				setState(107);
				match(UP_TO);
				setState(108);
				((InvestmentSizeRatingSubExpressionContext)_localctx).max = intExpr();
				 ((InvestmentSizeRatingSubExpressionContext)_localctx).result =  new MoneyRange(((InvestmentSizeRatingSubExpressionContext)_localctx).min.result, ((InvestmentSizeRatingSubExpressionContext)_localctx).max.result); 
				}
				}
				break;
			}
			setState(113);
			match(KC);
			setState(114);
			match(DOT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RegionExpressionContext extends ParserRuleContext {
		public Region result;
		public Token r;
		public TerminalNode REGION() { return getToken(TokensParser.REGION, 0); }
		public RegionExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_regionExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterRegionExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitRegionExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitRegionExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RegionExpressionContext regionExpression() throws RecognitionException {
		RegionExpressionContext _localctx = new RegionExpressionContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_regionExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(116);
			((RegionExpressionContext)_localctx).r = match(REGION);

			        ((RegionExpressionContext)_localctx).result =  Region.findByCode(((RegionExpressionContext)_localctx).r.getText());
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IncomeExpressionContext extends ParserRuleContext {
		public MainIncomeType result;
		public Token r;
		public TerminalNode INCOME() { return getToken(TokensParser.INCOME, 0); }
		public TerminalNode OTHER() { return getToken(TokensParser.OTHER, 0); }
		public IncomeExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_incomeExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterIncomeExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitIncomeExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitIncomeExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IncomeExpressionContext incomeExpression() throws RecognitionException {
		IncomeExpressionContext _localctx = new IncomeExpressionContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_incomeExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(119);
			((IncomeExpressionContext)_localctx).r = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==INCOME || _la==OTHER) ) {
				((IncomeExpressionContext)_localctx).r = (Token)_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}

			        ((IncomeExpressionContext)_localctx).result =  MainIncomeType.findByCode(((IncomeExpressionContext)_localctx).r.getText());
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PurposeExpressionContext extends ParserRuleContext {
		public Purpose result;
		public Token r;
		public TerminalNode PURPOSE() { return getToken(TokensParser.PURPOSE, 0); }
		public TerminalNode OTHER() { return getToken(TokensParser.OTHER, 0); }
		public PurposeExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_purposeExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterPurposeExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitPurposeExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitPurposeExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PurposeExpressionContext purposeExpression() throws RecognitionException {
		PurposeExpressionContext _localctx = new PurposeExpressionContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_purposeExpression);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(122);
			((PurposeExpressionContext)_localctx).r = _input.LT(1);
			_la = _input.LA(1);
			if ( !(_la==PURPOSE || _la==OTHER) ) {
				((PurposeExpressionContext)_localctx).r = (Token)_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}

			        ((PurposeExpressionContext)_localctx).result =  Purpose.findByCode(((PurposeExpressionContext)_localctx).r.getText());
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HealthExpressionContext extends ParserRuleContext {
		public LoanHealth result;
		public Token r;
		public TerminalNode HEALTH() { return getToken(TokensParser.HEALTH, 0); }
		public HealthExpressionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_healthExpression; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterHealthExpression(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitHealthExpression(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitHealthExpression(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HealthExpressionContext healthExpression() throws RecognitionException {
		HealthExpressionContext _localctx = new HealthExpressionContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_healthExpression);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(125);
			((HealthExpressionContext)_localctx).r = match(HEALTH);

			        String text = ((HealthExpressionContext)_localctx).r.getText();
			        switch (_input.getText(_localctx.start, _input.LT(-1))) {
			            case "nikdy nebyla":
			                ((HealthExpressionContext)_localctx).result =  LoanHealth.HEALTHY;
			                break;
			            case "nyní je":
			                ((HealthExpressionContext)_localctx).result =  LoanHealth.CURRENTLY_IN_DUE;
			                break;
			            case "nyní není":
			                ((HealthExpressionContext)_localctx).result =  LoanHealth.HISTORICALLY_IN_DUE;
			                break;
			            default:
			                throw new IllegalStateException("Unknown loan health: " + _input.getText(_localctx.start, _input.LT(-1)));
			        }
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DateExprContext extends ParserRuleContext {
		public LocalDate result;
		public IntExprContext d;
		public IntExprContext m;
		public IntExprContext y;
		public List<TerminalNode> DOT() { return getTokens(TokensParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(TokensParser.DOT, i);
		}
		public List<IntExprContext> intExpr() {
			return getRuleContexts(IntExprContext.class);
		}
		public IntExprContext intExpr(int i) {
			return getRuleContext(IntExprContext.class,i);
		}
		public DateExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_dateExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterDateExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitDateExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitDateExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DateExprContext dateExpr() throws RecognitionException {
		DateExprContext _localctx = new DateExprContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_dateExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(128);
			((DateExprContext)_localctx).d = intExpr();
			setState(129);
			match(DOT);
			setState(130);
			((DateExprContext)_localctx).m = intExpr();
			setState(131);
			match(DOT);
			setState(132);
			((DateExprContext)_localctx).y = intExpr();

			        ((DateExprContext)_localctx).result =  LocalDate.of(((DateExprContext)_localctx).y.result, ((DateExprContext)_localctx).m.result, ((DateExprContext)_localctx).d.result);
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FloatExprContext extends ParserRuleContext {
		public BigDecimal result;
		public Token f;
		public IntExprContext i;
		public TerminalNode FLOAT() { return getToken(TokensParser.FLOAT, 0); }
		public IntExprContext intExpr() {
			return getRuleContext(IntExprContext.class,0);
		}
		public FloatExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_floatExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterFloatExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitFloatExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitFloatExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FloatExprContext floatExpr() throws RecognitionException {
		FloatExprContext _localctx = new FloatExprContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_floatExpr);
		try {
			setState(140);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case FLOAT:
				enterOuterAlt(_localctx, 1);
				{
				setState(135);
				((FloatExprContext)_localctx).f = match(FLOAT);

				        final String replaced = ((FloatExprContext)_localctx).f.getText().replaceFirst("\\Q,\\E", ".");
				        ((FloatExprContext)_localctx).result =  new BigDecimal(replaced);
				    
				}
				break;
			case INTEGER:
				enterOuterAlt(_localctx, 2);
				{
				setState(137);
				((FloatExprContext)_localctx).i = intExpr();

				        ((FloatExprContext)_localctx).result =  new BigDecimal(((FloatExprContext)_localctx).i.result);
				    
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IntExprContext extends ParserRuleContext {
		public int result;
		public Token i;
		public TerminalNode INTEGER() { return getToken(TokensParser.INTEGER, 0); }
		public IntExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_intExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterIntExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitIntExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitIntExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IntExprContext intExpr() throws RecognitionException {
		IntExprContext _localctx = new IntExprContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_intExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(142);
			((IntExprContext)_localctx).i = match(INTEGER);

			        ((IntExprContext)_localctx).result =  Integer.parseInt(((IntExprContext)_localctx).i.getText());
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LongExprContext extends ParserRuleContext {
		public long result;
		public Token i;
		public TerminalNode INTEGER() { return getToken(TokensParser.INTEGER, 0); }
		public LongExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_longExpr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).enterLongExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TokensListener ) ((TokensListener)listener).exitLongExpr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TokensVisitor ) return ((TokensVisitor<? extends T>)visitor).visitLongExpr(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LongExprContext longExpr() throws RecognitionException {
		LongExprContext _localctx = new LongExprContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_longExpr);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(145);
			((LongExprContext)_localctx).i = match(INTEGER);

			        ((LongExprContext)_localctx).result =  Long.parseLong(((LongExprContext)_localctx).i.getText());
			    
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3;\u0097\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\3\2\3\2\3"+
		"\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\63\n\3\3\3"+
		"\3\3\5\3\67\n\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4@\n\4\3\4\3\4\3\5\3\5\3"+
		"\5\3\5\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7\3\7\7\7Q\n\7\f\7\16\7T\13\7\3\7"+
		"\3\7\3\7\3\7\5\7Z\n\7\3\7\3\7\3\7\3\b\3\b\3\b\5\bb\n\b\3\b\3\b\3\t\3\t"+
		"\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3\t\5\tr\n\t\3\t\3\t\3\t\3\n\3\n"+
		"\3\n\3\13\3\13\3\13\3\f\3\f\3\f\3\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3"+
		"\16\3\16\3\17\3\17\3\17\3\17\3\17\5\17\u008f\n\17\3\20\3\20\3\20\3\21"+
		"\3\21\3\21\3\21\2\2\22\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \2\4\4\2"+
		"\26\26\65\65\4\2\37\37\65\65\2\u0090\2\"\3\2\2\2\4(\3\2\2\2\68\3\2\2\2"+
		"\bC\3\2\2\2\nG\3\2\2\2\fK\3\2\2\2\16^\3\2\2\2\20q\3\2\2\2\22v\3\2\2\2"+
		"\24y\3\2\2\2\26|\3\2\2\2\30\177\3\2\2\2\32\u0082\3\2\2\2\34\u008e\3\2"+
		"\2\2\36\u0090\3\2\2\2 \u0093\3\2\2\2\"#\7\3\2\2#$\5 \21\2$%\7,\2\2%&\7"+
		"-\2\2&\'\b\2\1\2\'\3\3\2\2\2(\62\7\4\2\2)*\5\6\4\2*+\b\3\1\2+\63\3\2\2"+
		"\2,-\5\b\5\2-.\b\3\1\2.\63\3\2\2\2/\60\5\n\6\2\60\61\b\3\1\2\61\63\3\2"+
		"\2\2\62)\3\2\2\2\62,\3\2\2\2\62/\3\2\2\2\63\64\3\2\2\2\64\66\7\5\2\2\65"+
		"\67\7-\2\2\66\65\3\2\2\2\66\67\3\2\2\2\67\5\3\2\2\289\b\4\1\29:\7\60\2"+
		"\2:?\5\34\17\2;<\7/\2\2<=\5\34\17\2=>\b\4\1\2>@\3\2\2\2?;\3\2\2\2?@\3"+
		"\2\2\2@A\3\2\2\2AB\b\4\1\2B\7\3\2\2\2CD\7\64\2\2DE\5\34\17\2EF\b\5\1\2"+
		"F\t\3\2\2\2GH\7\63\2\2HI\5\34\17\2IJ\b\6\1\2J\13\3\2\2\2KY\b\7\1\2LM\5"+
		"\16\b\2MN\7\62\2\2NO\b\7\1\2OQ\3\2\2\2PL\3\2\2\2QT\3\2\2\2RP\3\2\2\2R"+
		"S\3\2\2\2SU\3\2\2\2TR\3\2\2\2UV\5\16\b\2VW\7\61\2\2WX\b\7\1\2XZ\3\2\2"+
		"\2YR\3\2\2\2YZ\3\2\2\2Z[\3\2\2\2[\\\5\16\b\2\\]\b\7\1\2]\r\3\2\2\2^_\5"+
		"\34\17\2_a\7\5\2\2`b\7-\2\2a`\3\2\2\2ab\3\2\2\2bc\3\2\2\2cd\b\b\1\2d\17"+
		"\3\2\2\2ef\5\36\20\2fg\b\t\1\2gr\3\2\2\2hi\7\6\2\2ij\5\36\20\2jk\b\t\1"+
		"\2kr\3\2\2\2lm\5\36\20\2mn\7/\2\2no\5\36\20\2op\b\t\1\2pr\3\2\2\2qe\3"+
		"\2\2\2qh\3\2\2\2ql\3\2\2\2rs\3\2\2\2st\7,\2\2tu\7-\2\2u\21\3\2\2\2vw\7"+
		"\7\2\2wx\b\n\1\2x\23\3\2\2\2yz\t\2\2\2z{\b\13\1\2{\25\3\2\2\2|}\t\3\2"+
		"\2}~\b\f\1\2~\27\3\2\2\2\177\u0080\7(\2\2\u0080\u0081\b\r\1\2\u0081\31"+
		"\3\2\2\2\u0082\u0083\5\36\20\2\u0083\u0084\7-\2\2\u0084\u0085\5\36\20"+
		"\2\u0085\u0086\7-\2\2\u0086\u0087\5\36\20\2\u0087\u0088\b\16\1\2\u0088"+
		"\33\3\2\2\2\u0089\u008a\78\2\2\u008a\u008f\b\17\1\2\u008b\u008c\5\36\20"+
		"\2\u008c\u008d\b\17\1\2\u008d\u008f\3\2\2\2\u008e\u0089\3\2\2\2\u008e"+
		"\u008b\3\2\2\2\u008f\35\3\2\2\2\u0090\u0091\7\67\2\2\u0091\u0092\b\20"+
		"\1\2\u0092\37\3\2\2\2\u0093\u0094\7\67\2\2\u0094\u0095\b\21\1\2\u0095"+
		"!\3\2\2\2\n\62\66?RYaq\u008e";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}

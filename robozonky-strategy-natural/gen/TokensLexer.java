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

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TokensLexer extends Lexer {
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
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "REGION", "REGION_A", "REGION_B", "REGION_C", 
			"REGION_E", "REGION_H", "REGION_J", "REGION_K", "REGION_L", "REGION_M", 
			"REGION_P", "REGION_S", "REGION_T", "REGION_U", "REGION_Z", "INCOME", 
			"INCOME_EMPLOYMENT", "INCOME_ENTREPRENEUR", "INCOME_LIBERAL_PROFESSION", 
			"INCOME_MATERNITY_LEAVE", "INCOME_PENSION", "INCOME_SELF_EMPLOYMENT", 
			"INCOME_STUDENT", "INCOME_UNEMPLOYED", "PURPOSE", "PURPOSE_AUTO_MOTO", 
			"PURPOSE_TRAVEL", "PURPOSE_HOUSEHOLD", "PURPOSE_ELECTRONICS", "PURPOSE_REFINANCING", 
			"PURPOSE_OWN_PROJECT", "PURPOSE_EDUCATION", "PURPOSE_HEALTH", "HEALTH", 
			"HEALTH_ALWAYS", "HEALTH_NOW", "HEALTH_NOT", "KC", "DOT", "DELIM", "UP_TO", 
			"IS", "OR", "OR_COMMA", "LESS_THAN", "MORE_THAN", "OTHER", "MONTHS", 
			"INTEGER", "FLOAT", "COMMENT", "NEWLINE", "WHITESPACE", "DIGIT", "COMMA"
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


	public TokensLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "Tokens.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2;\u02b5\b\1\4\2\t"+
		"\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
		"\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\4%\t%\4&\t&\4\'\t\'\4(\t(\4)\t)\4*\t*\4+\t+\4"+
		",\t,\4-\t-\4.\t.\4/\t/\4\60\t\60\4\61\t\61\4\62\t\62\4\63\t\63\4\64\t"+
		"\64\4\65\t\65\4\66\t\66\4\67\t\67\48\t8\49\t9\4:\t:\4;\t;\4<\t<\3\2\3"+
		"\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2"+
		"\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\4\3"+
		"\4\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\5\6\u00b5\n\6\3\7\3\7\3\7\3\7\3\7\3\7\3\b\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\t\3\t\3"+
		"\t\3\t\3\t\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\n\3\13\3\13\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f"+
		"\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3\r\3"+
		"\r\3\r\3\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\17\3\17"+
		"\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3\20\3\20"+
		"\3\20\3\20\3\20\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21\3\21"+
		"\3\21\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22\3\22"+
		"\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\23\3\24\3\24\3\24"+
		"\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25\3\25\3\25\3\25\3\25\3\25\5\25"+
		"\u0155\n\25\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26\3\26"+
		"\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\27\3\30\3\30\3\30"+
		"\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
		"\3\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
		"\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\32\3\32\3\32\3\32"+
		"\3\32\3\32\3\32\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34"+
		"\3\34\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\35\3\35\3\35\3\35\3\36\3\36\3\36\3\36\3\36\3\36\3\36\3\36\5\36"+
		"\u01c4\n\36\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3\37\3 \3 \3"+
		" \3 \3 \3 \3 \3 \3 \3 \3!\3!\3!\3!\3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\3"+
		"\"\3\"\3\"\3\"\3\"\3\"\3\"\3\"\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#\3#"+
		"\3#\3#\3#\3#\3#\3#\3#\3#\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$"+
		"\3$\3%\3%\3%\3%\3%\3%\3%\3%\3%\3&\3&\3&\3&\3&\3&\3&\3\'\3\'\3\'\5\'\u0228"+
		"\n\'\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3(\3)\3)\3)\3)\3)\3)\3)\3)\3"+
		")\3)\3*\3*\3*\3*\3*\3*\3*\3*\3+\3+\3+\3+\3,\3,\3-\3-\3-\3.\3.\3.\3.\3"+
		".\3/\3/\3/\3/\3\60\3\60\3\60\3\60\3\60\3\60\3\60\3\61\3\61\3\61\3\62\3"+
		"\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\62\3\63\3\63\3\63\3"+
		"\63\3\63\3\63\3\63\3\63\3\63\3\63\3\63\3\64\3\64\3\64\3\64\3\64\3\65\3"+
		"\65\3\65\3\65\3\65\3\65\3\65\3\65\3\66\6\66\u028a\n\66\r\66\16\66\u028b"+
		"\3\67\6\67\u028f\n\67\r\67\16\67\u0290\3\67\3\67\6\67\u0295\n\67\r\67"+
		"\16\67\u0296\38\38\78\u029b\n8\f8\168\u029e\138\38\38\38\38\39\39\39\5"+
		"9\u02a7\n9\39\39\3:\6:\u02ac\n:\r:\16:\u02ad\3:\3:\3;\3;\3<\3<\2\2=\3"+
		"\3\5\4\7\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37"+
		"\21!\22#\23%\24\'\25)\26+\27-\30/\31\61\32\63\33\65\34\67\359\36;\37="+
		" ?!A\"C#E$G%I&K\'M(O)Q*S+U,W-Y.[/]\60_\61a\62c\63e\64g\65i\66k\67m8o9"+
		"q:s;u\2w\2\3\2\5\4\2\f\f\17\17\4\2\13\13\"\"\3\2\62;\2\u02d5\2\3\3\2\2"+
		"\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3"+
		"\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2"+
		"\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2"+
		"\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\61\3\2"+
		"\2\2\2\63\3\2\2\2\2\65\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2\2;\3\2\2\2\2=\3"+
		"\2\2\2\2?\3\2\2\2\2A\3\2\2\2\2C\3\2\2\2\2E\3\2\2\2\2G\3\2\2\2\2I\3\2\2"+
		"\2\2K\3\2\2\2\2M\3\2\2\2\2O\3\2\2\2\2Q\3\2\2\2\2S\3\2\2\2\2U\3\2\2\2\2"+
		"W\3\2\2\2\2Y\3\2\2\2\2[\3\2\2\2\2]\3\2\2\2\2_\3\2\2\2\2a\3\2\2\2\2c\3"+
		"\2\2\2\2e\3\2\2\2\2g\3\2\2\2\2i\3\2\2\2\2k\3\2\2\2\2m\3\2\2\2\2o\3\2\2"+
		"\2\2q\3\2\2\2\2s\3\2\2\2\3y\3\2\2\2\5\u0096\3\2\2\2\7\u009c\3\2\2\2\t"+
		"\u00a3\3\2\2\2\13\u00b4\3\2\2\2\r\u00b6\3\2\2\2\17\u00bc\3\2\2\2\21\u00c9"+
		"\3\2\2\2\23\u00d3\3\2\2\2\25\u00de\3\2\2\2\27\u00ee\3\2\2\2\31\u00f7\3"+
		"\2\2\2\33\u0103\3\2\2\2\35\u010d\3\2\2\2\37\u0117\3\2\2\2!\u0120\3\2\2"+
		"\2#\u012c\3\2\2\2%\u013c\3\2\2\2\'\u0144\3\2\2\2)\u0154\3\2\2\2+\u0156"+
		"\3\2\2\2-\u0162\3\2\2\2/\u016d\3\2\2\2\61\u017f\3\2\2\2\63\u0196\3\2\2"+
		"\2\65\u019f\3\2\2\2\67\u01a4\3\2\2\29\u01ac\3\2\2\2;\u01c3\3\2\2\2=\u01c5"+
		"\3\2\2\2?\u01cf\3\2\2\2A\u01d9\3\2\2\2C\u01e3\3\2\2\2E\u01ef\3\2\2\2G"+
		"\u0204\3\2\2\2I\u0214\3\2\2\2K\u021d\3\2\2\2M\u0227\3\2\2\2O\u0229\3\2"+
		"\2\2Q\u0236\3\2\2\2S\u0240\3\2\2\2U\u0248\3\2\2\2W\u024c\3\2\2\2Y\u024e"+
		"\3\2\2\2[\u0251\3\2\2\2]\u0256\3\2\2\2_\u025a\3\2\2\2a\u0261\3\2\2\2c"+
		"\u0264\3\2\2\2e\u0270\3\2\2\2g\u027b\3\2\2\2i\u0280\3\2\2\2k\u0289\3\2"+
		"\2\2m\u028e\3\2\2\2o\u0298\3\2\2\2q\u02a6\3\2\2\2s\u02ab\3\2\2\2u\u02b1"+
		"\3\2\2\2w\u02b3\3\2\2\2yz\7E\2\2z{\7\u00ef\2\2{|\7n\2\2|}\7q\2\2}~\7x"+
		"\2\2~\177\7\u00e3\2\2\177\u0080\7\"\2\2\u0080\u0081\7|\2\2\u0081\u0082"+
		"\7\u0171\2\2\u0082\u0083\7u\2\2\u0083\u0084\7v\2\2\u0084\u0085\7c\2\2"+
		"\u0085\u0086\7v\2\2\u0086\u0087\7m\2\2\u0087\u0088\7q\2\2\u0088\u0089"+
		"\7x\2\2\u0089\u008a\7\u00e3\2\2\u008a\u008b\7\"\2\2\u008b\u008c\7\u010f"+
		"\2\2\u008c\u008d\7\u00e3\2\2\u008d\u008e\7u\2\2\u008e\u008f\7v\2\2\u008f"+
		"\u0090\7m\2\2\u0090\u0091\7c\2\2\u0091\u0092\7\"\2\2\u0092\u0093\7l\2"+
		"\2\u0093\u0094\7g\2\2\u0094\u0095\7\"\2\2\u0095\4\3\2\2\2\u0096\u0097"+
		"\7\u00fc\2\2\u0097\u0098\7t\2\2\u0098\u0099\7q\2\2\u0099\u009a\7m\2\2"+
		"\u009a\u009b\7\"\2\2\u009b\6\3\2\2\2\u009c\u009d\7\"\2\2\u009d\u009e\7"+
		"\'\2\2\u009e\u009f\7\"\2\2\u009f\u00a0\7r\2\2\u00a0\u00a1\7\60\2\2\u00a1"+
		"\u00a2\7c\2\2\u00a2\b\3\2\2\2\u00a3\u00a4\7c\2\2\u00a4\u00a5\7\u0180\2"+
		"\2\u00a5\n\3\2\2\2\u00a6\u00b5\5\r\7\2\u00a7\u00b5\5\17\b\2\u00a8\u00b5"+
		"\5\21\t\2\u00a9\u00b5\5\23\n\2\u00aa\u00b5\5\25\13\2\u00ab\u00b5\5\27"+
		"\f\2\u00ac\u00b5\5\31\r\2\u00ad\u00b5\5\33\16\2\u00ae\u00b5\5\35\17\2"+
		"\u00af\u00b5\5\37\20\2\u00b0\u00b5\5!\21\2\u00b1\u00b5\5#\22\2\u00b2\u00b5"+
		"\5%\23\2\u00b3\u00b5\5\'\24\2\u00b4\u00a6\3\2\2\2\u00b4\u00a7\3\2\2\2"+
		"\u00b4\u00a8\3\2\2\2\u00b4\u00a9\3\2\2\2\u00b4\u00aa\3\2\2\2\u00b4\u00ab"+
		"\3\2\2\2\u00b4\u00ac\3\2\2\2\u00b4\u00ad\3\2\2\2\u00b4\u00ae\3\2\2\2\u00b4"+
		"\u00af\3\2\2\2\u00b4\u00b0\3\2\2\2\u00b4\u00b1\3\2\2\2\u00b4\u00b2\3\2"+
		"\2\2\u00b4\u00b3\3\2\2\2\u00b5\f\3\2\2\2\u00b6\u00b7\7R\2\2\u00b7\u00b8"+
		"\7t\2\2\u00b8\u00b9\7c\2\2\u00b9\u00ba\7j\2\2\u00ba\u00bb\7c\2\2\u00bb"+
		"\16\3\2\2\2\u00bc\u00bd\7L\2\2\u00bd\u00be\7k\2\2\u00be\u00bf\7j\2\2\u00bf"+
		"\u00c0\7q\2\2\u00c0\u00c1\7o\2\2\u00c1\u00c2\7q\2\2\u00c2\u00c3\7t\2\2"+
		"\u00c3\u00c4\7c\2\2\u00c4\u00c5\7x\2\2\u00c5\u00c6\7u\2\2\u00c6\u00c7"+
		"\7m\2\2\u00c7\u00c8\7\u00ff\2\2\u00c8\20\3\2\2\2\u00c9\u00ca\7L\2\2\u00ca"+
		"\u00cb\7k\2\2\u00cb\u00cc\7j\2\2\u00cc\u00cd\7q\2\2\u00cd\u00ce\7\u010f"+
		"\2\2\u00ce\u00cf\7g\2\2\u00cf\u00d0\7u\2\2\u00d0\u00d1\7m\2\2\u00d1\u00d2"+
		"\7\u00ff\2\2\u00d2\22\3\2\2\2\u00d3\u00d4\7R\2\2\u00d4\u00d5\7c\2\2\u00d5"+
		"\u00d6\7t\2\2\u00d6\u00d7\7f\2\2\u00d7\u00d8\7w\2\2\u00d8\u00d9\7d\2\2"+
		"\u00d9\u00da\7k\2\2\u00da\u00db\7e\2\2\u00db\u00dc\7m\2\2\u00dc\u00dd"+
		"\7\u00ff\2\2\u00dd\24\3\2\2\2\u00de\u00df\7M\2\2\u00df\u00e0\7t\2\2\u00e0"+
		"\u00e1\7\u00e3\2\2\u00e1\u00e2\7n\2\2\u00e2\u00e3\7q\2\2\u00e3\u00e4\7"+
		"x\2\2\u00e4\u00e5\7\u00eb\2\2\u00e5\u00e6\7j\2\2\u00e6\u00e7\7t\2\2\u00e7"+
		"\u00e8\7c\2\2\u00e8\u00e9\7f\2\2\u00e9\u00ea\7g\2\2\u00ea\u00eb\7e\2\2"+
		"\u00eb\u00ec\7m\2\2\u00ec\u00ed\7\u00ff\2\2\u00ed\26\3\2\2\2\u00ee\u00ef"+
		"\7X\2\2\u00ef\u00f0\7{\2\2\u00f0\u00f1\7u\2\2\u00f1\u00f2\7q\2\2\u00f2"+
		"\u00f3\7\u010f\2\2\u00f3\u00f4\7k\2\2\u00f4\u00f5\7p\2\2\u00f5\u00f6\7"+
		"c\2\2\u00f6\30\3\2\2\2\u00f7\u00f8\7M\2\2\u00f8\u00f9\7c\2\2\u00f9\u00fa"+
		"\7t\2\2\u00fa\u00fb\7n\2\2\u00fb\u00fc\7q\2\2\u00fc\u00fd\7x\2\2\u00fd"+
		"\u00fe\7c\2\2\u00fe\u00ff\7t\2\2\u00ff\u0100\7u\2\2\u0100\u0101\7m\2\2"+
		"\u0101\u0102\7\u00ff\2\2\u0102\32\3\2\2\2\u0103\u0104\7N\2\2\u0104\u0105"+
		"\7k\2\2\u0105\u0106\7d\2\2\u0106\u0107\7g\2\2\u0107\u0108\7t\2\2\u0108"+
		"\u0109\7g\2\2\u0109\u010a\7e\2\2\u010a\u010b\7m\2\2\u010b\u010c\7\u00ff"+
		"\2\2\u010c\34\3\2\2\2\u010d\u010e\7Q\2\2\u010e\u010f\7n\2\2\u010f\u0110"+
		"\7q\2\2\u0110\u0111\7o\2\2\u0111\u0112\7q\2\2\u0112\u0113\7w\2\2\u0113"+
		"\u0114\7e\2\2\u0114\u0115\7m\2\2\u0115\u0116\7\u00ff\2\2\u0116\36\3\2"+
		"\2\2\u0117\u0118\7R\2\2\u0118\u0119\7n\2\2\u0119\u011a\7|\2\2\u011a\u011b"+
		"\7g\2\2\u011b\u011c\7\u014a\2\2\u011c\u011d\7u\2\2\u011d\u011e\7m\2\2"+
		"\u011e\u011f\7\u00ff\2\2\u011f \3\2\2\2\u0120\u0121\7U\2\2\u0121\u0122"+
		"\7v\2\2\u0122\u0123\7\u015b\2\2\u0123\u0124\7g\2\2\u0124\u0125\7f\2\2"+
		"\u0125\u0126\7q\2\2\u0126\u0127\7\u010f\2\2\u0127\u0128\7g\2\2\u0128\u0129"+
		"\7u\2\2\u0129\u012a\7m\2\2\u012a\u012b\7\u00ff\2\2\u012b\"\3\2\2\2\u012c"+
		"\u012d\7O\2\2\u012d\u012e\7q\2\2\u012e\u012f\7t\2\2\u012f\u0130\7c\2\2"+
		"\u0130\u0131\7x\2\2\u0131\u0132\7u\2\2\u0132\u0133\7m\2\2\u0133\u0134"+
		"\7q\2\2\u0134\u0135\7u\2\2\u0135\u0136\7n\2\2\u0136\u0137\7g\2\2\u0137"+
		"\u0138\7|\2\2\u0138\u0139\7u\2\2\u0139\u013a\7m\2\2\u013a\u013b\7\u00ff"+
		"\2\2\u013b$\3\2\2\2\u013c\u013d\7\u00dc\2\2\u013d\u013e\7u\2\2\u013e\u013f"+
		"\7v\2\2\u013f\u0140\7g\2\2\u0140\u0141\7e\2\2\u0141\u0142\7m\2\2\u0142"+
		"\u0143\7\u00ff\2\2\u0143&\3\2\2\2\u0144\u0145\7\\\2\2\u0145\u0146\7n\2"+
		"\2\u0146\u0147\7\u00ef\2\2\u0147\u0148\7p\2\2\u0148\u0149\7u\2\2\u0149"+
		"\u014a\7m\2\2\u014a\u014b\7\u00ff\2\2\u014b(\3\2\2\2\u014c\u0155\5+\26"+
		"\2\u014d\u0155\5-\27\2\u014e\u0155\5\65\33\2\u014f\u0155\5\63\32\2\u0150"+
		"\u0155\5\61\31\2\u0151\u0155\5\67\34\2\u0152\u0155\59\35\2\u0153\u0155"+
		"\5/\30\2\u0154\u014c\3\2\2\2\u0154\u014d\3\2\2\2\u0154\u014e\3\2\2\2\u0154"+
		"\u014f\3\2\2\2\u0154\u0150\3\2\2\2\u0154\u0151\3\2\2\2\u0154\u0152\3\2"+
		"\2\2\u0154\u0153\3\2\2\2\u0155*\3\2\2\2\u0156\u0157\7|\2\2\u0157\u0158"+
		"\7c\2\2\u0158\u0159\7o\2\2\u0159\u015a\7\u011d\2\2\u015a\u015b\7u\2\2"+
		"\u015b\u015c\7v\2\2\u015c\u015d\7p\2\2\u015d\u015e\7c\2\2\u015e\u015f"+
		"\7p\2\2\u015f\u0160\7g\2\2\u0160\u0161\7e\2\2\u0161,\3\2\2\2\u0162\u0163"+
		"\7r\2\2\u0163\u0164\7q\2\2\u0164\u0165\7f\2\2\u0165\u0166\7p\2\2\u0166"+
		"\u0167\7k\2\2\u0167\u0168\7m\2\2\u0168\u0169\7c\2\2\u0169\u016a\7v\2\2"+
		"\u016a\u016b\7g\2\2\u016b\u016c\7n\2\2\u016c.\3\2\2\2\u016d\u016e\7u\2"+
		"\2\u016e\u016f\7x\2\2\u016f\u0170\7q\2\2\u0170\u0171\7d\2\2\u0171\u0172"+
		"\7q\2\2\u0172\u0173\7f\2\2\u0173\u0174\7p\2\2\u0174\u0175\7\u00eb\2\2"+
		"\u0175\u0176\7\"\2\2\u0176\u0177\7r\2\2\u0177\u0178\7q\2\2\u0178\u0179"+
		"\7x\2\2\u0179\u017a\7q\2\2\u017a\u017b\7n\2\2\u017b\u017c\7\u00e3\2\2"+
		"\u017c\u017d\7p\2\2\u017d\u017e\7\u00ef\2\2\u017e\60\3\2\2\2\u017f\u0180"+
		"\7p\2\2\u0180\u0181\7c\2\2\u0181\u0182\7\"\2\2\u0182\u0183\7t\2\2\u0183"+
		"\u0184\7q\2\2\u0184\u0185\7f\2\2\u0185\u0186\7k\2\2\u0186\u0187\7\u010f"+
		"\2\2\u0187\u0188\7q\2\2\u0188\u0189\7x\2\2\u0189\u018a\7u\2\2\u018a\u018b"+
		"\7m\2\2\u018b\u018c\7\u00eb\2\2\u018c\u018d\7\"\2\2\u018d\u018e\7f\2\2"+
		"\u018e\u018f\7q\2\2\u018f\u0190\7x\2\2\u0190\u0191\7q\2\2\u0191\u0192"+
		"\7n\2\2\u0192\u0193\7g\2\2\u0193\u0194\7p\2\2\u0194\u0195\7\u00eb\2\2"+
		"\u0195\62\3\2\2\2\u0196\u0197\7f\2\2\u0197\u0198\7\u0171\2\2\u0198\u0199"+
		"\7e\2\2\u0199\u019a\7j\2\2\u019a\u019b\7q\2\2\u019b\u019c\7f\2\2\u019c"+
		"\u019d\7e\2\2\u019d\u019e\7g\2\2\u019e\64\3\2\2\2\u019f\u01a0\7Q\2\2\u01a0"+
		"\u01a1\7U\2\2\u01a1\u01a2\7X\2\2\u01a2\u01a3\7\u010e\2\2\u01a3\66\3\2"+
		"\2\2\u01a4\u01a5\7u\2\2\u01a5\u01a6\7v\2\2\u01a6\u01a7\7w\2\2\u01a7\u01a8"+
		"\7f\2\2\u01a8\u01a9\7g\2\2\u01a9\u01aa\7p\2\2\u01aa\u01ab\7v\2\2\u01ab"+
		"8\3\2\2\2\u01ac\u01ad\7d\2\2\u01ad\u01ae\7g\2\2\u01ae\u01af\7|\2\2\u01af"+
		"\u01b0\7\"\2\2\u01b0\u01b1\7|\2\2\u01b1\u01b2\7c\2\2\u01b2\u01b3\7o\2"+
		"\2\u01b3\u01b4\7\u011d\2\2\u01b4\u01b5\7u\2\2\u01b5\u01b6\7v\2\2\u01b6"+
		"\u01b7\7p\2\2\u01b7\u01b8\7\u00e3\2\2\u01b8\u01b9\7p\2\2\u01b9\u01ba\7"+
		"\u00ef\2\2\u01ba:\3\2\2\2\u01bb\u01c4\5=\37\2\u01bc\u01c4\5? \2\u01bd"+
		"\u01c4\5A!\2\u01be\u01c4\5C\"\2\u01bf\u01c4\5E#\2\u01c0\u01c4\5G$\2\u01c1"+
		"\u01c4\5I%\2\u01c2\u01c4\5K&\2\u01c3\u01bb\3\2\2\2\u01c3\u01bc\3\2\2\2"+
		"\u01c3\u01bd\3\2\2\2\u01c3\u01be\3\2\2\2\u01c3\u01bf\3\2\2\2\u01c3\u01c0"+
		"\3\2\2\2\u01c3\u01c1\3\2\2\2\u01c3\u01c2\3\2\2\2\u01c4<\3\2\2\2\u01c5"+
		"\u01c6\7c\2\2\u01c6\u01c7\7w\2\2\u01c7\u01c8\7v\2\2\u01c8\u01c9\7q\2\2"+
		"\u01c9\u01ca\7/\2\2\u01ca\u01cb\7o\2\2\u01cb\u01cc\7q\2\2\u01cc\u01cd"+
		"\7v\2\2\u01cd\u01ce\7q\2\2\u01ce>\3\2\2\2\u01cf\u01d0\7e\2\2\u01d0\u01d1"+
		"\7g\2\2\u01d1\u01d2\7u\2\2\u01d2\u01d3\7v\2\2\u01d3\u01d4\7q\2\2\u01d4"+
		"\u01d5\7x\2\2\u01d5\u01d6\7\u00e3\2\2\u01d6\u01d7\7p\2\2\u01d7\u01d8\7"+
		"\u00ef\2\2\u01d8@\3\2\2\2\u01d9\u01da\7f\2\2\u01da\u01db\7q\2\2\u01db"+
		"\u01dc\7o\2\2\u01dc\u01dd\7\u00e3\2\2\u01dd\u01de\7e\2\2\u01de\u01df\7"+
		"p\2\2\u01df\u01e0\7q\2\2\u01e0\u01e1\7u\2\2\u01e1\u01e2\7v\2\2\u01e2B"+
		"\3\2\2\2\u01e3\u01e4\7g\2\2\u01e4\u01e5\7n\2\2\u01e5\u01e6\7g\2\2\u01e6"+
		"\u01e7\7m\2\2\u01e7\u01e8\7v\2\2\u01e8\u01e9\7t\2\2\u01e9\u01ea\7q\2\2"+
		"\u01ea\u01eb\7p\2\2\u01eb\u01ec\7k\2\2\u01ec\u01ed\7m\2\2\u01ed\u01ee"+
		"\7c\2\2\u01eeD\3\2\2\2\u01ef\u01f0\7t\2\2\u01f0\u01f1\7g\2\2\u01f1\u01f2"+
		"\7h\2\2\u01f2\u01f3\7k\2\2\u01f3\u01f4\7p\2\2\u01f4\u01f5\7c\2\2\u01f5"+
		"\u01f6\7p\2\2\u01f6\u01f7\7e\2\2\u01f7\u01f8\7q\2\2\u01f8\u01f9\7x\2\2"+
		"\u01f9\u01fa\7\u00e3\2\2\u01fa\u01fb\7p\2\2\u01fb\u01fc\7\u00ef\2\2\u01fc"+
		"\u01fd\7\"\2\2\u01fd\u01fe\7r\2\2\u01fe\u01ff\7\u0171\2\2\u01ff\u0200"+
		"\7l\2\2\u0200\u0201\7\u010f\2\2\u0201\u0202\7g\2\2\u0202\u0203\7m\2\2"+
		"\u0203F\3\2\2\2\u0204\u0205\7x\2\2\u0205\u0206\7n\2\2\u0206\u0207\7c\2"+
		"\2\u0207\u0208\7u\2\2\u0208\u0209\7v\2\2\u0209\u020a\7p\2\2\u020a\u020b"+
		"\7\u00ef\2\2\u020b\u020c\7\"\2\2\u020c\u020d\7r\2\2\u020d\u020e\7t\2\2"+
		"\u020e\u020f\7q\2\2\u020f\u0210\7l\2\2\u0210\u0211\7g\2\2\u0211\u0212"+
		"\7m\2\2\u0212\u0213\7v\2\2\u0213H\3\2\2\2\u0214\u0215\7x\2\2\u0215\u0216"+
		"\7|\2\2\u0216\u0217\7f\2\2\u0217\u0218\7\u011d\2\2\u0218\u0219\7n\2\2"+
		"\u0219\u021a\7\u00e3\2\2\u021a\u021b\7p\2\2\u021b\u021c\7\u00ef\2\2\u021c"+
		"J\3\2\2\2\u021d\u021e\7|\2\2\u021e\u021f\7f\2\2\u021f\u0220\7t\2\2\u0220"+
		"\u0221\7c\2\2\u0221\u0222\7x\2\2\u0222\u0223\7\u00ef\2\2\u0223L\3\2\2"+
		"\2\u0224\u0228\5O(\2\u0225\u0228\5Q)\2\u0226\u0228\5S*\2\u0227\u0224\3"+
		"\2\2\2\u0227\u0225\3\2\2\2\u0227\u0226\3\2\2\2\u0228N\3\2\2\2\u0229\u022a"+
		"\7p\2\2\u022a\u022b\7k\2\2\u022b\u022c\7m\2\2\u022c\u022d\7f\2\2\u022d"+
		"\u022e\7{\2\2\u022e\u022f\7\"\2\2\u022f\u0230\7p\2\2\u0230\u0231\7g\2"+
		"\2\u0231\u0232\7d\2\2\u0232\u0233\7{\2\2\u0233\u0234\7n\2\2\u0234\u0235"+
		"\7c\2\2\u0235P\3\2\2\2\u0236\u0237\7p\2\2\u0237\u0238\7{\2\2\u0238\u0239"+
		"\7p\2\2\u0239\u023a\7\u00ef\2\2\u023a\u023b\7\"\2\2\u023b\u023c\7p\2\2"+
		"\u023c\u023d\7g\2\2\u023d\u023e\7p\2\2\u023e\u023f\7\u00ef\2\2\u023fR"+
		"\3\2\2\2\u0240\u0241\7p\2\2\u0241\u0242\7{\2\2\u0242\u0243\7p\2\2\u0243"+
		"\u0244\7\u00ef\2\2\u0244\u0245\7\"\2\2\u0245\u0246\7l\2\2\u0246\u0247"+
		"\7g\2\2\u0247T\3\2\2\2\u0248\u0249\7\"\2\2\u0249\u024a\7M\2\2\u024a\u024b"+
		"\7\u010f\2\2\u024bV\3\2\2\2\u024c\u024d\7\60\2\2\u024dX\3\2\2\2\u024e"+
		"\u024f\7/\2\2\u024f\u0250\7\"\2\2\u0250Z\3\2\2\2\u0251\u0252\7\"\2\2\u0252"+
		"\u0253\7c\2\2\u0253\u0254\7\u0180\2\2\u0254\u0255\7\"\2\2\u0255\\\3\2"+
		"\2\2\u0256\u0257\7l\2\2\u0257\u0258\7g\2\2\u0258\u0259\7\"\2\2\u0259^"+
		"\3\2\2\2\u025a\u025b\7\"\2\2\u025b\u025c\7p\2\2\u025c\u025d\7g\2\2\u025d"+
		"\u025e\7d\2\2\u025e\u025f\7q\2\2\u025f\u0260\7\"\2\2\u0260`\3\2\2\2\u0261"+
		"\u0262\5w<\2\u0262\u0263\7\"\2\2\u0263b\3\2\2\2\u0264\u0265\7p\2\2\u0265"+
		"\u0266\7g\2\2\u0266\u0267\7f\2\2\u0267\u0268\7q\2\2\u0268\u0269\7u\2\2"+
		"\u0269\u026a\7c\2\2\u026a\u026b\7j\2\2\u026b\u026c\7w\2\2\u026c\u026d"+
		"\7l\2\2\u026d\u026e\7g\2\2\u026e\u026f\7\"\2\2\u026fd\3\2\2\2\u0270\u0271"+
		"\7r\2\2\u0271\u0272\7\u015b\2\2\u0272\u0273\7g\2\2\u0273\u0274\7u\2\2"+
		"\u0274\u0275\7c\2\2\u0275\u0276\7j\2\2\u0276\u0277\7w\2\2\u0277\u0278"+
		"\7l\2\2\u0278\u0279\7g\2\2\u0279\u027a\7\"\2\2\u027af\3\2\2\2\u027b\u027c"+
		"\7l\2\2\u027c\u027d\7k\2\2\u027d\u027e\7p\2\2\u027e\u027f\7\u00eb\2\2"+
		"\u027fh\3\2\2\2\u0280\u0281\7\"\2\2\u0281\u0282\7o\2\2\u0282\u0283\7\u011d"+
		"\2\2\u0283\u0284\7u\2\2\u0284\u0285\7\u00ef\2\2\u0285\u0286\7e\2\2\u0286"+
		"\u0287\7\u0171\2\2\u0287j\3\2\2\2\u0288\u028a\5u;\2\u0289\u0288\3\2\2"+
		"\2\u028a\u028b\3\2\2\2\u028b\u0289\3\2\2\2\u028b\u028c\3\2\2\2\u028cl"+
		"\3\2\2\2\u028d\u028f\5u;\2\u028e\u028d\3\2\2\2\u028f\u0290\3\2\2\2\u0290"+
		"\u028e\3\2\2\2\u0290\u0291\3\2\2\2\u0291\u0292\3\2\2\2\u0292\u0294\5w"+
		"<\2\u0293\u0295\5u;\2\u0294\u0293\3\2\2\2\u0295\u0296\3\2\2\2\u0296\u0294"+
		"\3\2\2\2\u0296\u0297\3\2\2\2\u0297n\3\2\2\2\u0298\u029c\7%\2\2\u0299\u029b"+
		"\n\2\2\2\u029a\u0299\3\2\2\2\u029b\u029e\3\2\2\2\u029c\u029a\3\2\2\2\u029c"+
		"\u029d\3\2\2\2\u029d\u029f\3\2\2\2\u029e\u029c\3\2\2\2\u029f\u02a0\5q"+
		"9\2\u02a0\u02a1\3\2\2\2\u02a1\u02a2\b8\2\2\u02a2p\3\2\2\2\u02a3\u02a4"+
		"\7\17\2\2\u02a4\u02a7\7\f\2\2\u02a5\u02a7\t\2\2\2\u02a6\u02a3\3\2\2\2"+
		"\u02a6\u02a5\3\2\2\2\u02a7\u02a8\3\2\2\2\u02a8\u02a9\b9\2\2\u02a9r\3\2"+
		"\2\2\u02aa\u02ac\t\3\2\2\u02ab\u02aa\3\2\2\2\u02ac\u02ad\3\2\2\2\u02ad"+
		"\u02ab\3\2\2\2\u02ad\u02ae\3\2\2\2\u02ae\u02af\3\2\2\2\u02af\u02b0\b:"+
		"\2\2\u02b0t\3\2\2\2\u02b1\u02b2\t\4\2\2\u02b2v\3\2\2\2\u02b3\u02b4\7."+
		"\2\2\u02b4x\3\2\2\2\r\2\u00b4\u0154\u01c3\u0227\u028b\u0290\u0296\u029c"+
		"\u02a6\u02ad\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}

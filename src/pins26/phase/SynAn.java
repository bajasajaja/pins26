package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	/**
	 * Ustvari nov sintaksni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public SynAn(final String srcFileName) {
		this.lexAn = new LexAn(srcFileName);
	}

	@Override
	public void close() {
		lexAn.close();
	}

	/**
	 * Prevzame leksikalni analizator od leksikalnega analizatorja in preveri, ali
	 * je prave vrste.
	 *
	 * @param symbol Pricakovana vrsta leksikalnega simbola.
	 * @return Prevzeti leksikalni simbol.
	 */
	private Token check(Token.Symbol symbol) {
		final Token token = lexAn.takeToken();
		if (token.symbol() != symbol)
			throw new Report.Error(token, "Unexpected symbol '" + token.lexeme() + "'.");
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public void parse() {
				parseProgram();
                if (lexAn.peekToken().symbol() != Token.Symbol.EOF) Report.warning(lexAn.peekToken(), "Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
	}
	private void parseProgram() {
		Token.Symbol symbol = lexAn.peekToken().symbol();
		if (symbol == Token.Symbol.FUN || symbol == Token.Symbol.VAR){
			System.out.println("program -> definitions");
			parseDefinitions();
		} else {
			System.out.println("program -> empty");
		}
	}

	private void parseDefinitions() {
		System.out.println("definitions -> definition definitions2");
		parseDefinition();
		parseDefinitions2();
	}

	private void parseDefinition() {
		Token.Symbol symbol = lexAn.peekToken().symbol();
		if (symbol == Token.Symbol.FUN) {
			System.out.println("definition -> fun IDENTIFIER ( parameters ) definition_tail");
			check(Token.Symbol.FUN);
			check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.LPAREN);
			parseParameters();
			check(Token.Symbol.RPAREN);
			parseDefinitionTail();
		} else if (symbol == Token.Symbol.VAR) {
			System.out.println("definition -> var IDENTIFIER = initializers");
			check(Token.Symbol.VAR);
			check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.ASSIGN);
			parseInitializers();
		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected 'fun' or 'var'.");
		}
	}

	private void parseDefinitions2() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.FUN || s == Token.Symbol.VAR) {
			System.out.println("definitions2 -> definition definitions2");
			parseDefinition();
			parseDefinitions2();
		} else {
			System.out.println("definitions2 -> empty");
		}
	}

	private void parseParameters() {
		if (lexAn.peekToken().symbol() == Token.Symbol.IDENTIFIER) {
			System.out.println("parameters -> param_list");
			parseParamList();
		} else {
			System.out.println("parameters -> empty");
		}
	}

	private void parseDefinitionTail() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
			System.out.println("definition_tail -> = statements");
			check(Token.Symbol.ASSIGN);
			parseStatements();
		} else {
			System.out.println("definition_tail -> empty");
		}
	}

	private void parseParamList() {
		System.out.println("param_list -> IDENTIFIER param_list2");
		check(Token.Symbol.IDENTIFIER);
		parseParamList2();
	}

	private void parseParamList2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			System.out.println("param_list2 -> , IDENTIFIER param_list2");
			check(Token.Symbol.COMMA);
			check(Token.Symbol.IDENTIFIER);
			parseParamList2();
		} else {
			System.out.println("param_list2 -> empty");
		}
	}

	private void parseStatements() {
		System.out.println("statements -> statement ; statements2");
		parseStatement();
		check(Token.Symbol.SEMIC);
		parseStatements2();
	}

	private boolean isExpressionStart(Token.Symbol s) {
		return switch (s) {
			case NOT, ADD, SUB, PTR, INTCONST, CHARCONST, STRINGCONST, IDENTIFIER, LPAREN -> true;
			default -> false;
		};
	}

	private void parseStatements2() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (isExpressionStart(s) || s == Token.Symbol.IF || s == Token.Symbol.WHILE || s == Token.Symbol.LET) {
			System.out.println("statements2 -> statement ; statements2");
			parseStatement();
			check(Token.Symbol.SEMIC);
			parseStatements2();
		} else {
			System.out.println("statements2 -> empty");
		}
	}

	private void parseStatement() {
		Token.Symbol s = lexAn.peekToken().symbol();
		switch (s) {
			case IF:
				System.out.println("statement -> if expression then statements if_tail");
				check(Token.Symbol.IF);
				parseExpression();
				check(Token.Symbol.THEN);
				parseStatements();
				parseIfTail();
				break;
			case WHILE:
				System.out.println("statement -> while expression do statements end");
				check(Token.Symbol.WHILE);
				parseExpression();
				check(Token.Symbol.DO);
				parseStatements();
				check(Token.Symbol.END);
				break;
			case LET:
				System.out.println("statement -> let definitions_plus in statements end");
				check(Token.Symbol.LET);
				parseDefinitionsPlus();
				check(Token.Symbol.IN);
				parseStatements();
				check(Token.Symbol.END);
				break;
			default:
				System.out.println("statement -> expression statement_tail");
				parseExpression();
				parseStatementTail();
				break;
		}
	}

	private void parseStatementTail() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
			System.out.println("statement_tail -> = expression");
			check(Token.Symbol.ASSIGN);
			parseExpression();
		} else {
			System.out.println("statement_tail -> empty");
		}
	}

	private void parseIfTail() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ELSE) {
			System.out.println("if_tail -> else statements end");
			check(Token.Symbol.ELSE);
			parseStatements();
			check(Token.Symbol.END);
		} else {
			System.out.println("if_tail -> end");
			check(Token.Symbol.END);
		}
	}

	private void parseDefinitionsPlus() {
		System.out.println("definitions_plus -> definition definitions_plus2");
		parseDefinition();
		parseDefinitionsPlus2();
	}

	private void parseDefinitionsPlus2() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.FUN || s == Token.Symbol.VAR) {
			System.out.println("definitions_plus2 -> definition definitions_plus2");
			parseDefinition();
			parseDefinitionsPlus2();
		} else {
			System.out.println("definitions_plus2 -> empty");
		}
	}

	private void parseExpression() {
		System.out.println("expression -> conjunction expression2");
		parseConjunction();
		parseExpression2();
	}

	private void parseExpression2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.OR) {
			System.out.println("expression2 -> || conjunction expression2");
			check(Token.Symbol.OR);
			parseConjunction();
			parseExpression2();
		} else {
			System.out.println("expression2 -> empty");
		}
	}

	private void parseConjunction() {
		System.out.println("conjunction -> comparison conjunction2");
		parseComparison();
		parseConjunction2();
	}

	private void parseConjunction2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.AND) {
			System.out.println("conjunction2 -> && comparison conjunction2");
			check(Token.Symbol.AND);
			parseComparison();
			parseConjunction2();
		} else {
			System.out.println("conjunction2 -> empty");
		}
	}

	private void parseComparison() {
		System.out.println("comparison -> additive comparison_tail");
		parseAdditive();
		parseComparisonTail();
	}

	private void parseComparisonTail() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.EQU || s == Token.Symbol.NEQ || s == Token.Symbol.LTH ||
				s == Token.Symbol.GTH || s == Token.Symbol.LEQ || s == Token.Symbol.GEQ) {
			System.out.println("comparison_tail -> " + s + " additive");
			check(s);
			parseAdditive();
		} else {
			System.out.println("comparison_tail -> empty");
		}
	}

	private void parseAdditive() {
		System.out.println("additive -> multiplicative additive2");
		parseMultiplicative();
		parseAdditive2();
	}

	private void parseAdditive2() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.ADD) {
			System.out.println("additive2 -> + multiplicative additive2");
			check(Token.Symbol.ADD);
			parseMultiplicative();
			parseAdditive2();
		} else if (s == Token.Symbol.SUB) {
			System.out.println("additive2 -> - multiplicative additive2");
			check(Token.Symbol.SUB);
			parseMultiplicative();
			parseAdditive2();
		} else {
			System.out.println("additive2 -> empty");
		}
	}

	private void parseMultiplicative() {
		System.out.println("multiplicative -> prefix multiplicative2");
		parsePrefix();
		parseMultiplicative2();
	}

	private void parseMultiplicative2() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.MUL) {
			System.out.println("multiplicative2 -> * prefix multiplicative2");
			check(Token.Symbol.MUL);
			parsePrefix();
			parseMultiplicative2();
		} else if (s == Token.Symbol.DIV) {
			System.out.println("multiplicative2 -> / prefix multiplicative2");
			check(Token.Symbol.DIV);
			parsePrefix();
			parseMultiplicative2();
		} else if (s == Token.Symbol.MOD) {
			System.out.println("multiplicative2 -> % prefix multiplicative2");
			check(Token.Symbol.MOD);
			parsePrefix();
			parseMultiplicative2();
		} else {
			System.out.println("multiplicative2 -> empty");
		}
	}

	private void parsePrefix() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.NOT || s == Token.Symbol.ADD || s == Token.Symbol.SUB || s == Token.Symbol.PTR) {
			System.out.println("prefix -> " + s + " prefix");
			check(s);
			parsePrefix();
		} else {
			System.out.println("prefix -> postfix");
			parsePostfix();
		}
	}

	private void parsePostfix() {
		System.out.println("postfix -> primary postfix2");
		parsePrimary();
		parsePostfix2();
	}

	private void parsePostfix2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			System.out.println("postfix2 -> ^ postfix2");
			check(Token.Symbol.PTR);
			parsePostfix2();
		} else {
			System.out.println("postfix2 -> empty");
		}
	}

	private void parsePrimary() {
		Token.Symbol s = lexAn.peekToken().symbol();
		switch (s) {
			case INTCONST:
				System.out.println("primary -> INTCONST");
				check(Token.Symbol.INTCONST);
				break;
			case CHARCONST:
				System.out.println("primary -> CHARCONST");
				check(Token.Symbol.CHARCONST);
				break;
			case STRINGCONST:
				System.out.println("primary -> STRINGCONST");
				check(Token.Symbol.STRINGCONST);
				break;
			case IDENTIFIER:
				System.out.println("primary -> IDENTIFIER primary_tail");
				check(Token.Symbol.IDENTIFIER);
				parsePrimaryTail();
				break;
			case LPAREN:
				System.out.println("primary -> ( expression )");
				check(Token.Symbol.LPAREN);
				parseExpression();
				check(Token.Symbol.RPAREN);
				break;
			default:
				throw new Report.Error(lexAn.peekToken(), "Expected constant, identifier or '('.");
		}
	}

	private void parsePrimaryTail() {
		if (lexAn.peekToken().symbol() == Token.Symbol.LPAREN) {
			System.out.println("primary_tail -> ( arguments )");
			check(Token.Symbol.LPAREN);
			parseArguments();
			check(Token.Symbol.RPAREN);
		} else {
			System.out.println("primary_tail -> empty");
		}
	}

	private void parseArguments() {
		if (isExpressionStart(lexAn.peekToken().symbol())) {
			System.out.println("arguments -> arg_list");
			parseArgList();
		} else {
			System.out.println("arguments -> empty");
		}
	}

	private void parseArgList() {
		System.out.println("arg_list -> expression arg_list2");
		parseExpression();
		parseArgList2();
	}

	private void parseArgList2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			System.out.println("arg_list2 -> , expression arg_list2");
			check(Token.Symbol.COMMA);
			parseExpression();
			parseArgList2();
		} else {
			System.out.println("arg_list2 -> empty");
		}
	}

	private void parseInitializers() {
		System.out.println("initializers -> init_list");
		parseInitList();
	}

	private void parseInitList() {
		System.out.println("init_list -> initializer init_list2");
		parseInitializer();
		parseInitList2();
	}

	private void parseInitList2() {
		if (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			System.out.println("init_list2 -> , initializer init_list2");
			check(Token.Symbol.COMMA);
			parseInitializer();
			parseInitList2();
		} else {
			System.out.println("init_list2 -> empty");
		}
	}

	private void parseInitializer() {
		if (lexAn.peekToken().symbol() == Token.Symbol.INTCONST) {
			System.out.println("initializer -> INTCONST initializer_int_tail");
			check(Token.Symbol.INTCONST);
			parseInitializerIntTail();
		} else {
			System.out.println("initializer -> const");
			parseConst();
		}
	}

	private void parseInitializerIntTail() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.INTCONST || s == Token.Symbol.CHARCONST || s == Token.Symbol.STRINGCONST) {
			System.out.println("initializer_int_tail -> const");
			parseConst();
		} else {
			System.out.println("initializer_int_tail -> empty");
		}
	}

	private void parseConst() {
		Token.Symbol s = lexAn.peekToken().symbol();
		if (s == Token.Symbol.INTCONST || s == Token.Symbol.CHARCONST || s == Token.Symbol.STRINGCONST) {
			System.out.println("const -> " + s);
			check(s);
		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected a constant.");
		}
	}

	// --- ZAGON ---

	/**
	 * Zagon sintaksnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (syntax analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				synAn.parse();
			}

			// Upajmo, da kdaj pridemo to te tocke.
			// A zavedajmo se sledecega:
			// 1. Prevod je zaradi napak v programu lahko napacen :-o
			// 2. Izvorni program se zdalec ni tisto, kar je programer hotel, da bi bil ;-)
			Report.info("Done.");
		} catch (Report.Error error) {
			// Izpis opisa napake.
			System.err.println(error.getMessage());
			System.exit(1);
		}
	}

}

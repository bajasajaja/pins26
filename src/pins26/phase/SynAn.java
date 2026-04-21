package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Sintaksni analizator.
 */
public class SynAn implements AutoCloseable {

	/** Leksikalni analizator. */
	private final LexAn lexAn;

	private HashMap<AST.Node, Report.Locatable> attrLoc;

	private Token lastToken;

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

	private <T extends AST.Node> T loc(T node, Token startToken, Token endToken) {
		if (startToken != null && endToken != null) {
			attrLoc.put(node, new Report.Location(startToken, endToken));
		}
		return node;
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
		lastToken = token;
		return token;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public AST.Node parse(HashMap<AST.Node, Report.Locatable> attrLoc) {
				this.attrLoc = attrLoc;
				AST.Node ast = parseProgram();
                if (lexAn.peekToken().symbol() != Token.Symbol.EOF) Report.warning(lexAn.peekToken(), "Unexpected text '" + lexAn.peekToken().lexeme() + "...' at the end of the program.");
				return ast;
	}
	private AST.Nodes<AST.MainDef> parseProgram() {
		Token start = lexAn.peekToken();
		List<AST.MainDef> defs = new ArrayList<>();

		while (lexAn.peekToken().symbol() == Token.Symbol.FUN || lexAn.peekToken().symbol() == Token.Symbol.VAR) {
			defs.add(parseDefinition());
		}

		AST.Nodes<AST.MainDef> nodes = new AST.Nodes<>(defs);
		return loc(nodes, start, lastToken != null ? lastToken : start);
	}

	//definition -> fun IDENTIFIER (parameters) (= statements)? OR var IDENTIFIER = initializers

	private AST.MainDef parseDefinition() {
		Token start = lexAn.peekToken();
		if (start.symbol() == Token.Symbol.FUN) {
			check(Token.Symbol.FUN);
			Token id = check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.LPAREN);
			List<AST.ParDef> pars = parseParameters();
			check(Token.Symbol.RPAREN);
			List<AST.Stmt> body = parseDefinitionTail();

			AST.FunDef funDef = new AST.FunDef(id.lexeme(), pars, body);
			return loc(funDef, start, lastToken);
		} else if (start.symbol() == Token.Symbol.VAR) {
			check(Token.Symbol.VAR);
			Token id = check(Token.Symbol.IDENTIFIER);
			check(Token.Symbol.ASSIGN);
			List<AST.Init> inits = parseInitializers();

			AST.VarDef varDef = new AST.VarDef(id.lexeme(), inits);
			return loc(varDef, start, lastToken);
		} else {
			throw new Report.Error(lexAn.peekToken(), "Expected 'fun' or 'var'.");
		}
	}

	//parameters -> (IDENTIFIER (, IDENTIFIER)*)?

	private List<AST.ParDef> parseParameters() {
		List<AST.ParDef> pars = new ArrayList<>();
		if (lexAn.peekToken().symbol() == Token.Symbol.IDENTIFIER) {
			Token id = check(Token.Symbol.IDENTIFIER);
			pars.add(loc(new AST.ParDef(id.lexeme()), id, id));

			while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
				check(Token.Symbol.COMMA);
				Token nextId = check(Token.Symbol.IDENTIFIER);
				pars.add(loc(new AST.ParDef(nextId.lexeme()), nextId, nextId));
			}
		}
		return pars;
	}

	private List<AST.Stmt> parseDefinitionTail() {
		if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
			check(Token.Symbol.ASSIGN);
			return parseStatements();
		}
		return new ArrayList<>(); //For empty functions
	}

	//statements -> statement; (statement;)*

	private List<AST.Stmt> parseStatements() {
		List<AST.Stmt> stmts = new ArrayList<>();
		do {
			stmts.add(parseStatement());
			check(Token.Symbol.SEMIC); //Semicolon is required after each statement,but not important to AST
		} while (isStatementStart(lexAn.peekToken().symbol()));
		return stmts;
	}

	private boolean isStatementStart(Token.Symbol s) {
		return switch (s) {
			case IF, WHILE, LET, NOT, ADD, SUB, PTR, INTCONST, CHARCONST, STRINGCONST, IDENTIFIER, LPAREN -> true;
			default -> false;
		};
	}

	private AST.Stmt parseStatement() {
		Token start = lexAn.peekToken();
		switch (start.symbol()) {
			case IF: {
				check(Token.Symbol.IF);
				AST.Expr cond = parseExpression();
				check(Token.Symbol.THEN);
				List<AST.Stmt> thenStmts = parseStatements();
				List<AST.Stmt> elseStmts = new ArrayList<>();

				if (lexAn.peekToken().symbol() == Token.Symbol.ELSE) {
					check(Token.Symbol.ELSE);
					elseStmts = parseStatements();
				}
				check(Token.Symbol.END);

				AST.IfStmt ifStmt = new AST.IfStmt(cond, thenStmts, elseStmts);
				return loc(ifStmt, start, lastToken);
			}
			case WHILE: {
				check(Token.Symbol.WHILE);
				AST.Expr cond = parseExpression();
				check(Token.Symbol.DO);
				List<AST.Stmt> body = parseStatements();
				check(Token.Symbol.END);

				AST.WhileStmt whileStmt = new AST.WhileStmt(cond, body);
				return loc(whileStmt, start, lastToken);
			}
			case LET: {
				check(Token.Symbol.LET);
				List<AST.MainDef> defs = new ArrayList<>();
				do {
					defs.add(parseDefinition());
				} while (lexAn.peekToken().symbol() == Token.Symbol.FUN || lexAn.peekToken().symbol() == Token.Symbol.VAR);

				check(Token.Symbol.IN);
				List<AST.Stmt> stmts = parseStatements();
				check(Token.Symbol.END);

				AST.LetStmt letStmt = new AST.LetStmt(defs, stmts);
				return loc(letStmt, start, lastToken);
			}
			default: {
				AST.Expr expr = parseExpression();
				if (lexAn.peekToken().symbol() == Token.Symbol.ASSIGN) {
					check(Token.Symbol.ASSIGN);
					AST.Expr src = parseExpression();

					AST.AssignStmt assignStmt = new AST.AssignStmt(expr, src);
					return loc(assignStmt, start, lastToken);
				} else {
					AST.ExprStmt exprStmt = new AST.ExprStmt(expr);
					return loc(exprStmt, start, lastToken);
				}
			}
		}
	}

	//expression -> expression || expression

	private AST.Expr parseExpression() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parseConjunction();

		while (lexAn.peekToken().symbol() == Token.Symbol.OR) {
			check(Token.Symbol.OR);
			AST.Expr right = parseConjunction();
			expr = loc(new AST.BinExpr(AST.BinExpr.Oper.OR, expr, right), start, lastToken);
		}
		return expr;
	}

	//expression -> expression && expression

	private AST.Expr parseConjunction() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parseComparison();

		while (lexAn.peekToken().symbol() == Token.Symbol.AND) {
			check(Token.Symbol.AND);
			AST.Expr right = parseComparison();
			expr = loc(new AST.BinExpr(AST.BinExpr.Oper.AND, expr, right), start, lastToken);
		}
		return expr;
	}

	//expression -> expression (== | != | < | > | <= | >=) expression

	private AST.Expr parseComparison() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parseAdditive();
		Token.Symbol s = lexAn.peekToken().symbol();

		if (s == Token.Symbol.EQU || s == Token.Symbol.NEQ || s == Token.Symbol.LTH ||
				s == Token.Symbol.GTH || s == Token.Symbol.LEQ || s == Token.Symbol.GEQ) {
			check(s);
			AST.BinExpr.Oper op = switch (s) {
				case EQU -> AST.BinExpr.Oper.EQU;
				case NEQ -> AST.BinExpr.Oper.NEQ;
				case LTH -> AST.BinExpr.Oper.LTH;
				case GTH -> AST.BinExpr.Oper.GTH;
				case LEQ -> AST.BinExpr.Oper.LEQ;
				case GEQ -> AST.BinExpr.Oper.GEQ;
				default -> throw new Report.InternalError();
			};
			AST.Expr right = parseAdditive();
			expr = loc(new AST.BinExpr(op, expr, right), start, lastToken);
		}
		return expr;
	}

	//expression -> expression (+ | -) expression

	private AST.Expr parseAdditive() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parseMultiplicative();
		Token.Symbol s = lexAn.peekToken().symbol();

		while (s == Token.Symbol.ADD || s == Token.Symbol.SUB) {
			check(s);
			AST.BinExpr.Oper op = (s == Token.Symbol.ADD) ? AST.BinExpr.Oper.ADD : AST.BinExpr.Oper.SUB;
			AST.Expr right = parseMultiplicative();
			expr = loc(new AST.BinExpr(op, expr, right), start, lastToken);
			s = lexAn.peekToken().symbol();
		}
		return expr;
	}

	//expression -> expression (* | / | %) expression

	private AST.Expr parseMultiplicative() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parsePrefix();
		Token.Symbol s = lexAn.peekToken().symbol();

		while (s == Token.Symbol.MUL || s == Token.Symbol.DIV || s == Token.Symbol.MOD) {
			check(s);
			AST.BinExpr.Oper op = switch (s) {
				case MUL -> AST.BinExpr.Oper.MUL;
				case DIV -> AST.BinExpr.Oper.DIV;
				case MOD -> AST.BinExpr.Oper.MOD;
				default -> throw new Report.InternalError();
			};
			AST.Expr right = parsePrefix();
			expr = loc(new AST.BinExpr(op, expr, right), start, lastToken);
			s = lexAn.peekToken().symbol();
		}
		return expr;
	}

	//expression -> prefix-operator expression

	private AST.Expr parsePrefix() {
		Token start = lexAn.peekToken();
		Token.Symbol s = start.symbol();

		if (s == Token.Symbol.NOT || s == Token.Symbol.ADD || s == Token.Symbol.SUB || s == Token.Symbol.PTR) {
			check(s);
			AST.UnExpr.Oper op = switch (s) {
				case NOT -> AST.UnExpr.Oper.NOT;
				case ADD -> AST.UnExpr.Oper.ADD;
				case SUB -> AST.UnExpr.Oper.SUB;
				case PTR -> AST.UnExpr.Oper.MEMADDR;
				default -> throw new Report.InternalError();
			};
			AST.Expr expr = parsePrefix();
			return loc(new AST.UnExpr(op, expr), start, lastToken);
		} else {
			return parsePostfix();
		}
	}

	//expression -> expression postfix-operator

	private AST.Expr parsePostfix() {
		Token start = lexAn.peekToken();
		AST.Expr expr = parsePrimary();

		while (lexAn.peekToken().symbol() == Token.Symbol.PTR) {
			check(Token.Symbol.PTR);
			expr = loc(new AST.UnExpr(AST.UnExpr.Oper.VALUEAT, expr), start, lastToken);
		}
		return expr;
	}

	//expression -> (expression) OR IDENTIFIER (arguments)? OR const

	private AST.Expr parsePrimary() {
		Token start = lexAn.peekToken();
		Token.Symbol s = start.symbol();

		switch (s) {
			case INTCONST:
			case CHARCONST:
			case STRINGCONST:
				return parseConst();
			case IDENTIFIER: {
				Token id = check(Token.Symbol.IDENTIFIER);
				if (lexAn.peekToken().symbol() == Token.Symbol.LPAREN) {
					check(Token.Symbol.LPAREN);
					List<AST.Expr> args = new ArrayList<>();

					if (isStatementStart(lexAn.peekToken().symbol())) {
						args = parseArguments();
					}
					check(Token.Symbol.RPAREN);

					AST.CallExpr callExpr = new AST.CallExpr(id.lexeme(), args);
					return loc(callExpr, start, lastToken);
				} else {
					AST.VarExpr varExpr = new AST.VarExpr(id.lexeme());
					return loc(varExpr, start, lastToken);
				}
			}
			case LPAREN: {
				check(Token.Symbol.LPAREN);
				AST.Expr expr = parseExpression();
				check(Token.Symbol.RPAREN);
				return loc(expr, start, lastToken);
			}
			default:
				throw new Report.Error(lexAn.peekToken(), "Expected constant, identifier or '('.");
		}
	}

	//arguments -> (expression (, expression)*)?

	private List<AST.Expr> parseArguments() {
		List<AST.Expr> args = new ArrayList<>();
		args.add(parseExpression());

		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			check(Token.Symbol.COMMA);
			args.add(parseExpression());
		}
		return args;
	}

	//initializers -> (initializer (, initializer)*)

	private List<AST.Init> parseInitializers() {
		List<AST.Init> inits = new ArrayList<>();
		inits.add(parseInitializer());

		while (lexAn.peekToken().symbol() == Token.Symbol.COMMA) {
			check(Token.Symbol.COMMA);
			inits.add(parseInitializer());
		}
		return inits;
	}

	private AST.Init parseInitializer() {
		Token start = lexAn.peekToken();

		if (lexAn.peekToken().symbol() == Token.Symbol.INTCONST) {
			Token intConst = check(Token.Symbol.INTCONST);
			AST.AtomExpr first = loc(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, intConst.lexeme()), intConst, intConst);

			Token.Symbol next = lexAn.peekToken().symbol();
			// From the rule:initializer -> (INTCONST)? const ((how many)? value)
			if (next == Token.Symbol.INTCONST || next == Token.Symbol.CHARCONST || next == Token.Symbol.STRINGCONST) {
				AST.AtomExpr second = parseConst();
				AST.Init init = new AST.Init(first, second);
				return loc(init, start, lastToken);
			} else {
				// From the rule:initializer -> (INTCONST)? const ((how many)? value),we just implicitly add 1
				AST.AtomExpr implicitOne = loc(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), start, start);
				AST.Init init = new AST.Init(implicitOne, first);
				return loc(init, start, lastToken);
			}
		} else {
			AST.AtomExpr c = parseConst();
			AST.AtomExpr implicitOne = loc(new AST.AtomExpr(AST.AtomExpr.Type.INTCONST, "1"), start, start);
			AST.Init init = new AST.Init(implicitOne, c);
			return loc(init, start, lastToken);
		}
	}

	//const -> INTCONST | CHARCONST | STRINGCONST

	private AST.AtomExpr parseConst() {
		Token t = lexAn.peekToken();
		Token.Symbol s = t.symbol();

		if (s == Token.Symbol.INTCONST || s == Token.Symbol.CHARCONST || s == Token.Symbol.STRINGCONST) {
			check(s);
			AST.AtomExpr.Type type = switch (s) {
				case INTCONST -> AST.AtomExpr.Type.INTCONST;
				case CHARCONST -> AST.AtomExpr.Type.CHRCONST;
				case STRINGCONST -> AST.AtomExpr.Type.STRCONST;
				default -> throw new Report.InternalError();
			};
			AST.AtomExpr expr = new AST.AtomExpr(type, t.lexeme());
			return loc(expr, t, t);
		} else {
			throw new Report.Error(t, "Expected a constant.");
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
				HashMap<AST.Node, Report.Locatable> attrLocMap = new HashMap<>();
				synAn.parse(attrLocMap);
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

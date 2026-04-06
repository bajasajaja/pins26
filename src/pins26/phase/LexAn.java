package pins26.phase;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import pins26.common.*;

/**
 * Leksikalni analizator.
 */
public class LexAn implements AutoCloseable {

	/** Izvorna datoteka. */
	private final Reader srcFile;
	private final Map<String,Token.Symbol> KEYWORDS = new HashMap<>();
	private void initKeywords() {
		KEYWORDS.put("var", Token.Symbol.VAR);
		KEYWORDS.put("if", Token.Symbol.IF);
		KEYWORDS.put("else", Token.Symbol.ELSE);
		KEYWORDS.put("fun",Token.Symbol.FUN);
		KEYWORDS.put("then",Token.Symbol.THEN);
		KEYWORDS.put("while",Token.Symbol.WHILE);
		KEYWORDS.put("do",Token.Symbol.DO);
		KEYWORDS.put("let",Token.Symbol.LET);
		KEYWORDS.put("in",Token.Symbol.IN);
		KEYWORDS.put("end",Token.Symbol.END);
		KEYWORDS.put("=",Token.Symbol.ASSIGN);
		KEYWORDS.put(",",Token.Symbol.COMMA);
		KEYWORDS.put("||",Token.Symbol.OR);
		KEYWORDS.put("&&",Token.Symbol.AND);
		KEYWORDS.put("!",Token.Symbol.NOT);
		KEYWORDS.put("==",Token.Symbol.EQU);
		KEYWORDS.put("!=",Token.Symbol.NEQ);
		KEYWORDS.put("<", Token.Symbol.GTH);
		KEYWORDS.put(">", Token.Symbol.LTH);
		KEYWORDS.put("<=", Token.Symbol.GEQ);
		KEYWORDS.put(">=", Token.Symbol.LEQ);
		KEYWORDS.put("+",Token.Symbol.ADD);
		KEYWORDS.put("-",Token.Symbol.SUB);
		KEYWORDS.put("*",Token.Symbol.MUL);
		KEYWORDS.put("/",Token.Symbol.DIV);
		KEYWORDS.put("%",Token.Symbol.MOD);
		KEYWORDS.put("^",Token.Symbol.PTR);
		KEYWORDS.put("(",Token.Symbol.LPAREN);
		KEYWORDS.put(")",Token.Symbol.RPAREN);
	}

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param srcFileName Ime izvorne datoteke.
	 */
	public LexAn(final String srcFileName) {
		try {
			srcFile = new BufferedReader(new InputStreamReader(new FileInputStream(new File(srcFileName))));
			nextChar(); // Pripravi prvi znak izvorne datoteke (glej {@link nextChar}).
			initKeywords();
		} catch (FileNotFoundException __) {
			throw new Report.Error("Source file '" + srcFileName + "' not found.");
		}
	}

	@Override
	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file.");
		}
	}

	/** Trenutni znak izvorne datoteke (glej {@link LexAn#nextChar}). */
	private int buffChar = -2;

	/** Vrstica trenutnega znaka izvorne datoteke (glej {@link LexAn#nextChar}). */
	private int buffCharLine = 0;

	/** Stolpec trenutnega znaka izvorne datoteke (glej {@link LexAn#nextChar}). */
	private int buffCharColumn = 0;

	/**
	 * Prebere naslednji znak izvorne datoteke.
	 * 
	 * Izvorno datoteko beremo znak po znak. Trenutni znak izvorne datoteke je
	 * shranjen v spremenljivki {@link LexAn#buffChar}, vrstica in stolpec trenutnega
	 * znaka izvorne datoteke sta shranjena v spremenljivkah {@link LexAn#buffCharLine} in
	 * {@link LexAn#buffCharColumn}.
	 * 
	 * Zacetne vrednosti {@link LexAn#buffChar}, {@link LexAn#buffCharLine} in
	 * {@link LexAn#buffCharColumn} so {@code '\n'}, {@code 0} in {@code 0}: branje prvega
	 * znaka izvorne datoteke bo na osnovi vrednosti {@code '\n'} spremenljivke
	 * {@link LexAn#buffChar} prvemu znaku izvorne datoteke priredilo vrstico 1 in stolpec
	 * 1.
	 * 
	 * Pri branju izvorne datoteke se predpostavlja, da je v spremenljivki
	 * {@link LexAn#buffChar} ves"cas veljaven znak. Zunaj metode {@link LexAn#nextChar} so vse
	 * spremenljivke {@link LexAn#buffChar}, {@link LexAn#buffCharLine} in
	 * {@link LexAn#buffCharColumn} namenjene le branju.
	 * 
	 * Vrednost {@code -1} v spremenljivki {@link LexAn#buffChar} pomeni konec datoteke
	 * (vrednosti spremenljivk {@link LexAn#buffCharLine} in {@link LexAn#buffCharColumn} pa
	 * nista ve"c veljavni).
	 */
	private void nextChar() {
		try {
			switch (buffChar) {
			case -2: // Noben znak "se ni bil prebran.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? 0 : 1;
				buffCharColumn = buffChar == -1 ? 0 : 1;
				return;
			case -1: // Konec datoteke je bil "ze viden.
				return;
			case '\n': // Prejsnji znak je koncal vrstico, zacne se nova vrstica.
				buffChar = srcFile.read();
				buffCharLine = buffChar == -1 ? buffCharLine : buffCharLine + 1;
				buffCharColumn = buffChar == -1 ? buffCharColumn : 1;
				return;
			case '\t': // Prejsnji znak je tabulator, ta znak je morda potisnjen v desno.
				buffChar = srcFile.read();
				while (buffCharColumn % 4 != 0)
					buffCharColumn += 1;
				buffCharColumn += 1;
				return;
			default: // Prejsnji znak je brez posebnosti.
				buffChar = srcFile.read();
				buffCharColumn += 1;
				return;
			}
		} catch (IOException __) {
			throw new Report.Error("Cannot read source file.");
		}
	}

	/**
	 * Trenutni leksikalni simbol.
	 * "Ce vrednost spremenljivke {@code buffToken} ni {@code null}, je simbol "ze
	 * prebran iz vhodne datoteke, ni pa "se predan naprej sintaksnemu analizatorju.
	 * Ta simbol je dostopen z metodama {@link LexAn#peekToken} in {@link LexAn#takeToken}.
	 */
	private Token buffToken = null;

	/**
	 * Prebere naslednji leksikalni simbol, ki je nato dostopen preko metod
	 * {@link LexAn#peekToken} in {@link LexAn#takeToken}.
	 */
	private void nextToken() {
		while(Character.isWhitespace((char)buffChar)) {
			nextChar();
		}
		if(buffChar == -1) {
			buffToken = new Token(new Report.Location(0, 0), Token.Symbol.EOF, "");
			return;
		}
		Report.Location location = new Report.Location(buffCharLine, buffCharColumn);
		StringBuilder buffer = new StringBuilder();
		buffer.append((char)buffChar);
		if(Character.toString(buffChar).matches("[A-Za-z_]")){
			nextChar();
			while (buffChar != -1 && Character.toString(buffChar).matches("[A-Za-z0-9_]")) {
				buffer.append((char)buffChar);
				nextChar();
			}
			if (!Character.isWhitespace(buffChar) && buffChar != -1 && !Character.toString(buffChar).matches("[-+*/%^(),=<>!&|]")) {
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Forbidden character in identifier: " + (char)buffChar);
			}
			String type = buffer.toString();
			Token.Symbol symbol = KEYWORDS.get(type);
            buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), Objects.requireNonNullElse(symbol, Token.Symbol.IDENTIFIER), type);
			return;
		}
		if(Character.toString(buffChar).matches("[-+*/%^(),]")) {
			Token.Symbol symbol = KEYWORDS.get(Character.toString(buffChar));
			buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), symbol, Character.toString(buffChar));
			nextChar();
			return;
		}

		if (Character.toString(buffChar).matches("[&|]")){
			char current = (char)buffChar;
			Token.Symbol symbol = KEYWORDS.get(Character.toString(current));
			nextChar();
			if(buffChar == current) {
				buffer.append(current);
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), symbol, buffer.toString());
			} else{
				throw new Report.Error(new Report.Location(buffCharLine, buffCharColumn), "Forbidden character in operator: " + (char)buffChar + "for operation " + symbol);
			}
			nextChar();
			return;
		}
		if (Character.toString(buffChar).matches("[!=<>]")) {
			char current = (char) buffChar;
			Token.Symbol currentSymbol = KEYWORDS.get(Character.toString(current));
			nextChar();
			if(buffChar == '='){
				buffer.append((char)buffChar);
				Token.Symbol symbol = KEYWORDS.get(buffer.toString());
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), symbol, buffer.toString());
				nextChar();
			} else{
				buffToken = new Token(new Report.Location(buffCharLine, buffCharColumn), currentSymbol, Character.toString(current));
			}
			return;
		}

	}

	/**
	 * Vrne trenutni leksikalni simbol, ki ostane v lastnistvu leksikalnega
	 * analizatorja.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token peekToken() {
		if (buffToken == null)
			nextToken();
		return buffToken;
	}

	/**
	 * Vrne trenutni leksikalni simbol, ki preide v lastnistvo klicoce kode.
	 * 
	 * @return Leksikalni simbol.
	 */
	public Token takeToken() {
		if (buffToken == null)
			nextToken();
		final Token thisToken = buffToken;
		buffToken = null;
		return thisToken;
	}

	// --- ZAGON ---

	/**
	 * Zagon leksikalnega analizatorja kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (lexical analysis):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (LexAn lexAn = new LexAn(cmdLineArgs[0])) {
				while (lexAn.peekToken().symbol() != Token.Symbol.EOF)
					System.out.println(lexAn.takeToken());
				System.out.println(lexAn.takeToken());
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

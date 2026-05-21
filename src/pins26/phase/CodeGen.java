package pins26.phase;

import java.util.*;

import pins26.common.*;

/**
 * Generiranje kode.
 */
public class CodeGen {

	@SuppressWarnings({ "doclint:missing" })
	public CodeGen() {
		throw new Report.InternalError();
	}

	/**
	 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 * predstavitve.
	 * 
	 * Atributi:
	 * <ol>
	 * <li>({@link Abstr}) lokacija kode, ki pripada posameznemu vozliscu;</li>
	 * <li>({@link SemAn}) definicija uporabljenega imena;</li>
	 * <li>({@link SemAn}) ali je dani izraz levi izraz;</li>
	 * <li>({@link Memory}) klicni zapis funkcije;</li>
	 * <li>({@link Memory}) dostop do parametra;</li>
	 * <li>({@link Memory}) dostop do spremenljivke;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo kodo programa;</li>
	 * <li>({@link CodeGen}) seznam ukazov, ki predstavljajo podatke programa.</li>
	 * </ol>
	 */
	public static class AttrAST extends Memory.AttrAST {

		/** Atribut: seznam ukazov, ki predstavljajo kodo programa. */
		public final Map<AST.Node, List<PDM.CodeInstr>> attrCode;

		/** Atribut: seznam ukazov, ki predstavljajo podatke programa. */
		public final Map<AST.Node, List<PDM.DataInstr>> attrData;

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST  Abstraktno sintaksno drevo z dodanimi atributi pomnilniske
		 *                 predstavitve.
		 * @param attrCode Attribut: seznam ukazov, ki predstavljajo kodo programa.
		 * @param attrData Attribut: seznam ukazov, ki predstavljajo podatke programa.
		 */
		public AttrAST(final Memory.AttrAST attrAST, final Map<AST.Node, List<PDM.CodeInstr>> attrCode,
				final Map<AST.Node, List<PDM.DataInstr>> attrData) {
			super(attrAST);
			this.attrCode = attrCode;
			this.attrData = attrData;
		}

		/**
		 * Ustvari novo abstraktno sintaksno drevo z dodanimi atributi generiranja kode.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi generiranja
		 *                kode.
		 */
		public AttrAST(final AttrAST attrAST) {
			super(attrAST);
			this.attrCode = attrAST.attrCode;
			this.attrData = attrAST.attrData;
		}

		@Override
		public String head(final AST.Node node, final boolean highlighted) {
			final StringBuffer head = new StringBuffer();
			head.append(super.head(node, false));
			return head.toString();
		}

		@Override
		public void desc(final int indent, final AST.Node node, final boolean highlighted) {
			super.desc(indent, node, false);
			System.out.print(highlighted ? "\033[31m" : "");
			if (attrCode.get(node) != null) {
				List<PDM.CodeInstr> instrs = attrCode.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Code: ---\n");
					for (final PDM.CodeInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			if (attrData.get(node) != null) {
				List<PDM.DataInstr> instrs = attrData.get(node);
				if (instrs != null) {
					if (indent > 0)
						System.out.printf("%" + indent + "c", ' ');
					System.out.printf("--- Data: ---\n");
					for (final PDM.DataInstr instr : instrs) {
						if (indent > 0)
							System.out.printf("%" + indent + "c", ' ');
						System.out.println((instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
					}
				}
			}
			System.out.print(highlighted ? "\033[30m" : "");
			return;
		}

	}

	/**
	 * Izracuna kodo programa
	 * 
	 * @param memoryAttrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
	 *                      pomnilniske predstavitve.
	 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
	 *         predstavitve.
	 */
	public static AttrAST generate(final Memory.AttrAST memoryAttrAST) {
		AttrAST attrAST = new AttrAST(memoryAttrAST, new HashMap<AST.Node, List<PDM.CodeInstr>>(),
				new HashMap<AST.Node, List<PDM.DataInstr>>());
		(new CodeGenerator(attrAST)).generate();
		return attrAST;
	}

	/**
	 * Generiranje kode v abstraktnem sintaksnem drevesu.
	 */
	private static class CodeGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Stevec anonimnih label. */
		private int labelCounter = 0;

		/**
		 * Ustvari nov generator kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Sprozi generiranje kode v abstraktnem sintaksnem drevesu.
		 * 
		 * @return Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 *         predstavitve.
		 */
		public AttrAST generate() {
			attrAST.ast.accept(new Generator(), null);
			return new AttrAST(attrAST, Collections.unmodifiableMap(attrAST.attrCode),
					Collections.unmodifiableMap(attrAST.attrData));
		}

		/** Obiskovalec, ki generira kodo v abstraktnem sintaksnem drevesu. */
		private class Generator implements AST.FullVisitor<List<PDM.CodeInstr>, Mem.Frame> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public List<PDM.CodeInstr> visit(final AST.Nodes<? extends AST.Node> nodes , final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				int size = nodes.size();
				for (int i = 0; i < size; i++) {
					AST.Node node = nodes.get(i);
					node.accept(this, frame);
					List<PDM.CodeInstr> nodeCode = attrAST.attrCode.get(node);
					if (nodeCode != null) {
						code.addAll(nodeCode);
					}
					// Only keep the value of the last statement in block/function, pop intermediate ones
					if (node instanceof AST.Stmt && i < size - 1) {
						code.add(new PDM.PUSH(4, null));
						code.add(new PDM.POPN(null));
					}
				}
				attrAST.attrCode.put(nodes, code);
				return code;

			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.FunDef funDef, final Mem.Frame frame) {
				Mem.Frame current = attrAST.attrFrame.get(funDef);
				List<PDM.CodeInstr> code = new ArrayList<>();

				code.add(new PDM.LABEL(current.name,attrAST.attrLoc.get(funDef)));

				if(current.varsSize > 0){
					//reserve space for local vars,by shifting the SP
					code.add(new PDM.PUSH(-current.varsSize,attrAST.attrLoc.get(funDef)));
					code.add(new PDM.POPN(attrAST.attrLoc.get(funDef)));
				}
				funDef.pars.accept(this, current);
				funDef.stmts.accept(this, current);

				List<PDM.CodeInstr> stmtCode = attrAST.attrCode.get(funDef.stmts);
				if (stmtCode != null) {
					code.addAll(stmtCode);
				}
				//return value of last stmt
				code.add(new PDM.PUSH(current.parsSize, attrAST.attrLoc.get(funDef)));
				code.add(new PDM.RETN(current,attrAST.attrLoc.get(funDef)));

				attrAST.attrCode.put(funDef, code);
				return code;
			}
			@Override
			//we put them on the stack,but we dont need to genereta code for them yet,so we return null
			public List<PDM.CodeInstr> visit(final AST.ParDef parDef, final Mem.Frame frame) {
				return null;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.VarDef varDef, final Mem.Frame frame) {
				Mem.Access access = attrAST.attrVarAccess.get(varDef);
				List<PDM.CodeInstr> code = new ArrayList<>();
				List<PDM.DataInstr> data = new ArrayList<>();
				//globals
				if(access instanceof Mem.AbsAccess absAcc){
					data.add(new PDM.LABEL(absAcc.name,attrAST.attrLoc.get(varDef)));
					data.add(new PDM.SIZE(absAcc.size,attrAST.attrLoc.get(varDef)));
				}
				if(varDef.inits.size() > 0){
					String initLabel = "init_" + (++labelCounter);

					data.add(new PDM.LABEL(initLabel,attrAST.attrLoc.get(varDef)));
					data.add(new PDM.DATA(varDef.inits.size(),attrAST.attrLoc.get(varDef)));

					for (AST.Init init : varDef.inits) {
						int num = Integer.parseInt(init.num.value);
						data.add(new PDM.DATA(num,attrAST.attrLoc.get(init)));
						if(init.value.type == AST.AtomExpr.Type.STRCONST){
							String value = init.value.value.substring(1,init.value.value.length()-1);
							data.add(new PDM.DATA(value.length(),attrAST.attrLoc.get(init)));
							for (int i = 0; i < value.length(); i++) {
								data.add(new PDM.DATA((int)value.charAt(i),attrAST.attrLoc.get(init)));
							}
							data.add(new PDM.DATA(0,attrAST.attrLoc.get(init)));
						} else {
							data.add(new PDM.DATA(1,attrAST.attrLoc.get(init)));
							int charORInt = (init.value.type == AST.AtomExpr.Type.INTCONST)
									? Integer.parseInt(init.value.value)
									: init.value.value.charAt(1);
							data.add(new PDM.DATA(charORInt,attrAST.attrLoc.get(init)));

						}
					}
					if(access instanceof Mem.AbsAccess absAcc){
						code.add(new PDM.NAME(absAcc.name,attrAST.attrLoc.get(varDef)));
					} else if (access instanceof Mem.RelAccess relAcc) {
						code.add(new PDM.REGN(PDM.REGN.Reg.FP,attrAST.attrLoc.get(varDef)));
						code.add(new PDM.PUSH(relAcc.offset,attrAST.attrLoc.get(varDef)));
						code.add(new PDM.OPER(PDM.OPER.Oper.ADD,attrAST.attrLoc.get(varDef)));
					}

					code.add(new PDM.NAME(initLabel,attrAST.attrLoc.get(varDef)));
					code.add(new PDM.INIT(attrAST.attrLoc.get(varDef)));
				}
			if(!data.isEmpty()) attrAST.attrData.put(varDef, data);
			if(!code.isEmpty()) attrAST.attrCode.put(varDef, code);
			return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.Init init, final Mem.Frame frame) {
				return null;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.ExprStmt exprStmt, final Mem.Frame frame) {
				exprStmt.expr.accept(this, frame);
				List<PDM.CodeInstr> code = new ArrayList<>(attrAST.attrCode.get(exprStmt.expr));
				attrAST.attrCode.put(exprStmt, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.AssignStmt assignStmt, final Mem.Frame frame) {
				assignStmt.srcExpr.accept(this, frame);
				assignStmt.dstExpr.accept(this, frame);

				List<PDM.CodeInstr> code = new ArrayList<>();
				code.addAll(attrAST.attrCode.get(assignStmt.srcExpr));
				code.addAll(attrAST.attrCode.get(assignStmt.dstExpr));
				code.add(new PDM.SAVE(attrAST.attrLoc.get(assignStmt)));

				//dummy 0
				code.add(new PDM.PUSH(0,attrAST.attrLoc.get(assignStmt)));

				attrAST.attrCode.put(assignStmt, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.IfStmt ifStmt, final Mem.Frame frame) {
				ifStmt.cond.accept(this,frame);
				ifStmt.thenStmts.accept(this,frame);
				ifStmt.elseStmts.accept(this,frame);
				List<PDM.CodeInstr> code = new ArrayList<>();
				String thenLabel = "then_" + (++labelCounter);
				String elseLabel = "else_" + (++labelCounter);
				String endLabel = "end_" + (++labelCounter);

				code.addAll(attrAST.attrCode.get(ifStmt.cond));
				//not sure here if i have to do .cond and stuff,will check later
				code.add(new PDM.NAME(thenLabel,attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.NAME(elseLabel,attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.CJMP(attrAST.attrLoc.get(ifStmt)));

				code.add(new PDM.LABEL(thenLabel,attrAST.attrLoc.get(ifStmt)));
				List<PDM.CodeInstr> thenCode = attrAST.attrCode.get(ifStmt.thenStmts);

				if(thenCode != null) code.addAll(thenCode);

				code.add(new PDM.NAME(endLabel,attrAST.attrLoc.get(ifStmt)));
				code.add(new PDM.UJMP(attrAST.attrLoc.get(ifStmt)));

				code.add(new PDM.LABEL(elseLabel,attrAST.attrLoc.get(ifStmt)));
				List<PDM.CodeInstr> elseCode = attrAST.attrCode.get(ifStmt.elseStmts);
				if (elseCode != null) code.addAll(elseCode);

				code.add(new PDM.LABEL(endLabel,attrAST.attrLoc.get(ifStmt)));

				//dummy 0
				code.add(new PDM.PUSH(0,attrAST.attrLoc.get(ifStmt)));

				attrAST.attrCode.put(ifStmt, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.LetStmt letStmt,final Mem.Frame frame){
				letStmt.defs.accept(this,frame);
				letStmt.stmts.accept(this,frame);

				List<PDM.CodeInstr> code = new ArrayList<>();
				List<PDM.CodeInstr> defsCode = attrAST.attrCode.get(letStmt.defs);
				if (defsCode != null) code.addAll(defsCode);

				List<PDM.CodeInstr> stmtsCode = attrAST.attrCode.get(letStmt.stmts);
				if (stmtsCode != null) {
					code.addAll(stmtsCode);
				} else {
					code.add(new PDM.PUSH(0, attrAST.attrLoc.get(letStmt)));
				}

				attrAST.attrCode.put(letStmt, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.AtomExpr atomExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();

				if (atomExpr.type == AST.AtomExpr.Type.STRCONST) {
					List<PDM.DataInstr> data = new ArrayList<>();
					String strLabel = "str_" + (++labelCounter);
					data.add(new PDM.LABEL(strLabel,attrAST.attrLoc.get(atomExpr)));
					String value = atomExpr.value.substring(1,atomExpr.value.length() - 1);
					for (int i = 0; i < value.length(); i++) {
						data.add(new PDM.DATA((int) value.charAt(i), attrAST.attrLoc.get(atomExpr)));
					}
					data.add(new PDM.DATA(0, attrAST.attrLoc.get(atomExpr)));
					attrAST.attrData.put(atomExpr, data);
					code.add(new PDM.NAME(strLabel,attrAST.attrLoc.get(atomExpr)));

				}else if (atomExpr.type == AST.AtomExpr.Type.CHRCONST) {
					int charVal = atomExpr.value.charAt(1);
					code.add(new PDM.PUSH(charVal, attrAST.attrLoc.get(atomExpr)));
				} else {
					int intVal = Integer.parseInt(atomExpr.value);
					code.add(new PDM.PUSH(intVal, attrAST.attrLoc.get(atomExpr)));
				}

				attrAST.attrCode.put(atomExpr, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.UnExpr unExpr, final Mem.Frame frame) {
				unExpr.expr.accept(this,frame);
				List<PDM.CodeInstr> code = new ArrayList<>(attrAST.attrCode.get(unExpr.expr));

				switch (unExpr.oper){
					case ADD -> {}
					case SUB -> code.add(new PDM.OPER(PDM.OPER.Oper.NEG, attrAST.attrLoc.get(unExpr)));
					case NOT -> code.add(new PDM.OPER(PDM.OPER.Oper.NOT, attrAST.attrLoc.get(unExpr)));
					case MEMADDR -> {}
					case VALUEAT -> {
						Boolean lval = attrAST.attrLVal.get(unExpr);
						if (lval == null || !lval){
							code.add(new PDM.LOAD(attrAST.attrLoc.get(unExpr)));
						}
					}
				}
				attrAST.attrCode.put(unExpr, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.BinExpr binExpr, final Mem.Frame frame) {
				binExpr.fstExpr.accept(this, frame);
				binExpr.sndExpr.accept(this, frame);

				List<PDM.CodeInstr> code = new ArrayList<>();
				code.addAll(attrAST.attrCode.get(binExpr.fstExpr));
				code.addAll(attrAST.attrCode.get(binExpr.sndExpr));

				switch (binExpr.oper) {
					case ADD -> code.add(new PDM.OPER(PDM.OPER.Oper.ADD, attrAST.attrLoc.get(binExpr)));
					case SUB -> code.add(new PDM.OPER(PDM.OPER.Oper.SUB, attrAST.attrLoc.get(binExpr)));
					case MUL -> code.add(new PDM.OPER(PDM.OPER.Oper.MUL, attrAST.attrLoc.get(binExpr)));
					case DIV -> code.add(new PDM.OPER(PDM.OPER.Oper.DIV, attrAST.attrLoc.get(binExpr)));
					case MOD -> code.add(new PDM.OPER(PDM.OPER.Oper.MOD, attrAST.attrLoc.get(binExpr)));
					case EQU -> code.add(new PDM.OPER(PDM.OPER.Oper.EQU, attrAST.attrLoc.get(binExpr)));
					case NEQ -> code.add(new PDM.OPER(PDM.OPER.Oper.NEQ, attrAST.attrLoc.get(binExpr)));
					case LTH -> code.add(new PDM.OPER(PDM.OPER.Oper.LTH, attrAST.attrLoc.get(binExpr)));
					case GTH -> code.add(new PDM.OPER(PDM.OPER.Oper.GTH, attrAST.attrLoc.get(binExpr)));
					case LEQ -> code.add(new PDM.OPER(PDM.OPER.Oper.LEQ, attrAST.attrLoc.get(binExpr)));
					case GEQ -> code.add(new PDM.OPER(PDM.OPER.Oper.GEQ, attrAST.attrLoc.get(binExpr)));
					case AND -> code.add(new PDM.OPER(PDM.OPER.Oper.AND, attrAST.attrLoc.get(binExpr)));
					case OR  -> code.add(new PDM.OPER(PDM.OPER.Oper.OR, attrAST.attrLoc.get(binExpr)));
				}

				attrAST.attrCode.put(binExpr, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.VarExpr varExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				AST.Def def = attrAST.attrDef.get(varExpr);
				Boolean holdsLeftVal = attrAST.attrLVal.get(varExpr);

				Mem.Access access = null;
				if (def instanceof AST.VarDef varDef) {
					access = attrAST.attrVarAccess.get(varDef);
				} else if (def instanceof AST.ParDef parDef) {
					access = attrAST.attrParAccess.get(parDef);
				}

				if(access instanceof  Mem.AbsAccess absAcc){
					code.add(new PDM.NAME(absAcc.name,attrAST.attrLoc.get(varExpr)));
				} else if (access instanceof  Mem.RelAccess relAcc){
					code.add(new PDM.REGN(PDM.REGN.Reg.FP,attrAST.attrLoc.get(varExpr)));
					for (int i = 0; i < frame.depth - relAcc.depth; i++) {
						code.add(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
					}
					code.add(new PDM.PUSH(relAcc.offset,  attrAST.attrLoc.get(varExpr)));
					code.add(new PDM.OPER(PDM.OPER.Oper.ADD, attrAST.attrLoc.get(varExpr)));
				}
				if(holdsLeftVal == null || !holdsLeftVal){
					code.add(new PDM.LOAD(attrAST.attrLoc.get(varExpr)));
				}
				attrAST.attrCode.put(varExpr, code);
				return code;
			}
			@Override
			public List<PDM.CodeInstr> visit(final AST.CallExpr callExpr, final Mem.Frame frame) {
				List<PDM.CodeInstr> code = new ArrayList<>();
				AST.Def def = attrAST.attrDef.get(callExpr);

				List<AST.Expr> argsList = callExpr.args.getAll();
				//right to left
				for (int i = argsList.size() - 1; i >= 0 ; i--) {
					argsList.get(i).accept(this, frame);
					code.addAll(attrAST.attrCode.get(argsList.get(i)));
				}
				if(def instanceof AST.FunDef funDef){
					Mem.Frame target = attrAST.attrFrame.get(funDef);
					code.add(new PDM.REGN(PDM.REGN.Reg.FP,attrAST.attrLoc.get(callExpr)));

					for (int i = 0; i < frame.depth - target.depth; i++) {
						code.add(new PDM.LOAD(attrAST.attrLoc.get(callExpr)));
					}
					code.add(new PDM.NAME(target.name, attrAST.attrLoc.get(callExpr)));
					code.add(new PDM.CALL(target, attrAST.attrLoc.get(callExpr)));
				}
				attrAST.attrCode.put(callExpr, code);
				return code;

			}
		}
	}

	/**
	 * Generator seznama ukazov, ki predstavljajo kodo programa.
	 */
	public static class CodeSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov za inicializacijo staticnih spremenljivk. */
		private final Vector<PDM.CodeInstr> codeInitSegment = new Vector<PDM.CodeInstr>();

		/** Seznam ukazov funkcij. */
		private final Vector<PDM.CodeInstr> codeFunsSegment = new Vector<PDM.CodeInstr>();

		/** Klicni zapis funkcije {@code main}. */
		private Mem.Frame main = null;

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo kodo programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public CodeSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo kodo programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo kodo programa.
		 */
		public List<PDM.CodeInstr> codeSegment() {
			attrAST.ast.accept(new Generator(), null);
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("main", null));
			codeInitSegment.addLast(new PDM.CALL(main, null));
			codeInitSegment.addLast(new PDM.PUSH(0, null));
			codeInitSegment.addLast(new PDM.NAME("exit", null));
			codeInitSegment.addLast(new PDM.CALL(null, null));
			final Vector<PDM.CodeInstr> codeSegment = new Vector<PDM.CodeInstr>();
			codeSegment.addAll(codeInitSegment);
			codeSegment.addAll(codeFunsSegment);
			return Collections.unmodifiableList(codeSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo kodo programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.FunDef funDef, final Object arg) {
				if (funDef.stmts.size() == 0)
					return null;
				List<PDM.CodeInstr> code = attrAST.attrCode.get(funDef);
				codeFunsSegment.addAll(code);
				funDef.pars.accept(this, arg);
				funDef.stmts.accept(this, arg);
				switch (funDef.name) {
				case "main" -> main = attrAST.attrFrame.get(funDef);
				}
				return null;
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				switch (attrAST.attrVarAccess.get(varDef)) {
				case Mem.AbsAccess __: {
					List<PDM.CodeInstr> code = attrAST.attrCode.get(varDef);
					codeInitSegment.addAll(code);
					break;
				}
				case Mem.RelAccess __: {
					break;
				}
				default:
					throw new Report.InternalError();
				}
				return null;
			}

		}

	}

	/**
	 * Generator seznama ukazov, ki predstavljajo podatke programa.
	 */
	public static class DataSegmentGenerator {

		/**
		 * Abstraktno sintaksno drevo z dodanimi atributi izracuna pomnilniske
		 * predstavitve.
		 */
		private final AttrAST attrAST;

		/** Seznam ukazov, ki predstavljajo podatke programa. */
		private final Vector<PDM.DataInstr> dataSegment = new Vector<PDM.DataInstr>();

		/**
		 * Ustvari nov generator seznama ukazov, ki predstavljajo podatke programa.
		 *
		 * @param attrAST Abstraktno sintaksno drevo z dodanimi atributi izracuna
		 *                pomnilniske predstavitve.
		 */
		public DataSegmentGenerator(final AttrAST attrAST) {
			this.attrAST = attrAST;
		}

		/**
		 * Izracuna seznam ukazov, ki predstavljajo podatke programa.
		 * 
		 * @return Seznam ukazov, ki predstavljajo podatke programa.
		 */
		public List<PDM.DataInstr> dataSegment() {
			attrAST.ast.accept(new Generator(), null);
			return Collections.unmodifiableList(dataSegment);
		}

		/**
		 * Obiskovalec, ki izracuna seznam ukazov, ki predstavljajo podatke programa.
		 */
		private class Generator implements AST.FullVisitor<Object, Object> {

			@SuppressWarnings({ "doclint:missing" })
			public Generator() {
			}

			@Override
			public Object visit(final AST.VarDef varDef, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(varDef);
				if (data != null)
					dataSegment.addAll(data);
				varDef.inits.accept(this, arg);
				return null;
			}

			@Override
			public Object visit(final AST.AtomExpr atomExpr, final Object arg) {
				List<PDM.DataInstr> data = attrAST.attrData.get(atomExpr);
				if (data != null)
					dataSegment.addAll(data);
				return null;
			}

		}

	}

	// --- ZAGON ---

	/**
	 * Zagon izracuna pomnilniske predstavitve kot samostojnega programa.
	 * 
	 * @param cmdLineArgs Argumenti v ukazni vrstici.
	 */
	public static void main(final String[] cmdLineArgs) {
		System.out.println("This is PINS'26 compiler (code generation):");

		try {
			if (cmdLineArgs.length == 0)
				throw new Report.Error("No source file specified in the command line.");
			if (cmdLineArgs.length > 1)
				Report.warning("Unused arguments in the command line.");

			try (SynAn synAn = new SynAn(cmdLineArgs[0])) {
				// abstraktna sintaksa:
				final Abstr.AttrAST abstrAttrAST = Abstr.constructAST(synAn);
				// semanticna analiza:
				final SemAn.AttrAST semanAttrAST = SemAn.analyze(abstrAttrAST);
				// pomnilniska predstavitev:
				final Memory.AttrAST memoryAttrAST = Memory.organize(semanAttrAST);
				// generiranje kode:
				final AttrAST codegenAttrAST = CodeGen.generate(memoryAttrAST);

				(new AST.Logger(codegenAttrAST)).log();
				{
					int addr = 0;
					final List<PDM.CodeInstr> codeSegment = (new CodeSegmentGenerator(codegenAttrAST)).codeSegment();
					{
						System.out.println("\n\033[1mCODE SEGMENT:\033[0m");
						for (final PDM.CodeInstr instr : codeSegment) {
							System.out.printf("%8d [%s] %s\n", addr, instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					final List<PDM.DataInstr> dataSegment = (new DataSegmentGenerator(codegenAttrAST)).dataSegment();
					{
						System.out.println("\n\033[1mDATA SEGMENT:\033[0m");
						for (final PDM.DataInstr instr : dataSegment) {
							System.out.printf("%8d [%s] %s\n", addr, (instr instanceof PDM.SIZE) ? " " : instr.size(),
									(instr instanceof PDM.LABEL ? "" : "  ") + instr.toString());
							addr += instr.size();
						}
					}
					System.out.println();
				}
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

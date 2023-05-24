package compile.syntax;

import compile.lexical.token.TokenList;
import compile.lexical.token.TokenType;
import compile.symbol.FuncSymbol;
import compile.symbol.LocalSymbol;
import compile.symbol.ParamSymbol;
import compile.symbol.SymbolTable;
import compile.syntax.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public class SyntaxParser {
    private boolean isProcessed;
    private final SymbolTable symbolTable;
    private final TokenList tokens;

    private RootAST rootAST;

    public SyntaxParser(SymbolTable symbolTable, TokenList tokens) {
        this.symbolTable = symbolTable;
        this.tokens = tokens;
    }

    private void checkIfIsProcessed() {
        if (isProcessed) {
            return;
        }
        isProcessed = true;
        parseRoot();
    }

    private void parseRoot() {
        List<CompUnitAST> compUnits = new ArrayList<>();
        while (tokens.hasNext()) {
            switch (tokens.peekType()) {
                case CONST -> compUnits.addAll(parseConstDecl());
                case FLOAT, INT, VOID -> {
                    if (tokens.peekType(2) == TokenType.LP) {
                        compUnits.add(parseFuncDef());
                    } else {
                        compUnits.addAll(parseGlobalDecl());
                    }
                }
                default -> throw new RuntimeException();
            }
        }
        rootAST = new RootAST(compUnits);
    }

    private List<ConstDefAST> parseConstDecl() {
        List<ConstDefAST> constDefs = new ArrayList<>();
        tokens.expectAndFetch(TokenType.CONST);
        boolean isFloat = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        do {
            ConstDefAST constDef = parseConstDef(isFloat);
            constDefs.add(constDef);
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return constDefs;
    }

    private ConstDefAST parseConstDef(boolean isFloat) {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<Integer> dimensions = parseDimensionDef();
            tokens.expectAndFetch(TokenType.ASSIGN);
            InitValAST initVal = parseInitVal();
            Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
            return new ConstDefAST(symbolTable.makeConst(isFloat, name, dimensions,
                    exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                            exp -> exp.getValue().calc()))));
        }
        tokens.expectAndFetch(TokenType.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        Number value = rVal.calc();
        if (isFloat) {
            return new ConstDefAST(symbolTable.makeConst(name, value.floatValue()));
        } else {
            return new ConstDefAST(symbolTable.makeConst(name, value.intValue()));
        }
    }

    private List<GlobalDefAST> parseGlobalDecl() {
        boolean isFloat = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        List<GlobalDefAST> globalDefs = new ArrayList<>();
        do {
            GlobalDefAST globalDef = parseGlobalDef(isFloat);
            globalDefs.add(globalDef);
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return globalDefs;
    }

    private GlobalDefAST parseGlobalDef(boolean isFloat) {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<Integer> dimensions = parseDimensionDef();
            if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
                InitValAST initVal = parseInitVal();
                Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
                return new GlobalDefAST(symbolTable.makeGlobal(isFloat, name, dimensions,
                        exps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                exp -> exp.getValue().calc()))));
            } else {
                return new GlobalDefAST(symbolTable.makeGlobal(isFloat, name, dimensions, new HashMap<>()));
            }
        }
        if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
            ExpAST rVal = parseAddSubExp();
            if (isFloat) {
                return new GlobalDefAST(symbolTable.makeGlobal(name, rVal.calc().floatValue()));
            } else {
                return new GlobalDefAST(symbolTable.makeGlobal(name, rVal.calc().intValue()));
            }
        }
        return new GlobalDefAST(symbolTable.makeGlobal(name, 0));
    }

    private List<StmtAST> parseLocalDecl() {
        boolean isFloat = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        List<StmtAST> stmts = new ArrayList<>();
        do {
            List<StmtAST> newStmts = parseLocalDef(isFloat);
            stmts.addAll(newStmts);
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return stmts;
    }

    private List<StmtAST> parseLocalDef(boolean isFloat) {
        List<StmtAST> stmts = new ArrayList<>();
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<Integer> dimensions = parseDimensionDef();
            LocalSymbol symbol = symbolTable.makeLocal(isFloat, name, dimensions);
            stmts.add(new LocalDefAST(symbol));
            if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
                InitValAST initVal = parseInitVal();
                Map<Integer, ExpAST> exps = allocInitVal(initVal, dimensions);
                int totalSize = dimensions.stream().reduce(4, Math::multiplyExact);
                stmts.add(new ExpStmtAST(new FuncCallExpAST(symbolTable.getFunc("memset"),
                        List.of(new VarExpAST(symbol, List.of()), new IntLitExpAST(0), new IntLitExpAST(totalSize)))));
                for (Map.Entry<Integer, ExpAST> exp : exps.entrySet()) {
                    ExpAST[] dimensionExps = new ExpAST[dimensions.size()];
                    int t = exp.getKey();
                    for (int j = dimensions.size() - 1; j >= 0; j--) {
                        dimensionExps[j] = new IntLitExpAST(t % dimensions.get(j));
                        t /= dimensions.get(j);
                    }
                    stmts.add(new AssignStmtAST(new LValAST(symbol, List.of(dimensionExps)), exp.getValue()));
                }
            }
        } else {
            LocalSymbol symbol = symbolTable.makeLocal(isFloat, name);
            stmts.add(new LocalDefAST(symbol));
            if (tokens.matchAndThenThrow(TokenType.ASSIGN)) {
                LValAST lVal = new LValAST(symbol, List.of());
                ExpAST rVal = parseAddSubExp();
                stmts.add(new AssignStmtAST(lVal, rVal));
            }
        }
        return stmts;
    }

    private List<Integer> parseDimensionDef() {
        List<Integer> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            dimensions.add(parseAddSubExp().calc().intValue());
            tokens.expectAndFetch(TokenType.RB);
        }
        return dimensions;
    }

    private CompUnitAST parseFuncDef() {
        boolean hasRet, isFloat;
        switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT, TokenType.VOID).getType()) {
            case FLOAT -> {
                hasRet = true;
                isFloat = true;
            }
            case INT -> {
                hasRet = true;
                isFloat = false;
            }
            case VOID -> {
                hasRet = false;
                isFloat = false;
            }
            default -> throw new RuntimeException();
        }
        String name = tokens.nextIdentity();
        FuncSymbol decl = hasRet ? symbolTable.makeFunc(isFloat, name) : symbolTable.makeFunc(name);
        symbolTable.in();
        parseFuncDefParams(decl);
        BlockStmtAST body = parseBlock();
        symbolTable.out();
        return new FuncDefAST(decl, body);
    }

    private void parseFuncDefParams(FuncSymbol func) {
        tokens.expectAndFetch(TokenType.LP);
        while (!tokens.matchAndThenThrow(TokenType.RP)) {
            func.addParam(parseFuncDefParam());
            if (tokens.matchAndThenThrow(TokenType.RP)) {
                break;
            }
            tokens.expectAndFetch(TokenType.COMMA);
        }
    }

    private ParamSymbol parseFuncDefParam() {
        boolean isFloat = switch (tokens.expectAndFetch(TokenType.FLOAT, TokenType.INT).getType()) {
            case FLOAT -> true;
            case INT -> false;
            default -> throw new RuntimeException();
        };
        String name = tokens.nextIdentity();
        if (!tokens.matchAndThenThrow(TokenType.LB)) {
            return symbolTable.makeParam(isFloat, name);
        }
        tokens.expectAndFetch(TokenType.RB);
        List<Integer> dimensions = new ArrayList<>();
        dimensions.add(-1);
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            ExpAST exp = parseAddSubExp();
            dimensions.add(exp.calc().intValue());
            tokens.matchAndThenThrow(TokenType.RB);
        }
        return symbolTable.makeParam(isFloat, name, dimensions);
    }

    private BlockStmtAST parseBlock() {
        List<StmtAST> stmts = new ArrayList<>();
        tokens.expectAndFetch(TokenType.LC);
        while (!tokens.matchAndThenThrow(TokenType.RC)) {
            stmts.addAll(parseStmt());
        }
        return new BlockStmtAST(stmts);
    }

    private List<? extends StmtAST> parseStmt() {
        return switch (tokens.peekType()) {
            case BREAK -> List.of(parseBreakStmt());
            case CONST -> parseConstDecl();
            case CONTINUE -> List.of(parseContinueStmt());
            case FLOAT, INT -> parseLocalDecl();
            case ID -> {
                int lookahead = 0;
                boolean isAssignStmt = false;
                while (tokens.peekType(lookahead) != TokenType.SEMICOLON) {
                    if (tokens.peekType(lookahead) == TokenType.ASSIGN) {
                        isAssignStmt = true;
                        break;
                    }
                    lookahead++;
                }
                if (isAssignStmt) {
                    yield List.of(parseAssignStmt());
                } else {
                    yield List.of(parseExpStmt());
                }
            }
            case IF -> {
                symbolTable.in();
                IfStmtAST ifStmt = parseIfStmt();
                symbolTable.out();
                yield List.of(ifStmt);
            }
            case LC -> {
                symbolTable.in();
                BlockStmtAST blockStmt = parseBlock();
                symbolTable.out();
                yield List.of(blockStmt);
            }
            case RETURN -> List.of(parseRetStmt());
            case SEMICOLON -> List.of(parseBlankStmt());
            case WHILE -> {
                symbolTable.in();
                WhileStmtAST whileStmt = parseWhileStmt();
                symbolTable.out();
                yield List.of(whileStmt);
            }
            default -> throw new RuntimeException();
        };
    }

    private ExpAST parseLOrExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseLAndExp());
        while (tokens.matchAndThenThrow(TokenType.L_OR)) {
            exps.add(parseLAndExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new BinaryExpAST(BinaryExpAST.Type.L_OR, root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseLAndExp() {
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseEqNeExp());
        while (tokens.matchAndThenThrow(TokenType.L_AND)) {
            exps.add(parseEqNeExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 1; i < exps.size(); i++) {
            root = new BinaryExpAST(BinaryExpAST.Type.L_AND, root, exps.get(i));
        }
        return root;
    }

    private ExpAST parseEqNeExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseGeGtLeLtExp());
        while (tokens.match(TokenType.EQ, TokenType.NE)) {
            types.add(switch (tokens.expectAndFetch(TokenType.EQ, TokenType.NE).getType()) {
                case EQ -> BinaryExpAST.Type.EQ;
                case NE -> BinaryExpAST.Type.NE;
                default -> throw new RuntimeException();
            });
            exps.add(parseGeGtLeLtExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseGeGtLeLtExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseAddSubExp());
        while (tokens.match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            types.add(switch (tokens.expectAndFetch(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT).getType()) {
                case GE -> BinaryExpAST.Type.GE;
                case GT -> BinaryExpAST.Type.GT;
                case LE -> BinaryExpAST.Type.LE;
                case LT -> BinaryExpAST.Type.LT;
                default -> throw new RuntimeException();
            });
            exps.add(parseAddSubExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseAddSubExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseMulDivModExp());
        while (tokens.match(TokenType.PLUS, TokenType.MINUS)) {
            types.add(switch (tokens.expectAndFetch(TokenType.PLUS, TokenType.MINUS).getType()) {
                case PLUS -> BinaryExpAST.Type.ADD;
                case MINUS -> BinaryExpAST.Type.SUB;
                default -> throw new RuntimeException();
            });
            exps.add(parseMulDivModExp());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseMulDivModExp() {
        List<BinaryExpAST.Type> types = new ArrayList<>();
        List<ExpAST> exps = new ArrayList<>();
        exps.add(parseUnary());
        while (tokens.match(TokenType.MUL, TokenType.DIV, TokenType.MOD)) {
            types.add(switch (tokens.expectAndFetch(TokenType.MUL, TokenType.DIV, TokenType.MOD).getType()) {
                case MUL -> BinaryExpAST.Type.MUL;
                case DIV -> BinaryExpAST.Type.DIV;
                case MOD -> BinaryExpAST.Type.MOD;
                default -> throw new RuntimeException();
            });
            exps.add(parseUnary());
        }
        ExpAST root = exps.get(0);
        for (int i = 0; i < types.size(); i++) {
            root = new BinaryExpAST(types.get(i), root, exps.get(i + 1));
        }
        return root;
    }

    private ExpAST parseUnary() {
        return switch (tokens.peekType()) {
            case FLOAT_LIT -> new FloatLitExpAST(tokens.nextFloat());
            case ID -> {
                if (tokens.peekType(1) == TokenType.LP) {
                    yield parseFuncCallExp();
                } else {
                    yield parseVarExpAST();
                }
            }
            case INT_LIT -> new IntLitExpAST(tokens.nextInt());
            case L_NOT -> {
                tokens.expectAndFetch(TokenType.L_NOT);
                yield new UnaryExpAST(UnaryExpAST.Type.L_NOT, parseUnary());
            }
            case LP -> {
                tokens.expectAndFetch(TokenType.LP);
                ExpAST exp = parseAddSubExp();
                tokens.expectAndFetch(TokenType.RP);
                yield exp;
            }
            case MINUS -> {
                tokens.expectAndFetch(TokenType.MINUS);
                yield new UnaryExpAST(UnaryExpAST.Type.NEG, parseUnary());
            }
            case PLUS -> {
                tokens.expectAndFetch(TokenType.PLUS);
                yield parseUnary();
            }
            default -> throw new RuntimeException();
        };
    }

    private StmtAST parseAssignStmt() {
        LValAST lVal = parseLVal();
        tokens.expectAndFetch(TokenType.ASSIGN);
        ExpAST rVal = parseAddSubExp();
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new AssignStmtAST(lVal, rVal);
    }

    private BlankStmtAST parseBlankStmt() {
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new BlankStmtAST();
    }

    private BreakStmtAST parseBreakStmt() {
        tokens.expectAndFetch(TokenType.BREAK);
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new BreakStmtAST();
    }

    private ContinueStmtAST parseContinueStmt() {
        tokens.expectAndFetch(TokenType.CONTINUE);
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new ContinueStmtAST();
    }

    private ExpAST parseCond() {
        tokens.expectAndFetch(TokenType.LP);
        ExpAST cond = parseLOrExp();
        tokens.expectAndFetch(TokenType.RP);
        return cond;
    }

    private List<ExpAST> parseDimensionExp() {
        List<ExpAST> dimensions = new ArrayList<>();
        while (tokens.matchAndThenThrow(TokenType.LB)) {
            dimensions.add(parseAddSubExp());
            tokens.expectAndFetch(TokenType.RB);
        }
        return dimensions;
    }

    private ExpStmtAST parseExpStmt() {
        ExpStmtAST expStmt = new ExpStmtAST(parseAddSubExp());
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return expStmt;
    }

    private FuncCallExpAST parseFuncCallExp() {
        FuncSymbol func = symbolTable.getFunc(tokens.nextIdentity());
        tokens.expectAndFetch(TokenType.LP);
        List<ExpAST> params = new ArrayList<>();
        while (!tokens.matchAndThenThrow(TokenType.RP)) {
            params.add(parseAddSubExp());
            if (tokens.matchAndThenThrow(TokenType.RP)) {
                break;
            }
            tokens.expectAndFetch(TokenType.COMMA);
        }
        return new FuncCallExpAST(func, params);
    }

    private IfStmtAST parseIfStmt() {
        tokens.expectAndFetch(TokenType.IF);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST stmt1 = stmts.get(0);
        if (!tokens.matchAndThenThrow(TokenType.ELSE)) {
            return new IfStmtAST(cond, stmt1, null);
        }
        stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST stmt2 = stmts.get(0);
        return new IfStmtAST(cond, stmt1, stmt2);
    }

    private InitValAST parseInitVal() {
        List<ExpAST> exps = new ArrayList<>();
        tokens.expectAndFetch(TokenType.LC);
        if (tokens.matchAndThenThrow(TokenType.RC)) {
            return new InitValAST(exps);
        }
        do {
            if (tokens.match(TokenType.LC)) {
                exps.add(parseInitVal());
            } else {
                exps.add(parseAddSubExp());
            }
        } while (tokens.matchAndThenThrow(TokenType.COMMA));
        tokens.expectAndFetch(TokenType.RC);
        return new InitValAST(exps);
    }

    private LValAST parseLVal() {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new LValAST(symbolTable.getData(name), dimensions);
        }
        return new LValAST(symbolTable.getData(name), List.of());
    }

    private StmtAST parseRetStmt() {
        tokens.expectAndFetch(TokenType.RETURN);
        if (tokens.matchAndThenThrow(TokenType.SEMICOLON)) {
            return new RetStmtAST(null);
        }
        ExpAST retVal = parseAddSubExp();
        tokens.expectAndFetch(TokenType.SEMICOLON);
        return new RetStmtAST(retVal);
    }

    private ExpAST parseVarExpAST() {
        String name = tokens.nextIdentity();
        if (tokens.match(TokenType.LB)) {
            List<ExpAST> dimensions = parseDimensionExp();
            return new VarExpAST(symbolTable.getData(name), dimensions);
        }
        return new VarExpAST(symbolTable.getData(name), List.of());
    }

    private WhileStmtAST parseWhileStmt() {
        tokens.expectAndFetch(TokenType.WHILE);
        ExpAST cond = parseCond();
        List<? extends StmtAST> stmts = parseStmt();
        if (stmts.size() != 1) {
            throw new RuntimeException();
        }
        StmtAST body = stmts.get(0);
        return new WhileStmtAST(cond, body);
    }

    private static Map<Integer, ExpAST> allocInitVal(ExpAST src, List<Integer> dimensions) {
        record Task(ExpAST exp, int index, int off) {
        }
        Map<Integer, ExpAST> result = new HashMap<>();
        Queue<Task> tasks = new ArrayDeque<>();
        tasks.offer(new Task(src, 1, 0));
        while (!tasks.isEmpty()) {
            Task task = tasks.poll();
            int dim = dimensions.stream().skip(task.index()).reduce(1, Math::multiplyExact);
            int offset = task.off();
            for (ExpAST exp : (InitValAST) task.exp()) {
                if (exp instanceof InitValAST) {
                    offset = (offset + dim - 1) / dim * dim;
                    tasks.offer(new Task(exp, task.index() + 1, offset));
                    offset += dim;
                } else {
                    result.put(offset, exp);
                    offset++;
                }
            }
        }
        return result;
    }

    public RootAST getRootAST() {
        checkIfIsProcessed();
        return rootAST;
    }
}
